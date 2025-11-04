package it.org.somda.sdc.dpws.soap;

import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import dpws_test_service.messages._2017._05._10.TestNotification;
import dpws_test_service.messages._2017._05._10.TestOperationRequest;
import dpws_test_service.messages._2017._05._10.TestOperationResponse;
import it.org.somda.sdc.dpws.IntegrationTestPeer;
import it.org.somda.sdc.dpws.MockedUdpBindingModule;
import it.org.somda.sdc.dpws.TestServiceMetadata;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.DpwsUtil;
import org.somda.sdc.dpws.client.Client;
import org.somda.sdc.dpws.device.Device;
import org.somda.sdc.dpws.device.DeviceSettings;
import org.somda.sdc.dpws.device.factory.DeviceFactory;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.factory.HostedServiceFactory;
import org.somda.sdc.dpws.soap.SoapConfig;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.NotificationObject;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import test.org.somda.common.TimedWait;

import javax.xml.namespace.QName;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DeviceAndClientPeer extends IntegrationTestPeer {

    public static final String SCOPE_1 = "http://integration-test-scope1";
    public static final String SCOPE_2 = "http://integration-test-scope2";

    public static final QName QNAME_1 = new QName("http://type-ns", "integration-test-type1");
    public static final QName QNAME_2 = new QName("http://type-ns", "integration-test-type2");

    public static final int NOTIFICATION_COUNT = 3;

    private ListeningExecutorService executorService;

    private Device device;
    private Client client;
    private DpwsFramework dpwsFramework;

    private DpwsTestService1 service1;

    private final Duration defaultMaxWait = Duration.ofSeconds(10);

    private final SoapUtil soapUtil;
    private Thread notificationThread;

    public DeviceAndClientPeer(String localDeviceEprAddress) {
        setupInjector(new DefaultDpwsConfigModule() {
            @Override
            public void customConfigure() {
                bind(SoapConfig.JAXB_CONTEXT_PATH, String.class,
                        TestServiceMetadata.JAXB_CONTEXT_PATH);
            }
        }, new MockedUdpBindingModule());

        this.soapUtil = getInjector().getInstance(SoapUtil.class);
        this.executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));

        var wsaUtil = getInjector().getInstance(WsAddressingUtil.class);
        var epr = wsaUtil.createEprWithAddress(localDeviceEprAddress);
        var deviceSettings = new DeviceSettings() {
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
        };

        this.dpwsFramework = getInjector().getInstance(DpwsFramework.class);
        this.client = getInjector().getInstance(Client.class);
        this.device = getInjector().getInstance(DeviceFactory.class).createDevice(deviceSettings);

    }

    @Override
    protected void startUp() throws Exception {
        device.getDiscoveryAccess().setScopes(Arrays.asList(SCOPE_1, SCOPE_2));
        device.getDiscoveryAccess().setTypes(Arrays.asList(QNAME_1, QNAME_2));

        DpwsUtil dpwsUtil = getInjector().getInstance(DpwsUtil.class);

        device.getHostingServiceAccess().setThisDevice(dpwsUtil.createThisDevice(
                dpwsUtil.createLocalizedStrings("en", "Peer with device and client").get(), null, null));

        HostedServiceFactory hostedServiceFactory = getInjector().getInstance(HostedServiceFactory.class);
        service1 = getInjector().getInstance(DpwsTestService1.class);

        var classLoader = getClass().getClassLoader();
        var wsdlResource1 = classLoader.getResourceAsStream(TestServiceMetadata.SERVICE_ID_1_RESOURCE_PATH);
        assert wsdlResource1 != null;
        device.getHostingServiceAccess().addHostedService(hostedServiceFactory.createHostedService(
                TestServiceMetadata.SERVICE_ID_1,
                Arrays.asList(
                        new QName(TestServiceMetadata.NAMESPACE_SRV, TestServiceMetadata.PORT_TYPE_NAME_1),
                        new QName(TestServiceMetadata.NAMESPACE_SRV, TestServiceMetadata.PORT_TYPE_NAME_2)),
                service1,
                ByteStreams.toByteArray(wsdlResource1)));

        dpwsFramework.startAsync().awaitRunning();
        device.startAsync().awaitRunning();
        client.startAsync().awaitRunning();

        notificationThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    var notification = new TestNotification();
                    notification.setParam1("notification");
                    notification.setParam2(100);
                    service1.sendNotification(notification);
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                } catch (MarshallingException | TransportException e) {
                    e.printStackTrace();
                }
            }
        });
        notificationThread.start();
    }

    @Override
    protected void shutDown() throws Exception {
        notificationThread.interrupt();
        device.stopAsync().awaitTerminated();
        client.stopAsync().awaitTerminated();
        dpwsFramework.stopAsync().awaitTerminated();

        executorService.shutdown();
        executorService.awaitTermination(defaultMaxWait.toSeconds(), TimeUnit.SECONDS);
    }

    public void startInteraction(String remoteEprAddress) throws Exception {
        var future = client.connect(remoteEprAddress);
        var hostingServiceProxy = future.get(defaultMaxWait.toSeconds(), TimeUnit.SECONDS);
        var hostedServiceProxy = hostingServiceProxy.getHostedServices().get(TestServiceMetadata.SERVICE_ID_1);
        assert hostedServiceProxy != null;

        var requestResponseFuture = executorService.submit(new DoRequestResponse(hostedServiceProxy));
        var notificationFuture = executorService.submit(new DoNotification(hostedServiceProxy));

        requestResponseFuture.get(defaultMaxWait.toSeconds(), TimeUnit.SECONDS);
        notificationFuture.get(defaultMaxWait.toSeconds() * NOTIFICATION_COUNT, TimeUnit.SECONDS);
    }

    private class DoRequestResponse implements Runnable {
        private final HostedServiceProxy hostedServiceProxy;

        DoRequestResponse(HostedServiceProxy hostedServiceProxy) {
            this.hostedServiceProxy = hostedServiceProxy;
        }

        @Override
        public void run() {
            try {
                // Just check if send and receive works regardless of response payload verification
                var request = soapUtil.createMessage(TestServiceMetadata.ACTION_OPERATION_REQUEST_1);
                var requestBody = new TestOperationRequest();
                requestBody.setParam1("ThisIsSomeRandomContent");
                requestBody.setParam2(10);
                soapUtil.setBody(requestBody, request);
                var response = hostedServiceProxy.getRequestResponseClient().sendRequestResponse(request);
                soapUtil.getBody(response, TestOperationResponse.class).orElseThrow(() ->
                        new RuntimeException("Response message body is invalid."));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class DoNotification implements Runnable {
        private final HostedServiceProxy hostedServiceProxy;

        DoNotification(HostedServiceProxy hostedServiceProxy) {
            this.hostedServiceProxy = hostedServiceProxy;
        }

        @Override
        public void run() {
            try {
                // Just check if notifications are received regardless of payload verification
                var interceptor = new NotificationSink();
                var subscribe = hostedServiceProxy.getEventSinkAccess().subscribe(
                        Collections.singletonList(TestServiceMetadata.ACTION_NOTIFICATION_1),
                        Duration.ofMinutes(1),
                        interceptor);
                var subscribeResult = subscribe.get(defaultMaxWait.toSeconds(), TimeUnit.SECONDS);

                assert interceptor.waitForMessages(3, defaultMaxWait);

                var unsubscribe = hostedServiceProxy.getEventSinkAccess().unsubscribe(subscribeResult.getSubscriptionId());
                unsubscribe.get(defaultMaxWait.toSeconds(), TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private class NotificationSink implements Interceptor {
            private TimedWait<List<TestNotification>> timedWait = new TimedWait<>(ArrayList::new);

            @MessageInterceptor(TestServiceMetadata.ACTION_NOTIFICATION_1)
            void onNotification(NotificationObject message) {
                var notification = soapUtil.getBody(message.getNotification(), TestNotification.class)
                        .orElseThrow(() -> new RuntimeException("TestNotification could not be converted"));
                timedWait.modifyData(testNotifications -> testNotifications.add(notification));
            }

            boolean waitForMessages(int messageCount, Duration wait) {
                return timedWait.waitForData(testNotifications -> testNotifications.size() >= messageCount, wait);
            }
        }
    }
}
