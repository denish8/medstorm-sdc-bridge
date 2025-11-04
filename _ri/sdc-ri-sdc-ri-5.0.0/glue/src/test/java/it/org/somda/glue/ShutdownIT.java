package it.org.somda.glue;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import it.org.somda.glue.consumer.TestSdcClient;
import it.org.somda.glue.provider.TestSdcDevice;
import it.org.somda.sdc.dpws.MemoryCommunicationLog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.CommunicationLogContext;
import org.somda.sdc.dpws.factory.CommunicationLogFactory;
import org.somda.sdc.dpws.guice.AppDelayExecutor;
import org.somda.sdc.dpws.guice.NetworkJobThreadPool;
import org.somda.sdc.dpws.guice.WsDiscovery;
import org.somda.sdc.dpws.http.HttpServerRegistry;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.glue.consumer.ConnectConfiguration;
import org.somda.sdc.glue.consumer.SdcRemoteDevicesConnector;
import org.somda.sdc.glue.consumer.SdcRemoteDevicesConnectorImpl;
import org.somda.sdc.glue.guice.Consumer;
import org.somda.sdc.glue.guice.WatchdogScheduledExecutor;
import test.org.somda.common.CIDetector;
import test.org.somda.common.LoggingTestWatcher;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(LoggingTestWatcher.class)
class ShutdownIT {
    private static final Logger LOG = LogManager.getLogger(ShutdownIT.class);
    private static Duration WAIT_TIME = Duration.ofSeconds(30);

    static {
        if (CIDetector.isRunningInCi()) {
            WAIT_TIME = Duration.ofSeconds(180);
            LOG.info("CI detected, setting WAIT_TIME to {}s", WAIT_TIME.getSeconds());
        }
    }

    private TestSdcDevice testDevice;
    private TestSdcClient testClient;

    private MemoryCommunicationLogFactory communicationLogFactory;

    @BeforeEach
    void setUp() {
        testDevice = new TestSdcDevice(Collections.emptyList());
        communicationLogFactory = new MemoryCommunicationLogFactory();
        testClient = new TestSdcClient(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        super.configure();
                        bind(CommunicationLogFactory.class).toInstance(communicationLogFactory);

                        // make this a singleton to allow verifying the running state later
                        bind(SdcRemoteDevicesConnector.class)
                                .to(SdcRemoteDevicesConnectorImpl.class).in(Singleton.class);
                    }
                }
        );

    }

    @AfterEach
    void tearDown() throws Exception {
        if (testClient.isRunning()) {
            testClient.stopAsync().awaitTerminated(WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        }
        if (testDevice.isRunning()) {
            testDevice.stopAsync().awaitTerminated(WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        }
    }

    @Test
    void testConsumerDisconnectOnShutdown() throws Exception {
        testDevice.startAsync().awaitRunning(WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        testClient.startAsync().awaitRunning(WAIT_TIME.getSeconds(), TimeUnit.SECONDS);

        var hostingServiceFuture = testClient.getClient()
                .connect(testDevice.getSdcDevice().getEprAddress());
        var hostingServiceProxy = hostingServiceFuture.get(WAIT_TIME.getSeconds(), TimeUnit.SECONDS);

        var remoteDeviceFuture = testClient.getConnector().connect(hostingServiceProxy,
                ConnectConfiguration.create(ConnectConfiguration.ALL_PERIODIC_AND_WAVEFORM_REPORTS));

        var sdcRemoteDevice = remoteDeviceFuture.get(WAIT_TIME.getSeconds(), TimeUnit.SECONDS);

        assertNotNull(testClient.getConnector().getConnectedDevice(testDevice.getSdcDevice().getEprAddress()));
        assertFalse(testDevice.getSdcDevice().getActiveSubscriptions().isEmpty());

        testClient.stopAsync().awaitTerminated(WAIT_TIME.getSeconds(), TimeUnit.SECONDS);

        assertTrue(testClient.getConnector().getConnectedDevices().isEmpty());

        // the last message must be the unsubscribe response
        var clientMessages = communicationLogFactory.getMemoryCommunicationLog().get().getMessages();
        assertFalse(clientMessages.isEmpty());
        var lastMessage = clientMessages.get(clientMessages.size() - 1);
        assertTrue(lastMessage.getMessage().contains("/UnsubscribeResponse"));

        /*
         * collect all services to check their state is terminated
         */
        var services = new ArrayList<Service>();

        services.add(sdcRemoteDevice);
        // scheduled
        List.of(AppDelayExecutor.class, WatchdogScheduledExecutor.class).forEach(service ->
                services.add(testClient.getInjector().getInstance(Key.get(
                                new TypeLiteral<ExecutorWrapperService<ScheduledExecutorService>>() {
                                },
                                service
                        ))
                )
        );

        // listening
        List.of(NetworkJobThreadPool.class,
                        WsDiscovery.class,
                        Consumer.class)
                .forEach(service ->
                        services.add(testClient.getInjector().getInstance(Key.get(
                                        new TypeLiteral<ExecutorWrapperService<ListeningExecutorService>>() {
                                        },
                                        service
                                ))
                        )
                );

        // Collect all additional services
        List.of(SoapMarshalling.class,
                        HttpServerRegistry.class,
                        SdcRemoteDevicesConnector.class)
                .forEach(serviceClass -> services.add(testClient.getInjector().getInstance(serviceClass)));


        services.forEach(service -> {
            assertEquals(
                    Service.State.TERMINATED, service.state(),
                    service.toString() + " did not shut down"
            );
        });
    }

    @Test
    void testProviderShutdown() throws Exception {
        testDevice.startAsync().awaitRunning(WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        testClient.startAsync().awaitRunning(WAIT_TIME.getSeconds(), TimeUnit.SECONDS);

        var hostingServiceFuture = testClient.getClient()
                .connect(testDevice.getSdcDevice().getEprAddress());
        var hostingServiceProxy = hostingServiceFuture.get(WAIT_TIME.getSeconds(), TimeUnit.SECONDS);

        var remoteDeviceFuture = testClient.getConnector().connect(hostingServiceProxy,
                ConnectConfiguration.create(ConnectConfiguration.ALL_PERIODIC_AND_WAVEFORM_REPORTS));

        remoteDeviceFuture.get(WAIT_TIME.getSeconds(), TimeUnit.SECONDS);

        assertNotNull(testClient.getConnector().getConnectedDevice(testDevice.getSdcDevice().getEprAddress()));
        assertFalse(testDevice.getSdcDevice().getActiveSubscriptions().isEmpty());

        testDevice.stopAsync().awaitTerminated(WAIT_TIME.getSeconds(), TimeUnit.SECONDS);

        // the last incoming message must be a subscriptionEnd
        var clientMessages = communicationLogFactory.getMemoryCommunicationLog().get().getMessages();
        assertFalse(clientMessages.isEmpty());
        var lastMessage = Lists.reverse(clientMessages).stream().filter(message -> CommunicationLog.Direction.INBOUND.equals(message.getDirection())).findFirst();
        assertTrue(lastMessage.get().getMessage().contains("/SubscriptionEnd"));

        // wait for the client to register the disconnect, which happens during the renew
        var waitTime = Duration.ofSeconds(10);
        var waitUntil = Instant.now().plus(waitTime);
        while (Instant.now().isBefore(waitUntil)) {
            // break early when condition is already fulfilled
            if (testClient.getConnector().getConnectedDevices().isEmpty()) {
                break;
            }
            Thread.sleep(5);
        }
        // client must've registered the provider shutdown
        assertTrue(
                testClient.getConnector().getConnectedDevices().isEmpty(),
                "TestClient did not register the provider absence in time"
        );

        /*
         * collect all services to check their state is terminated
         */
        var services = new ArrayList<Service>();

        services.add(testDevice.getSdcDevice());

        // scheduled
        List.of(AppDelayExecutor.class)
                .forEach(service ->
                        services.add(testDevice.getInjector().getInstance(Key.get(
                                        new TypeLiteral<ExecutorWrapperService<ScheduledExecutorService>>() {
                                        },
                                        service
                                ))
                        )
                );

        // listening
        List.of(NetworkJobThreadPool.class,
                        WsDiscovery.class)
                .forEach(service ->
                        services.add(testDevice.getInjector().getInstance(Key.get(
                                        new TypeLiteral<ExecutorWrapperService<ListeningExecutorService>>() {
                                        },
                                        service
                                ))
                        )
                );

        // Collect all additional services
        List.of(SoapMarshalling.class,
                        HttpServerRegistry.class)
                .forEach(serviceClass -> services.add(testDevice.getInjector().getInstance(serviceClass)));


        services.forEach(service -> {
            assertEquals(
                    Service.State.TERMINATED, service.state(),
                    service.toString() + " did not shut down"
            );
        });
    }

    static class MemoryCommunicationLogFactory implements CommunicationLogFactory {
        private static MemoryCommunicationLog communicationLog = null;

        @Override
        public CommunicationLog createCommunicationLog() {
            if (communicationLog == null) {
                communicationLog = new MemoryCommunicationLog();
            }
            return communicationLog;
        }

        Optional<MemoryCommunicationLog> getMemoryCommunicationLog() {
            return Optional.ofNullable(communicationLog);
        }
    }
}
