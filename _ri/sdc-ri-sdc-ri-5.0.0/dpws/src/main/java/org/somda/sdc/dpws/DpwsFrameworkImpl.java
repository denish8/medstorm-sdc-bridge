package org.somda.sdc.dpws;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.Service;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.guice.AppDelayExecutor;
import org.somda.sdc.dpws.guice.DiscoveryUdpQueue;
import org.somda.sdc.dpws.guice.NetworkJobThreadPool;
import org.somda.sdc.dpws.guice.ResolverThreadPool;
import org.somda.sdc.dpws.guice.WsDiscovery;
import org.somda.sdc.dpws.helper.JaxbMarshalling;
import org.somda.sdc.dpws.http.HttpServerRegistry;
import org.somda.sdc.dpws.http.helper.HttpServerClientSelfTest;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants;
import org.somda.sdc.dpws.udp.UdpBindingService;
import org.somda.sdc.dpws.udp.UdpMessageQueueService;
import org.somda.sdc.dpws.udp.factory.UdpBindingServiceFactory;
import org.somda.sdc.dpws.wsdl.WsdlMarshalling;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Default implementation of {@link DpwsFramework}.
 */
public class DpwsFrameworkImpl extends AbstractIdleService implements DpwsFramework {
    private static final Logger LOG = LogManager.getLogger(DpwsFrameworkImpl.class);

    private NetworkInterface networkInterface;
    private final Logger instanceLogger;
    private final UdpMessageQueueService udpMessageQueueService;
    private final UdpBindingServiceFactory udpBindingServiceFactory;
    private final FrameworkMetadata metadata;

    private final List<Service> registeredServices;
    private UdpBindingService udpBindingService;
    private final HttpServerClientSelfTest httpServerClientSelfTest;

    @Inject
    DpwsFrameworkImpl(@DiscoveryUdpQueue UdpMessageQueueService udpMessageQueueService,
                      UdpBindingServiceFactory udpBindingServiceFactory,
                      HttpServerRegistry httpServerRegistry,
                      JaxbMarshalling jaxbMarshalling,
                      SoapMarshalling soapMarshalling,
                      WsdlMarshalling wsdlMarshalling,
                      @AppDelayExecutor ExecutorWrapperService<ScheduledExecutorService> appDelayExecutor,
                      @NetworkJobThreadPool ExecutorWrapperService<ListeningExecutorService> networkJobExecutor,
                      @WsDiscovery ExecutorWrapperService<ListeningExecutorService> wsDiscoveryExecutor,
                      @ResolverThreadPool ExecutorWrapperService<ListeningExecutorService> resolveExecutor,
                      FrameworkMetadata metadata,
                      @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                      HttpServerClientSelfTest httpServerClientSelfTest) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.udpMessageQueueService = udpMessageQueueService;
        this.udpBindingServiceFactory = udpBindingServiceFactory;
        this.metadata = metadata;
        this.registeredServices = new ArrayList<>();
        registeredServices.addAll(List.of(
                // dpws thread pools
                appDelayExecutor, networkJobExecutor, wsDiscoveryExecutor, resolveExecutor,
                // dpws services
                jaxbMarshalling, soapMarshalling, wsdlMarshalling, httpServerRegistry
        ));
        this.httpServerClientSelfTest = httpServerClientSelfTest;
    }

    @Override
    protected void startUp() throws SocketException, UnknownHostException {
        instanceLogger.info("Start SDCri DPWS framework");
        logMetadata();

        if (networkInterface == null) {
            networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
            instanceLogger.info("Initializing dpws framework with loopback interface {}", networkInterface);
        }

        if (instanceLogger.isDebugEnabled()) {
            printNetworkInterfaceInformation();
        }

        configureDiscovery();
        registeredServices.addAll(List.of(
                // remaining dpws services which depend on the network interface being set,
                // which is why they are added delayed
                udpBindingService, udpMessageQueueService
        ));
        registeredServices.forEach(service -> service.startAsync().awaitRunning());

        httpServerClientSelfTest.testConnection();

        instanceLogger.info("SDCri DPWS framework is ready for use");
    }

    private void printNetworkInterfaceInformation() throws SocketException {
        Iterator<NetworkInterface> networkInterfaceIterator = NetworkInterface.getNetworkInterfaces().asIterator();
        while (networkInterfaceIterator.hasNext()) {
            NetworkInterface netInterface = networkInterfaceIterator.next();
            instanceLogger.debug("Found network interface: [{};isUp={};isLoopBack={},supportsMulticast={},MTU={}," +
                            "isVirtual={}]",
                    netInterface,
                    netInterface.isUp(),
                    netInterface.isLoopback(),
                    netInterface.supportsMulticast(),
                    netInterface.getMTU(),
                    netInterface.isVirtual());
            Iterator<InetAddress> inetAddressIterator = netInterface.getInetAddresses().asIterator();
            int i = 0;
            while (inetAddressIterator.hasNext()) {
                instanceLogger.debug("{}.address[{}]: {}", netInterface.getName(), i++, inetAddressIterator.next());
            }
        }
    }

    @Override
    protected void shutDown() {
        instanceLogger.info("Shutting down SDCri DPWS framework");
        Lists.reverse(registeredServices).forEach(service -> service.stopAsync().awaitTerminated());
        instanceLogger.info("SDCri DPWS framework shut down");
    }

    private void configureDiscovery() throws UnknownHostException, SocketException {
        InetAddress wsdMulticastAddress;
        try {
            wsdMulticastAddress = InetAddress.getByName(WsDiscoveryConstants.IPV4_MULTICAST_ADDRESS);
        } catch (UnknownHostException e) {
            instanceLogger.warn("WS-Discovery multicast port could not be retrieved as InetAddress: {}",
                    e.getMessage());
            throw new RuntimeException(e);
        }

        udpBindingService = udpBindingServiceFactory.createUdpBindingService(
                networkInterface,
                wsdMulticastAddress,
                DpwsConstants.DISCOVERY_PORT,
                DpwsConstants.MAX_UDP_ENVELOPE_SIZE);
        udpMessageQueueService.setUdpBinding(udpBindingService);
        udpBindingService.setMessageReceiver(udpMessageQueueService);
    }

    @Override
    public void setNetworkInterface(NetworkInterface networkInterface) {
        if (isRunning()) {
            instanceLogger.warn("Framework is already running, cannot change network interface");
            return;
        }
        this.networkInterface = networkInterface;
    }

    @Override
    public synchronized void registerService(Collection<Service> services) {
        // don't add any duplicates
        services.forEach(
                service -> {
                    if (!registeredServices.contains(service)) {
                        registeredServices.add(service);
                    }
                }
        );

        // if we're already running, we need to start the service now
        if (isRunning() || state().equals(State.STARTING)) {
            services.forEach(
                    service -> {
                        if (service.state().equals(State.NEW)) {
                            instanceLogger.info("Delayed start of service {}", service);
                            service.startAsync().awaitRunning();
                        } else {
                            service.awaitRunning();
                        }
                    }
            );
        }
    }

    private void logMetadata() {
        instanceLogger.info("SDCri version:\t{}", metadata.getFrameworkVersion());
        instanceLogger.info("Java vendor:\t\t{}", metadata.getJavaVendor());
        instanceLogger.info("Java version:\t{}", metadata.getJavaVersion());
        instanceLogger.info("OS version:\t\t{}", metadata.getOsVersion());
    }

}
