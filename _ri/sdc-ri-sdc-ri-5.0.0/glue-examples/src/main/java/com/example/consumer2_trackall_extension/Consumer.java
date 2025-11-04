package com.example.consumer2_trackall_extension;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.common.FindExtensions;
import org.somda.sdc.biceps.common.access.MdibAccessObserver;
import org.somda.sdc.biceps.common.event.AbstractMdibAccessMessage;
import org.somda.sdc.biceps.common.event.MetricStateModificationMessage;
import org.somda.sdc.common.util.AutoLock;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.client.Client;
import org.somda.sdc.dpws.client.DiscoveryObserver;
import org.somda.sdc.dpws.client.event.DeviceEnteredMessage;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.glue.consumer.ConnectConfiguration;
import org.somda.sdc.glue.consumer.PrerequisitesException;
import org.somda.sdc.glue.consumer.SdcRemoteDevice;
import org.somda.sdc.glue.consumer.SdcRemoteDevicesConnector;
import org.somda.sdc.glue.examples.extension.CompiledExtension;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * This is an example consumer that connects to a device based on a configured UUID, tracks all changes,
 * and tries to parse the state extension from provider2_extension example.
 */
public class Consumer extends AbstractIdleService {
    private static final Logger LOG = LogManager.getLogger(Consumer.class);
    private static final Duration MAX_WAIT = Duration.ofSeconds(11);
    private static final long MAX_WAIT_SEC = MAX_WAIT.getSeconds();

    private static final String EXTENSION_NAMESPACE = "http://biceps.extension";
    private static final String EXTENSION_STATE_NAME = "MyStateExtension";

    private final ConsumerUtil consumerUtil;
    private final Client client;
    private final SdcRemoteDevicesConnector connector;
    private final DpwsFramework dpwsFramework;
    private final NetworkInterface networkInterface;

    private HostingServiceProxy hostingServiceProxy;
    private SdcRemoteDevice sdcRemoteDevice;

    private final Thread connectorThread;
    private final Lock connectLock;
    private final Condition connectCondition;
    private int connectCount;

    Consumer(ConsumerUtil consumerUtil) throws SocketException, UnknownHostException {
        this.consumerUtil = consumerUtil;

        this.hostingServiceProxy = null;
        this.sdcRemoteDevice = null;

        this.connectCount = 0;
        this.connectLock = new ReentrantLock();
        this.connectCondition = connectLock.newCondition();
        this.connectCount = 0;

        var injector = consumerUtil.getInjector();
        this.dpwsFramework = injector.getInstance(DpwsFramework.class);
        this.client = injector.getInstance(Client.class);
        this.connector = injector.getInstance(SdcRemoteDevicesConnector.class);
        if (consumerUtil.getIface() != null && !consumerUtil.getIface().isBlank()) {
            LOG.info("Starting with interface {}", consumerUtil.getIface());
            this.networkInterface = NetworkInterface.getByName(consumerUtil.getIface());
        } else {
            if (consumerUtil.getAddress() != null && !consumerUtil.getAddress().isBlank()) {
                // bind to adapter matching ip
                LOG.info("Starting with address {}", consumerUtil.getAddress());
                this.networkInterface = NetworkInterface.getByInetAddress(
                        InetAddress.getByName(consumerUtil.getAddress())
                );
            } else {
                // find loopback interface for fallback
                networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
                LOG.info("Starting with fallback default adapter {}", networkInterface);
            }
        }

        this.connectorThread = new Thread(new ConnectorThread());
        connectorThread.setDaemon(true);
    }

    public static void main(String[] args) throws Exception {
        var settings = new ConsumerUtil(args);
        var targetEpr = settings.getEpr();
        if (targetEpr == null || targetEpr.isEmpty()) {
            LOG.error("An EPR is required but was not found (see command line argument --epr)");
            System.exit(1);
        }

        var consumer = new Consumer(settings);
        consumer.startAsync().awaitRunning();

        LOG.info("Press any key to exit");
        try {
            System.in.read();
        } catch (IOException e) {
            // pass and quit
        }
        LOG.info("Shutting down");
        consumer.stopAsync().awaitTerminated();
    }

    protected void startUp() {
        dpwsFramework.setNetworkInterface(networkInterface);
        dpwsFramework.startAsync().awaitRunning();
        client.startAsync().awaitRunning();

        connectorThread.start();

        LOG.info("Starting implicit discovery of hosting service with EPR {}", consumerUtil.getEpr());
        client.registerDiscoveryObserver(new ImplicitDiscovery());

        LOG.info("Starting explicit discovery of hosting service with EPR {}", consumerUtil.getEpr());
        triggerConnect();
    }

    protected void shutDown() {
        connectorThread.interrupt();
        connector.stopAsync().awaitTerminated();
        client.stopAsync().awaitTerminated();
        dpwsFramework.stopAsync().awaitTerminated();
    }

    private void triggerConnect() {
        try (var ignored = AutoLock.lock(connectLock)) {
            connectCount++;
            connectCondition.signalAll();
        }
    }

    class ConnectorThread implements Runnable {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try (var ignored = AutoLock.lock(connectLock)) {
                    if (connectCount == 0) {
                        connectCondition.await();
                        if (connectCount == 0) {
                            continue;
                        }
                    }
                    connectCount--;
                    connect();
                } catch (InterceptorException e) {
                    LOG.warn(e);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        private void connect() throws InterceptorException {
            if (hostingServiceProxy != null) {
                LOG.info("Skip connect, SdcDevice with EPR {} already connected", consumerUtil.getEpr());
                return;
            }
            LOG.info("Connect to EPR {}", consumerUtil.getEpr());

            var hostingServiceFuture = client.connect(consumerUtil.getEpr());
            try {
                hostingServiceProxy = hostingServiceFuture.get(MAX_WAIT_SEC, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                LOG.warn("Explicit discovery failed after {}s. Waiting for device to join the network.", MAX_WAIT_SEC, e);
                return;
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("Explicit discovery failed. Waiting for device to join the network.", e);
                return;
            }

            try {
                var remoteDeviceFuture = connector
                        .connect(
                                hostingServiceProxy,
                                ConnectConfiguration.create(ConnectConfiguration.ALL_EPISODIC_AND_WAVEFORM_REPORTS)
                        );
                sdcRemoteDevice = remoteDeviceFuture.get(MAX_WAIT_SEC, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                LOG.error("Couldn't attach to remote MDIB and subscriptions for {} after {}s", consumerUtil.getEpr(), MAX_WAIT_SEC, e);
                System.exit(1);
            } catch (PrerequisitesException | InterruptedException | ExecutionException e) {
                LOG.error("Couldn't attach to remote MDIB and subscriptions for {}", consumerUtil.getEpr(), e);
                System.exit(1);
            }

            // attach report listener
            sdcRemoteDevice.getMdibAccessObservable().registerObserver(new MdibAccessObserver() {
                @Subscribe
                void onUpdate(AbstractMdibAccessMessage updates) {
                    LOG.info("Received update: {}", updates.getClass().getSimpleName());

                    lookForExtensionFromProvider2Example(updates);
                }

                private void lookForExtensionFromProvider2Example(AbstractMdibAccessMessage updates) {
                    if (updates instanceof MetricStateModificationMessage) {
                        var anonymousExtensions = extractAnonymousExtensionNodesFromMessage((MetricStateModificationMessage) updates);
                        anonymousExtensions.forEach(extensionNode ->
                                LOG.info("Received anonymous extension [{}]{}: '{}'",
                                        EXTENSION_NAMESPACE,
                                        EXTENSION_STATE_NAME,
                                        extensionNode.getTextContent())
                        );

                        var compiledExtensions = extractCompiledExtensionNodesFromMessage((MetricStateModificationMessage) updates);
                        compiledExtensions.forEach(extension ->
                                LOG.info("Received compiled extension {}: '{}'", extension.getClass(), extension)
                        );
                    }
                }

                private List<Node> extractAnonymousExtensionNodesFromMessage(MetricStateModificationMessage message) {
                    return message.getStates().values().stream()
                            // compile stream of extension nodes
                            .flatMap(Collection::stream)
                            .flatMap(e -> FindExtensions.forQName(e, new QName(EXTENSION_NAMESPACE, EXTENSION_STATE_NAME)).stream())
                            .collect(Collectors.toList());
                }

                private List<CompiledExtension> extractCompiledExtensionNodesFromMessage(MetricStateModificationMessage message) {
                    return message.getStates().values().stream()
                            // compile stream of extension nodes
                            .flatMap(Collection::stream)
                            .flatMap(e -> FindExtensions.forClass(e, CompiledExtension.class).stream())
                            .collect(Collectors.toList());
                }
            });
        }
    }

    class ImplicitDiscovery implements DiscoveryObserver {
        @Subscribe
        void deviceEntered(DeviceEnteredMessage message) {
            if (!message.getPayload().getEprAddress().equalsIgnoreCase(consumerUtil.getEpr())) {
                LOG.info("Implicit discovery: EPR mismatch ({})", message.getPayload().getEprAddress());
                return;
            }

            LOG.info("Device with EPR {} entered the network. Try to connect.", consumerUtil.getEpr());
            triggerConnect();
        }
    }
}
