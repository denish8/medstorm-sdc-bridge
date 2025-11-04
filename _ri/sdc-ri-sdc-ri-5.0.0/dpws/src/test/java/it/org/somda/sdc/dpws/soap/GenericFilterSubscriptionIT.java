package it.org.somda.sdc.dpws.soap;

import com.google.common.util.concurrent.SettableFuture;
import dpws_test_service.messages._2017._05._10.TestNotification;
import dpws_test_service.messages._2017._05._10.TestOperationRequest;
import it.org.somda.sdc.dpws.IntegrationTestUtil;
import it.org.somda.sdc.dpws.MockedUdpBindingModule;
import it.org.somda.sdc.dpws.TestServiceMetadata;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.crypto.CryptoConfig;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.sdc.dpws.device.DeviceSettings;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.soap.SoapConfig;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.NotificationObject;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConfig;
import org.somda.sdc.dpws.soap.wseventing.GenericEventSource;
import org.somda.sdc.dpws.soap.wseventing.IndividualSubscriptionHandler;
import org.somda.sdc.dpws.soap.wseventing.SourceSubscriptionManager;
import org.somda.sdc.dpws.soap.wseventing.WsEventingConstants;
import org.somda.sdc.dpws.soap.wseventing.factory.GenericEventSourceInterceptorFactory;
import org.somda.sdc.dpws.soap.wseventing.model.SubscriptionEnd;
import org.somda.sdc.dpws.soap.wseventing.model.WsEventingStatus;
import test.org.somda.common.LoggingTestWatcher;

import javax.net.ssl.HostnameVerifier;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(LoggingTestWatcher.class)
class GenericFilterSubscriptionIT {
    private static final String FILTER_DIALECT = "urn:test:example";
    private static final Duration MAX_WAIT_TIME = Duration.ofMinutes(3);
    private final IntegrationTestUtil IT = new IntegrationTestUtil();
    private final SoapUtil soapUtil = IT.getInjector().getInstance(SoapUtil.class);
    private final WsAddressingUtil wsaUtil = IT.getInjector().getInstance(WsAddressingUtil.class);
    private BasicPopulatedDevice devicePeer;
    private GenericEventSource customEventSource;
    private ClientPeer clientPeer;
    private HostnameVerifier verifier;
    private SettableFuture<ImmutablePair<String, TestOperationRequest>> individualSubscription;
    private SettableFuture<String> individualSubscriptionEnd;

    GenericFilterSubscriptionIT() {
        IntegrationTestUtil.preferIpV4Usage();
    }

    @BeforeEach
    void setUp() {
        var serverCryptoSettings = Ssl.setupServer();

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

        individualSubscription = SettableFuture.create();
        individualSubscriptionEnd = SettableFuture.create();
        customEventSource = IT.getInjector().getInstance(GenericEventSourceInterceptorFactory.class).create(
                FILTER_DIALECT,
                new IndividualSubscriptionHandler() {
                    @Override
                    public void startStream(SourceSubscriptionManager subscriptionManager) {
                        final var filterObjects = subscriptionManager.getFilters();
                        assert !filterObjects.isEmpty();
                        assert filterObjects.get(0) instanceof TestOperationRequest;
                        individualSubscription.set(ImmutablePair.of(
                                subscriptionManager.getSubscriptionId(), (TestOperationRequest) filterObjects.get(0)));
                    }

                    @Override
                    public void endStream(SourceSubscriptionManager subscriptionManager) {
                        individualSubscriptionEnd.set(subscriptionManager.getSubscriptionId());
                    }
                }
        );
        devicePeer.getService1().registerEventSource(customEventSource);

        var clientCryptoSettings = Ssl.setupClient();
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
        }, new MockedUdpBindingModule());
    }

    @AfterEach
    void tearDown() {
        this.devicePeer.stopAsync().awaitTerminated();
        this.clientPeer.stopAsync().awaitTerminated();
    }

    @Test
    void testGenericEventSourceSubscriptionWithIndividualDeliveryAndEndBySource() throws Exception {
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        final var hostingServiceProxy = clientPeer.getClient().connect(devicePeer.getEprAddress())
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        final var count = 5;
        SettableFuture<List<Object>> notificationFuture = SettableFuture.create();
        final var srv1 = hostingServiceProxy.getHostedServices().get(TestServiceMetadata.SERVICE_ID_1);
        final var filter = new TestOperationRequest();

        final var expectedParam1 = "Test";
        final var expectedParam2 = 100;

        filter.setParam1(expectedParam1);
        filter.setParam2(expectedParam2);
        final var subscribe = srv1.getEventSinkAccess()
                .subscribe(
                        FILTER_DIALECT,
                        Collections.singletonList(filter), Duration.ofMinutes(1),
                        new Interceptor() {
                            private final List<Object> receivedNotifications = new ArrayList<>();

                            @MessageInterceptor(value = TestServiceMetadata.ACTION_NOTIFICATION_1)
                            void onNotification(NotificationObject message) {
                                soapUtil.getBody(message.getNotification(), TestNotification.class).ifPresent(receivedNotifications::add);
                                if (receivedNotifications.size() == count) {
                                    notificationFuture.set(receivedNotifications);
                                }
                            }

                            @MessageInterceptor(value = WsEventingConstants.WSA_ACTION_SUBSCRIPTION_END)
                            void onSubscriptionEnd(NotificationObject message) {
                                soapUtil.getBody(message.getNotification(), SubscriptionEnd.class).ifPresent(receivedNotifications::add);
                                if (receivedNotifications.size() == count) {
                                    notificationFuture.set(receivedNotifications);
                                }
                            }
                        });

        subscribe.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);

        final var activeSubs = devicePeer.getDevice().getActiveSubscriptions();
        final var subscriptionManagerOpt = activeSubs.values().stream().findFirst();
        assertTrue(subscriptionManagerOpt.isPresent());
        assertEquals(FILTER_DIALECT, subscriptionManagerOpt.get().getFilterDialect());
        final var subscriptionFilter = subscriptionManagerOpt.get().getFilters().stream().findFirst();
        assertTrue(subscriptionFilter.isPresent());
        assertInstanceOf(TestOperationRequest.class, subscriptionFilter.get());
        final var castSubscriptionFilter = (TestOperationRequest) subscriptionFilter.get();
        assertEquals(expectedParam1, castSubscriptionFilter.getParam1());
        assertEquals(expectedParam2, castSubscriptionFilter.getParam2());

        final var incomingSubscription = individualSubscription.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        assertEquals(expectedParam1, incomingSubscription.right.getParam1());
        assertEquals(expectedParam2, incomingSubscription.right.getParam2());

        for (int i = 0; i < count - 1; i++) {
            final var notification = new TestNotification();
            notification.setParam1(expectedParam1);
            notification.setParam2(expectedParam2 + i);
            customEventSource.sendNotificationFor(incomingSubscription.left,
                    TestServiceMetadata.ACTION_NOTIFICATION_1, notification);
        }

        customEventSource.endSubscriptionFor(incomingSubscription.left);

        final var notifications = notificationFuture.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        assertEquals(count, notifications.size());

        for (int i = 0; i < count - 1; i++) {
            assertEquals(expectedParam1, ((TestNotification)notifications.get(i)).getParam1());
            assertEquals(expectedParam2 + i, ((TestNotification)notifications.get(i)).getParam2());
        }

        assertInstanceOf(SubscriptionEnd.class, notifications.get(count - 1));
    }

    @Test
    void testGenericEventSourceSubscriptionWithIndividualDeliveryAndEndBySink() throws Exception {
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        final var hostingServiceProxy = clientPeer.getClient().connect(devicePeer.getEprAddress())
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        final var count = 5;
        SettableFuture<List<TestNotification>> notificationFuture = SettableFuture.create();
        final var srv1 = hostingServiceProxy.getHostedServices().get(TestServiceMetadata.SERVICE_ID_1);
        final var filter = new TestOperationRequest();

        final var expectedParam1 = "Test";
        final var expectedParam2 = 100;

        filter.setParam1(expectedParam1);
        filter.setParam2(expectedParam2);
        final var subscribe = srv1.getEventSinkAccess()
                .subscribe(
                        FILTER_DIALECT,
                        Collections.singletonList(filter), Duration.ofMinutes(1),
                        new Interceptor() {
                            private final List<TestNotification> receivedNotifications = new ArrayList<>();

                            @MessageInterceptor(value = TestServiceMetadata.ACTION_NOTIFICATION_1)
                            void onNotification(NotificationObject message) {
                                soapUtil.getBody(message.getNotification(), TestNotification.class).ifPresent(receivedNotifications::add);
                                if (receivedNotifications.size() == count) {
                                    notificationFuture.set(receivedNotifications);
                                }
                            }
                        });

        final var subscribeResult = subscribe.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        final var incomingSubscription = individualSubscription.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        for (int i = 0; i < count; i++) {
            final var notification = new TestNotification();
            notification.setParam1(expectedParam1);
            notification.setParam2(expectedParam2 + i);
            customEventSource.sendNotificationFor(incomingSubscription.left,
                    TestServiceMetadata.ACTION_NOTIFICATION_1, notification);
        }

        srv1.getEventSinkAccess().unsubscribe(subscribeResult.getSubscriptionId());
        final var incomingSubscriptionEnd = individualSubscriptionEnd.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        assertEquals(incomingSubscription.left, incomingSubscriptionEnd);
    }

    @Test
    void testGenericEventSourceSubscriptionWithEndBySource() throws Exception {
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        final var hostingServiceProxy = clientPeer.getClient().connect(devicePeer.getEprAddress())
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        final var count = 5;
        SettableFuture<List<Object>> notificationFuture = SettableFuture.create();
        final var srv1 = hostingServiceProxy.getHostedServices().get(TestServiceMetadata.SERVICE_ID_1);
        final var filter = new TestOperationRequest();

        final var expectedParam1 = "Test";
        final var expectedParam2 = 100;

        filter.setParam1(expectedParam1);
        filter.setParam2(expectedParam2);
        final var subscribe = srv1.getEventSinkAccess()
                .subscribe(
                        FILTER_DIALECT,
                        Collections.singletonList(filter), Duration.ofMinutes(1),
                        new Interceptor() {
                            private final List<Object> receivedNotifications = new ArrayList<>();

                            @MessageInterceptor(value = TestServiceMetadata.ACTION_NOTIFICATION_1)
                            void onNotification(NotificationObject message) {
                                soapUtil.getBody(message.getNotification(), TestNotification.class).ifPresent(receivedNotifications::add);
                                if (receivedNotifications.size() == count) {
                                    notificationFuture.set(receivedNotifications);
                                }
                            }

                            @MessageInterceptor(value = WsEventingConstants.WSA_ACTION_SUBSCRIPTION_END)
                            void onSubscriptionEnd(NotificationObject message) {
                                soapUtil.getBody(message.getNotification(), SubscriptionEnd.class).ifPresent(receivedNotifications::add);
                                if (receivedNotifications.size() == count) {
                                    notificationFuture.set(receivedNotifications);
                                }
                            }
                        });

        subscribe.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);

        for (int i = 0; i < count - 1; i++) {
            final var notification = new TestNotification();
            notification.setParam1(expectedParam1);
            notification.setParam2(expectedParam2 + i);
            customEventSource.sendNotification(TestServiceMetadata.ACTION_NOTIFICATION_1, notification);
        }
        customEventSource.subscriptionEndToAll(WsEventingStatus.STATUS_SOURCE_CANCELLING);

        final var notifications = notificationFuture.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        assertEquals(count, notifications.size());

        for (int i = 0; i < count - 1; i++) {
            assertEquals(expectedParam1, ((TestNotification)notifications.get(i)).getParam1());
            assertEquals(expectedParam2 + i, ((TestNotification)notifications.get(i)).getParam2());
        }

        assertInstanceOf(SubscriptionEnd.class, notifications.get(count - 1));
    }

    @Test
    void testGenericEventSourceSubscriptionWithEndBySink() throws Exception {
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        final var hostingServiceProxy = clientPeer.getClient().connect(devicePeer.getEprAddress())
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        final var count = 5;
        SettableFuture<List<TestNotification>> notificationFuture = SettableFuture.create();
        final var srv1 = hostingServiceProxy.getHostedServices().get(TestServiceMetadata.SERVICE_ID_1);
        final var filter = new TestOperationRequest();

        final var expectedParam1 = "Test";
        final var expectedParam2 = 100;

        filter.setParam1(expectedParam1);
        filter.setParam2(expectedParam2);
        final var subscribe = srv1.getEventSinkAccess()
                .subscribe(
                        FILTER_DIALECT,
                        Collections.singletonList(filter), Duration.ofMinutes(1),
                        new Interceptor() {
                            private final List<TestNotification> receivedNotifications = new ArrayList<>();

                            @MessageInterceptor(value = TestServiceMetadata.ACTION_NOTIFICATION_1)
                            void onNotification(NotificationObject message) {
                                soapUtil.getBody(message.getNotification(), TestNotification.class).ifPresent(receivedNotifications::add);
                                if (receivedNotifications.size() == count) {
                                    notificationFuture.set(receivedNotifications);
                                }
                            }
                        });

        final var subscribeResult = subscribe.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);

        for (int i = 0; i < count; i++) {
            final var notification = new TestNotification();
            notification.setParam1(expectedParam1);
            notification.setParam2(expectedParam2 + i);
            customEventSource.sendNotification(TestServiceMetadata.ACTION_NOTIFICATION_1, notification);
        }

        final var notifications = notificationFuture.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        assertEquals(count, notifications.size());

        for (int i = 0; i < count; i++) {
            assertEquals(expectedParam1, notifications.get(i).getParam1());
            assertEquals(expectedParam2 + i, notifications.get(i).getParam2());
        }

        srv1.getEventSinkAccess().unsubscribe(subscribeResult.getSubscriptionId()).get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        assertTrue(customEventSource.getActiveSubscriptions().isEmpty());
    }
}
