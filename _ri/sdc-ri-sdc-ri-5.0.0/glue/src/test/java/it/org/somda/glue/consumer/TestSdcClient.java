package it.org.somda.glue.consumer;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import it.org.somda.glue.common.IntegrationTestPeer;
import it.org.somda.sdc.dpws.MockedUdpBindingModule;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.client.Client;
import org.somda.sdc.dpws.guice.NetworkJobThreadPool;
import org.somda.sdc.dpws.guice.WsDiscovery;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConfig;
import org.somda.sdc.glue.consumer.ConsumerConfig;
import org.somda.sdc.glue.consumer.SdcRemoteDevicesConnector;
import org.somda.sdc.glue.guice.GlueDpwsConfigModule;
import test.org.somda.common.CIDetector;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestSdcClient extends IntegrationTestPeer {
    private static final Logger LOG = LogManager.getLogger(TestSdcClient.class);
    public static final Duration REQUESTED_EXPIRES = Duration.ofSeconds(20);

    private final Client client;
    private final SdcRemoteDevicesConnector connector;
    private DpwsFramework dpwsFramework;



    public TestSdcClient(AbstractModule... modules) {
        var defaultOverrides = List.of(
                new MockedUdpBindingModule(),
                new GlueDpwsConfigModule() {
                    @Override
                    protected void customConfigure() {
                        super.customConfigure();

                        bind(ConsumerConfig.REQUESTED_EXPIRES,
                                Duration.class,
                                REQUESTED_EXPIRES);

                        if (CIDetector.isRunningInCi()) {
                            var httpTimeouts = Duration.ofSeconds(120);
                            var futureTimeouts = Duration.ofSeconds(30);
                            LOG.info("CI detected, setting relaxed HTTP client timeouts of {}s",
                                    httpTimeouts.toSeconds());
                            bind(DpwsConfig.HTTP_CLIENT_CONNECT_TIMEOUT,
                                    Duration.class,
                                    httpTimeouts);

                            bind(DpwsConfig.HTTP_CLIENT_READ_TIMEOUT,
                                    Duration.class,
                                    httpTimeouts);

                            List<String> increasedTimeouts = List.of(
                                    DpwsConfig.MAX_WAIT_FOR_FUTURES,
                                    WsDiscoveryConfig.MAX_WAIT_FOR_PROBE_MATCHES,
                                    WsDiscoveryConfig.MAX_WAIT_FOR_RESOLVE_MATCHES
                            );
                            increasedTimeouts.forEach(item -> {
                                LOG.info("CI detected, setting {} to {}s", item, futureTimeouts.toSeconds());
                                bind(item,
                                        Duration.class,
                                        futureTimeouts);
                            });
                        }
                    }
                },
                new AbstractModule() {
                    // bump network pool size because of parallelism tests

                    ExecutorWrapperService<ListeningExecutorService> networkJobThreadPoolExecutor = null;
                    ExecutorWrapperService<ListeningExecutorService> wsDiscoveryExecutor = null;

                    @Provides
                    @NetworkJobThreadPool
                    ExecutorWrapperService<ListeningExecutorService> getNetworkJobThreadPool(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
                        if (networkJobThreadPoolExecutor == null) {
                            Callable<ListeningExecutorService> executor = () -> MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(
                                    30,
                                    new ThreadFactoryBuilder()
                                            .setNameFormat("NetworkJobThreadPool-thread-%d")
                                            .setDaemon(true)
                                            .build()
                            ));
                            networkJobThreadPoolExecutor = new ExecutorWrapperService<>(executor, "NetworkJobThreadPool", frameworkIdentifier);
                        }

                        return networkJobThreadPoolExecutor;
                    }

                    @Provides
                    @WsDiscovery
                    ExecutorWrapperService<ListeningExecutorService> getWsDiscoveryExecutor(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
                        if (wsDiscoveryExecutor == null) {
                            Callable<ListeningExecutorService> executor = () -> MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(
                                    30,
                                    new ThreadFactoryBuilder()
                                            .setNameFormat("WsDiscovery-thread-%d")
                                            .setDaemon(true)
                                            .build()
                            ));

                            wsDiscoveryExecutor = new ExecutorWrapperService<>(executor, "WsDiscovery", frameworkIdentifier);
                        }

                        return wsDiscoveryExecutor;
                    }
                }
        );

        List<AbstractModule> overrides = Stream
                .concat(defaultOverrides.stream(), Arrays.stream(modules))
                .collect(Collectors.toList());

        setupInjector(overrides);

        final Injector injector = getInjector();
        this.client = injector.getInstance(Client.class);
        this.connector = injector.getInstance(SdcRemoteDevicesConnector.class);
    }

    public Client getClient() {
        return client;
    }

    public SdcRemoteDevicesConnector getConnector() {
        return connector;
    }

    @Override
    protected void startUp() throws SocketException {
        this.dpwsFramework = getInjector().getInstance(DpwsFramework.class);
        this.dpwsFramework.setNetworkInterface(NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress()));
        dpwsFramework.startAsync().awaitRunning();
        client.startAsync().awaitRunning();
    }

    @Override
    protected void shutDown() {
        client.stopAsync().awaitTerminated();
        dpwsFramework.stopAsync().awaitTerminated();
    }
}
