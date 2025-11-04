package it.org.somda.sdc.dpws.soap;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import dpws_test_service.messages._2017._05._10.ObjectFactory;
import dpws_test_service.messages._2017._05._10.TestNotification;
import dpws_test_service.messages._2017._05._10.TestOperationRequest;
import it.org.somda.sdc.dpws.IntegrationTestUtil;
import it.org.somda.sdc.dpws.MockedUdpBindingModule;
import it.org.somda.sdc.dpws.TestServiceMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.CommunicationLogDummyImpl;
import org.somda.sdc.dpws.CommunicationLogImpl;
import org.somda.sdc.dpws.CommunicationLogSink;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.client.DiscoveredDevice;
import org.somda.sdc.dpws.crypto.CryptoConfig;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.sdc.dpws.device.DeviceSettings;
import org.somda.sdc.dpws.factory.CommunicationLogFactory;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.SoapConfig;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.TransportInfo;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.NotificationObject;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConfig;
import org.somda.sdc.dpws.soap.wseventing.SubscribeResult;
import test.org.somda.common.LoggingTestWatcher;

import javax.net.ssl.HostnameVerifier;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(LoggingTestWatcher.class)
class CryptoIT {
    private static final Duration MAX_WAIT_TIME = Duration.ofMinutes(3);

    private final IntegrationTestUtil IT = new IntegrationTestUtil();
    private final SoapUtil soapUtil = IT.getInjector().getInstance(SoapUtil.class);
    private final WsAddressingUtil wsaUtil = IT.getInjector().getInstance(WsAddressingUtil.class);

    private BasicPopulatedDevice devicePeer;
    private ClientPeer clientPeer;
    private HostnameVerifier verifier;
    private TestCommLogSink logSink;
    private X509Certificate clientCertificate;
    private X509Certificate serverCertificate;

    CryptoIT() {
        IntegrationTestUtil.preferIpV4Usage();
    }

    @BeforeEach
    void setUp() {
        final CryptoSettings serverCryptoSettings = Ssl.setupServer();

        // add custom hostname verifier
        this.verifier = mock(HostnameVerifier.class);
        when(verifier.verify(anyString(), any())).thenReturn(true);

        this.devicePeer = new BasicPopulatedDevice(new DeviceSettings() {
            final EndpointReferenceType epr = wsaUtil.createEprWithAddress(soapUtil.createUriFromUuid(UUID.randomUUID()));

            @Override
            public EndpointReferenceType getEndpointReference() {
                return epr;
            }

            @Override
            public NetworkInterface getNetworkInterface() {
                try {
                    return NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }, new DefaultDpwsConfigModule() {
            @Override
            public void customConfigure() {
                bind(CryptoConfig.CRYPTO_SETTINGS, CryptoSettings.class, serverCryptoSettings);
                bind(DpwsConfig.HTTP_SUPPORT, Boolean.class, false);
                bind(DpwsConfig.HTTPS_SUPPORT, Boolean.class, true);
                bind(CryptoConfig.CRYPTO_DEVICE_HOSTNAME_VERIFIER, HostnameVerifier.class, verifier);
                bind(CryptoConfig.CRYPTO_TLS_ENABLED_VERSIONS, String[].class, new String[]{"TLSv1.3"});
                bind(CryptoConfig.CRYPTO_TLS_ENABLED_CIPHERS, String[].class, new String[]{"TLS_AES_128_GCM_SHA256"});
            }
        }, new MockedUdpBindingModule());

        final CryptoSettings clientCryptoSettings = Ssl.setupClient();
        try {
            this.clientPeer = new ClientPeer(new DefaultDpwsConfigModule() {
                @Override
                public void customConfigure() {
                    bind(WsDiscoveryConfig.MAX_WAIT_FOR_PROBE_MATCHES, Duration.class,
                            Duration.ofSeconds(MAX_WAIT_TIME.getSeconds() / 2));
                    bind(CryptoConfig.CRYPTO_SETTINGS, CryptoSettings.class, clientCryptoSettings);
                    bind(SoapConfig.JAXB_CONTEXT_PATH, String.class,
                            TestServiceMetadata.JAXB_CONTEXT_PATH);
                    bind(CryptoConfig.CRYPTO_TLS_ENABLED_VERSIONS, String[].class, new String[]{"TLSv1.3"});
                    bind(CryptoConfig.CRYPTO_TLS_ENABLED_CIPHERS, String[].class, new String[]{"TLS_AES_128_GCM_SHA256"});
                    bind(DpwsConfig.HTTP_SUPPORT, Boolean.class, false);
                    bind(DpwsConfig.HTTPS_SUPPORT, Boolean.class, true);
                }
            }, new MockedUdpBindingModule(), new AbstractModule() {
                @Override
                protected void configure() {
                    bind(CommunicationLogSink.class).to(TestCommLogSink.class).asEagerSingleton();
                    install(new FactoryModuleBuilder()
                            .implement(CommunicationLog.class, CommunicationLogImpl.class)
                            .build(CommunicationLogFactory.class));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        logSink = (TestCommLogSink) clientPeer.getInjector().getInstance(CommunicationLogSink.class);
        clientCertificate = Ssl.getClientCertificate();
        serverCertificate = Ssl.getServerCertificate();
    }

    @AfterEach
    void tearDown() {
        this.devicePeer.stopAsync().awaitTerminated();
        this.clientPeer.stopAsync().awaitTerminated();
        this.logSink.clear();
    }

    @Test
    void testDirectedProbeSecured() throws Exception {
        // Given a device under test (DUT) and a client up and running
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        // When the DUT's physical addresses are resolved
        final DiscoveredDevice discoveredDevice = clientPeer.getClient().resolve(devicePeer.getEprAddress())
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        final List<String> xAddrs = discoveredDevice.getXAddrs();
        assertFalse(xAddrs.isEmpty());
        var uri = xAddrs.get(0);

        // Then expect the EPR address returned by a directed probe to be the DUT's EPR address
        final String expectedEprAddress = devicePeer.getEprAddress();

        assertEquals(expectedEprAddress, clientPeer.getClient().directedProbe(uri)
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS)
                .getProbeMatch().get(0).getEndpointReference().getAddress().getValue());
    }

    @Test
    void testTransportInfoInRequestResponse() throws Exception {
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        var connectFuture = clientPeer.getClient().connect(devicePeer.getEprAddress());
        var hostingServiceProxy = connectFuture.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        var hostedServiceProxy = hostingServiceProxy.getHostedServices().get(TestServiceMetadata.SERVICE_ID_1);
        assertNotNull(hostedServiceProxy);

        final TestOperationRequest request = new TestOperationRequest();
        request.setParam1("testString");
        request.setParam2(11);

        var reqMsg = clientPeer.getInjector().getInstance(SoapUtil.class)
                .createMessage(TestServiceMetadata.ACTION_OPERATION_REQUEST_1, request);
        hostedServiceProxy.getRequestResponseClient().sendRequestResponse(reqMsg);
        assertEquals(1, devicePeer.getTransportInfosReceivedFromService1().size());
        assertFalse(devicePeer.getTransportInfosReceivedFromService1().get(0).getX509Certificates().isEmpty());
    }

    @Test
    void testTransportInfoInRequestResponseServer() throws Exception {
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        var connectFuture = clientPeer.getClient().connect(devicePeer.getEprAddress());
        var hostingServiceProxy = connectFuture.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        var hostedServiceProxy = hostingServiceProxy.getHostedServices().get(TestServiceMetadata.SERVICE_ID_1);
        assertNotNull(hostedServiceProxy);

        final TestOperationRequest request = new TestOperationRequest();
        request.setParam1("testString");
        request.setParam2(11);

        var reqMsg = clientPeer.getInjector().getInstance(SoapUtil.class)
                .createMessage(TestServiceMetadata.ACTION_OPERATION_REQUEST_1, request);
        final var numberBeforeRR = logSink.getInboundTransportInfos().stream().filter(info -> info.getX509Certificates().size() > 0).count();
        hostedServiceProxy.getRequestResponseClient().sendRequestResponse(reqMsg);
        final var numberAfterRR = logSink.getInboundTransportInfos().stream().filter(info -> info.getX509Certificates().size() > 0).count();

        assertEquals(1, numberAfterRR - numberBeforeRR);
        for (var certificate : logSink.getInboundTransportInfos().stream().map(TransportInfo::getX509Certificates).flatMap(List::stream).collect(Collectors.toList())) {
            assertEquals(serverCertificate, certificate);
        }
    }

    @Test
    void testTransportInfoInRequestResponseClient() throws Exception {
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        var connectFuture = clientPeer.getClient().connect(devicePeer.getEprAddress());
        var hostingServiceProxy = connectFuture.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        var hostedServiceProxy = hostingServiceProxy.getHostedServices().get(TestServiceMetadata.SERVICE_ID_1);
        assertNotNull(hostedServiceProxy);

        final var request = new TestOperationRequest();
        request.setParam1("testString");
        request.setParam2(11);

        var reqMsg = clientPeer.getInjector().getInstance(SoapUtil.class)
                .createMessage(TestServiceMetadata.ACTION_OPERATION_REQUEST_1, request);
        final var numberBeforeRR = logSink.getOutboundTransportInfos().stream().filter(info -> info.getX509Certificates().size() > 0).count();
        hostedServiceProxy.getRequestResponseClient().sendRequestResponse(reqMsg);
        final var numberAfterRR = logSink.getOutboundTransportInfos().stream().filter(info -> info.getX509Certificates().size() > 0).count();

        assertEquals(1, numberAfterRR - numberBeforeRR);
        for (var certificate : logSink.getOutboundTransportInfos().stream().map(TransportInfo::getX509Certificates).flatMap(List::stream).collect(Collectors.toList())) {
            assertEquals(clientCertificate, certificate);
        }
    }

    @Test
    void testNotificationSecured() throws Exception {
        // Given a device under test (DUT) and a client up and running
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();
        final HostingServiceProxy hostingServiceProxy = clientPeer.getClient().connect(devicePeer.getEprAddress())
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        final int COUNT = 100;
        final SettableFuture<List<TestNotification>> notificationFuture = SettableFuture.create();
        final HostedServiceProxy srv1 = hostingServiceProxy.getHostedServices().get(TestServiceMetadata.SERVICE_ID_1);
        final ListenableFuture<SubscribeResult> subscribe = srv1.getEventSinkAccess()
                .subscribe(Collections.singletonList(TestServiceMetadata.ACTION_NOTIFICATION_1), Duration.ofMinutes(1),
                        new Interceptor() {
                            private final List<TestNotification> receivedNotifications = new ArrayList<>();

                            @MessageInterceptor(value = TestServiceMetadata.ACTION_NOTIFICATION_1)
                            void onNotification(NotificationObject message) {
                                assertTrue(message.getCommunicationContext().isPresent());
                                assertFalse(message.getCommunicationContext().get().getTransportInfo().getX509Certificates().isEmpty());
                                receivedNotifications.add(
                                        soapUtil.getBody(message.getNotification(), TestNotification.class)
                                                .orElseThrow(() -> new RuntimeException("TestNotification could not be converted")));
                                if (receivedNotifications.size() == COUNT) {
                                    notificationFuture.set(receivedNotifications);
                                }
                            }
                        });

        subscribe.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);

        final ObjectFactory factory = IT.getInjector().getInstance(ObjectFactory.class);
        for (int i = 0; i < COUNT; ++i) {
            final TestNotification testNotification = factory.createTestNotification();
            testNotification.setParam1(Integer.toString(i));
            testNotification.setParam2(i);
            devicePeer.getService1().sendNotification(testNotification);
        }

        final List<TestNotification> notifications = notificationFuture.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        assertEquals(COUNT, notifications.size());
        for (int i = 0; i < COUNT; ++i) {
            final TestNotification notification = notifications.get(i);
            assertEquals(Integer.toString(i), notification.getParam1());
            assertEquals(i, notification.getParam2());
        }
    }

    @Test
    void testDeviceHostnameVerifierCalled() throws Exception {
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        // When the DUT's physical addresses are resolved
        final DiscoveredDevice discoveredDevice = clientPeer.getClient().resolve(devicePeer.getEprAddress())
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        final List<String> xAddrs = discoveredDevice.getXAddrs();
        assertFalse(xAddrs.isEmpty());
        var uri = xAddrs.get(0);

        // Then expect the EPR address returned by a directed probe to be the DUT's EPR address
        final String expectedEprAddress = devicePeer.getEprAddress();

        // make five requests using this connection
        for (int i = 0; i < 5; i++) {
            assertEquals(expectedEprAddress, clientPeer.getClient().directedProbe(uri)
                    .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS)
                    .getProbeMatch().get(0).getEndpointReference().getAddress().getValue());
        }

        // hostname verifier is called two times: SSLConnectionSocketFactory, JettyHttpServerRegistry
        verify(verifier, times(2)).verify(any(), any());
    }

    @Test
    void testConfigureTlsVersionServer() throws Exception {
        final CryptoSettings clientCryptoSettings = Ssl.setupClient();
        clientPeer = new ClientPeer(new DefaultDpwsConfigModule() {
            @Override
            public void customConfigure() {
                bind(WsDiscoveryConfig.MAX_WAIT_FOR_PROBE_MATCHES, Duration.class,
                        Duration.ofSeconds(MAX_WAIT_TIME.getSeconds() / 2));
                bind(CryptoConfig.CRYPTO_SETTINGS, CryptoSettings.class, clientCryptoSettings);
                bind(DpwsConfig.HTTP_SUPPORT, Boolean.class, false);
                bind(DpwsConfig.HTTPS_SUPPORT, Boolean.class, true);
                bind(SoapConfig.JAXB_CONTEXT_PATH, String.class,
                        TestServiceMetadata.JAXB_CONTEXT_PATH);
                bind(CryptoConfig.CRYPTO_TLS_ENABLED_VERSIONS, String[].class, new String[]{"TLSv1.2"});
            }
        }, new MockedUdpBindingModule());

        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        // When the DUT's physical addresses are resolved
        final DiscoveredDevice discoveredDevice = clientPeer.getClient().resolve(devicePeer.getEprAddress())
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        final List<String> xAddrs = discoveredDevice.getXAddrs();
        assertFalse(xAddrs.isEmpty());
        var uri = xAddrs.get(0);

        // this should throw because we're incompatible
        assertThrows(ExecutionException.class, () -> clientPeer.getClient().directedProbe(uri)
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS)
                .getProbeMatch().get(0).getEndpointReference().getAddress().getValue());
    }

    @Test
    void testConfigureTlsCiphers() throws Exception {
        final CryptoSettings clientCryptoSettings = Ssl.setupClient();
        clientPeer = new ClientPeer(new DefaultDpwsConfigModule() {
            @Override
            public void customConfigure() {
                bind(WsDiscoveryConfig.MAX_WAIT_FOR_PROBE_MATCHES, Duration.class,
                        Duration.ofSeconds(MAX_WAIT_TIME.getSeconds() / 2));
                bind(CryptoConfig.CRYPTO_SETTINGS, CryptoSettings.class, clientCryptoSettings);
                bind(SoapConfig.JAXB_CONTEXT_PATH, String.class,
                        TestServiceMetadata.JAXB_CONTEXT_PATH);
                bind(DpwsConfig.HTTP_SUPPORT, Boolean.class, false);
                bind(DpwsConfig.HTTPS_SUPPORT, Boolean.class, true);
                bind(CryptoConfig.CRYPTO_TLS_ENABLED_VERSIONS, String[].class, new String[]{"TLSv1.3"});
                bind(CryptoConfig.CRYPTO_TLS_ENABLED_CIPHERS, String[].class, new String[]{"TLS_AES_256_GCM_SHA384"});
            }
        }, new MockedUdpBindingModule());

        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        // When the DUT's physical addresses are resolved
        final DiscoveredDevice discoveredDevice = clientPeer.getClient().resolve(devicePeer.getEprAddress())
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        final List<String> xAddrs = discoveredDevice.getXAddrs();
        assertFalse(xAddrs.isEmpty());
        var uri = xAddrs.get(0);

        // this should throw because we're incompatible
        assertThrows(ExecutionException.class, () ->
                clientPeer.getClient().directedProbe(uri)
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS)
                .getProbeMatch().get(0).getEndpointReference().getAddress().getValue()
        );
    }

    @Test
    void testClientPlainServerEncrypted() throws Exception {
        final CryptoSettings clientCryptoSettings = Ssl.setupClient();
        clientPeer = new ClientPeer(new DefaultDpwsConfigModule() {
            @Override
            public void customConfigure() {
                bind(WsDiscoveryConfig.MAX_WAIT_FOR_PROBE_MATCHES, Duration.class,
                        Duration.ofSeconds(MAX_WAIT_TIME.getSeconds() / 2));
                bind(CryptoConfig.CRYPTO_SETTINGS, CryptoSettings.class, clientCryptoSettings);
                bind(SoapConfig.JAXB_CONTEXT_PATH, String.class,
                        TestServiceMetadata.JAXB_CONTEXT_PATH);
                bind(DpwsConfig.HTTP_SUPPORT, Boolean.class, true);
                bind(DpwsConfig.HTTPS_SUPPORT, Boolean.class, false);
                bind(CryptoConfig.CRYPTO_TLS_ENABLED_VERSIONS, String[].class, new String[]{"TLSv1.3"});
                bind(CryptoConfig.CRYPTO_TLS_ENABLED_CIPHERS, String[].class, new String[]{"TLS_AES_256_GCM_SHA384"});
            }
        }, new MockedUdpBindingModule());

        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        // When the DUT's physical addresses are resolved
        final DiscoveredDevice discoveredDevice = clientPeer.getClient().resolve(devicePeer.getEprAddress())
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        final List<String> xAddrs = discoveredDevice.getXAddrs();
        assertFalse(xAddrs.isEmpty());
        var uri = xAddrs.get(0);

        // this should throw because we're incompatible
        assertThrows(ExecutionException.class, () ->
                clientPeer.getClient().directedProbe(uri)
                        .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS)
                        .getProbeMatch().get(0).getEndpointReference().getAddress().getValue()
        );
    }

    @Test
    void testNotificationSecuredMixedModeClient() throws Exception {

        this.clientPeer = new ClientPeer(new DefaultDpwsConfigModule() {
            @Override
            public void customConfigure() {
                bind(WsDiscoveryConfig.MAX_WAIT_FOR_PROBE_MATCHES, Duration.class,
                        Duration.ofSeconds(MAX_WAIT_TIME.getSeconds() / 2));
                bind(CryptoConfig.CRYPTO_SETTINGS, CryptoSettings.class, Ssl.setupClient());
                bind(SoapConfig.JAXB_CONTEXT_PATH, String.class,
                        TestServiceMetadata.JAXB_CONTEXT_PATH);
                bind(CryptoConfig.CRYPTO_TLS_ENABLED_VERSIONS, String[].class, new String[]{"TLSv1.3"});
                bind(CryptoConfig.CRYPTO_TLS_ENABLED_CIPHERS, String[].class, new String[]{"TLS_AES_128_GCM_SHA256"});
                bind(DpwsConfig.HTTP_SUPPORT, Boolean.class, true);
                bind(DpwsConfig.HTTPS_SUPPORT, Boolean.class, true);
            }
        }, new MockedUdpBindingModule());

        // don't fix what ain't broke
        testNotificationSecured();
    }


    @Test
    void testNotificationSecuredMixedModeServer() throws Exception {
        this.devicePeer = new BasicPopulatedDevice(new DeviceSettings() {
            final EndpointReferenceType epr = wsaUtil.createEprWithAddress(soapUtil.createUriFromUuid(UUID.randomUUID()));

            @Override
            public EndpointReferenceType getEndpointReference() {
                return epr;
            }

            @Override
            public NetworkInterface getNetworkInterface() {
                try {
                    return NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }, new DefaultDpwsConfigModule() {
            @Override
            public void customConfigure() {
                bind(CryptoConfig.CRYPTO_SETTINGS, CryptoSettings.class, Ssl.setupServer());
                bind(DpwsConfig.HTTP_SUPPORT, Boolean.class, true);
                bind(DpwsConfig.HTTPS_SUPPORT, Boolean.class, true);
                bind(CryptoConfig.CRYPTO_DEVICE_HOSTNAME_VERIFIER, HostnameVerifier.class, verifier);
                bind(CryptoConfig.CRYPTO_TLS_ENABLED_VERSIONS, String[].class, new String[]{"TLSv1.3"});
                bind(CryptoConfig.CRYPTO_TLS_ENABLED_CIPHERS, String[].class, new String[]{"TLS_AES_128_GCM_SHA256"});
            }
        }, new MockedUdpBindingModule());

        // don't fix what ain't broke
        testNotificationSecured();
    }

    static class TestCommLogSink implements CommunicationLogSink {

        private final ArrayList<TransportInfo> inboundTransportInfos;
        private final ArrayList<TransportInfo> outboundTransportInfos;

        TestCommLogSink() {
            this.inboundTransportInfos = new ArrayList<>();
            this.outboundTransportInfos = new ArrayList<>();
        }

        @Override
        public OutputStream createTargetStream(CommunicationLog.TransportType path,
                                               CommunicationLog.Direction direction,
                                               CommunicationLog.MessageType messageType,
                                               CommunicationContext communicationContext) {
            var os = new ByteArrayOutputStream();
            if (CommunicationLog.Direction.INBOUND.equals(direction)) {
                inboundTransportInfos.add(communicationContext.getTransportInfo());
            } else {
                outboundTransportInfos.add(communicationContext.getTransportInfo());
            }
            return os;
        }

        public void clear() {
            inboundTransportInfos.clear();
            outboundTransportInfos.clear();
        }

        public ArrayList<TransportInfo> getInboundTransportInfos() {
            return inboundTransportInfos;
        }

        public ArrayList<TransportInfo> getOutboundTransportInfos() {
            return outboundTransportInfos;
        }
    }
}
