package org.somda.sdc.dpws.client;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.TransportBinding;
import org.somda.sdc.dpws.client.helper.DiscoveredDeviceResolver;
import org.somda.sdc.dpws.client.helper.DiscoveryClientUdpProcessor;
import org.somda.sdc.dpws.client.helper.HelloByeAndProbeMatchesObserverImpl;
import org.somda.sdc.dpws.client.helper.HostingServiceResolver;
import org.somda.sdc.dpws.client.helper.factory.ClientHelperFactory;
import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.guice.ClientSpecific;
import org.somda.sdc.dpws.guice.DiscoveryUdpQueue;
import org.somda.sdc.dpws.guice.NetworkJobThreadPool;
import org.somda.sdc.dpws.helper.NotificationSourceUdpCallback;
import org.somda.sdc.dpws.helper.factory.DpwsHelperFactory;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.NotificationSource;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.factory.NotificationSinkFactory;
import org.somda.sdc.dpws.soap.factory.NotificationSourceFactory;
import org.somda.sdc.dpws.soap.factory.RequestResponseClientFactory;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingServerInterceptor;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsdiscovery.HelloByeAndProbeMatchesObserver;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryClient;
import org.somda.sdc.dpws.soap.wsdiscovery.factory.WsDiscoveryClientFactory;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ProbeMatchesType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ResolveMatchType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ResolveMatchesType;
import org.somda.sdc.dpws.udp.UdpMessageQueueService;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Default implementation of {@linkplain Client}.
 */
public class ClientImpl extends AbstractIdleService implements Client, Service, HelloByeAndProbeMatchesObserver {
    private static final Logger LOG = LogManager.getLogger(ClientImpl.class);

    private final UdpMessageQueueService discoveryMessageQueue;
    private final HostingServiceResolver hostingServiceResolver;
    private final DiscoveryClientUdpProcessor msgProcessor;
    private final HelloByeAndProbeMatchesObserverImpl helloByeAndProbeMatchesObserverImpl;
    private final WsDiscoveryClient wsDiscoveryClient;
    private final ExecutorWrapperService<ListeningExecutorService> executorService;
    private final WsAddressingUtil wsAddressingUtil;
    private final TransportBindingFactory transportBindingFactory;
    private final RequestResponseClientFactory requestResponseClientFactory;
    private final Duration maxWaitForFutures;
    private final Logger instanceLogger;

    @Inject
    ClientImpl(@Named(DpwsConfig.MAX_WAIT_FOR_FUTURES) Duration maxWaitForFutures,
               WsDiscoveryClientFactory discoveryClientFactory,
               NotificationSourceFactory notificationSourceFactory,
               DpwsHelperFactory dpwsHelperFactory,
               @DiscoveryUdpQueue UdpMessageQueueService discoveryMessageQueue,
               NotificationSinkFactory notificationSinkFactory,
               ClientHelperFactory clientHelperFactory,
               @NetworkJobThreadPool ExecutorWrapperService<ListeningExecutorService> executorService,
               WsAddressingUtil wsAddressingUtil,
               TransportBindingFactory transportBindingFactory,
               RequestResponseClientFactory requestResponseClientFactory,
               HostingServiceResolver hostingServiceResolver,
               @ClientSpecific WsAddressingServerInterceptor wsAddressingServerInterceptor,
               @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.maxWaitForFutures = maxWaitForFutures;
        this.discoveryMessageQueue = discoveryMessageQueue;
        this.hostingServiceResolver = hostingServiceResolver;
        this.executorService = executorService;
        this.wsAddressingUtil = wsAddressingUtil;
        this.transportBindingFactory = transportBindingFactory;
        this.requestResponseClientFactory = requestResponseClientFactory;

        // Create binding between a notification source and outgoing UDP messages to send probes and resolves
        NotificationSourceUdpCallback callback =
                dpwsHelperFactory.createNotificationSourceUdpCallback(discoveryMessageQueue);
        // Bind the callback to a notification source
        NotificationSource notificationSource = notificationSourceFactory.createNotificationSource(callback);
        // Connect that notification source to a WS-Discovery client
        wsDiscoveryClient = discoveryClientFactory.createWsDiscoveryClient(notificationSource);

        // Create notification sink for WS-Discovery dedicated to the client side
        var notificationSink = notificationSinkFactory.createNotificationSink(wsAddressingServerInterceptor);

        // Create binding between a notification sink and incoming UDP messages to receive hello, bye, probeMatches and
        // resolveMatches
        msgProcessor = clientHelperFactory.createDiscoveryClientUdpProcessor(notificationSink);

        // Bind the notification sink to the WS-Discovery client that was created before
        notificationSink.register(wsDiscoveryClient);

        // Create device resolver proxy util object
        DiscoveredDeviceResolver discoveredDeviceResolver = clientHelperFactory
                .createDiscoveredDeviceResolver(wsDiscoveryClient);

        // Create observer for WS-Discovery messages
        helloByeAndProbeMatchesObserverImpl = clientHelperFactory.createDiscoveryObserver(discoveredDeviceResolver);
    }

    @Override
    public void probe(DiscoveryFilter discoveryFilter) throws TransportException, InterceptorException {
        checkRunning();

        try {
            wsDiscoveryClient.sendProbe(discoveryFilter.getDiscoveryId(), discoveryFilter.getTypes(),
                    discoveryFilter.getScopes(), discoveryFilter.getMatchBy());
        } catch (MarshallingException e) {
            instanceLogger.error("Marshalling failed while probing for devices", e.getCause());
        }
    }

    @Override
    public ListenableFuture<ProbeMatchesType> directedProbe(String xAddr) {
        checkRunning();

        TransportBinding tBinding = transportBindingFactory.createTransportBinding(xAddr, null);
        RequestResponseClient rrc = requestResponseClientFactory.createRequestResponseClient(tBinding);
        return wsDiscoveryClient.sendDirectedProbe(rrc, new ArrayList<>(), new ArrayList<>(), null);
    }

    @Override
    public ListenableFuture<DiscoveredDevice> resolve(String eprAddress) throws InterceptorException {
        checkRunning();

        try {
            final SettableFuture<DiscoveredDevice> deviceSettableFuture = SettableFuture.create();
            final ListenableFuture<ResolveMatchesType> resolveMatchesFuture = wsDiscoveryClient
                    .sendResolve(wsAddressingUtil.createEprWithAddress(eprAddress));
            Futures.addCallback(resolveMatchesFuture, new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable ResolveMatchesType resolveMatchesType) {
                    if (resolveMatchesType == null) {
                        instanceLogger.warn("Received ResolveMatches with empty payload");
                    } else {
                        final ResolveMatchType rm = resolveMatchesType.getResolveMatch();
                        List<String> scopes = Collections.emptyList();
                        if (rm.getScopes() != null) {
                            scopes = rm.getScopes().getValue();
                        }
                        deviceSettableFuture.set(new DiscoveredDevice(
                                rm.getEndpointReference().getAddress().getValue(),
                                rm.getTypes(),
                                scopes,
                                rm.getXAddrs(),
                                rm.getMetadataVersion()));
                    }
                }

                @Override
                public void onFailure(Throwable throwable) {
                    instanceLogger.trace("Resolve failed.", throwable);
                    deviceSettableFuture.setException(throwable);
                }
            }, executorService.get());

            return deviceSettableFuture;
        } catch (MarshallingException e) {
            instanceLogger.warn("Marshalling failed while probing for devices", e.getCause());
            final SettableFuture<DiscoveredDevice> errorFuture = SettableFuture.create();
            errorFuture.setException(e);
            return errorFuture;
        } catch (TransportException e) {
            instanceLogger.warn("Sending failed on transport layer", e.getCause());
            final SettableFuture<DiscoveredDevice> errorFuture = SettableFuture.create();
            errorFuture.setException(e);
            return errorFuture;
        }
    }

    @Override
    public ListenableFuture<HostingServiceProxy> connect(DiscoveredDevice discoveredDevice) {
        checkRunning();
        return hostingServiceResolver.resolveHostingService(discoveredDevice);
    }

    @Override
    public ListenableFuture<HostingServiceProxy> connect(String eprAddress) throws InterceptorException {
        checkRunning();

        final ListenableFuture<DiscoveredDevice> resolveFuture = resolve(eprAddress);
        final SettableFuture<HostingServiceProxy> hspFuture = SettableFuture.create();

        Futures.addCallback(resolveFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable DiscoveredDevice discoveredDevice) {
                if (discoveredDevice == null) {
                    throw new RuntimeException(String.format("Resolve of %s failed", eprAddress));
                }

                var connectFuture = connect(discoveredDevice);
                try {
                    hspFuture.set(connectFuture.get(maxWaitForFutures.toMillis(), TimeUnit.MILLISECONDS));
                } catch (TimeoutException e) {
                    // cancel task on timeout
                    connectFuture.cancel(true);
                    instanceLogger.debug(
                        "Connecting to {} timed out after {} seconds",
                        eprAddress, maxWaitForFutures.toSeconds(), e
                    );
                    throw new RuntimeException(
                        String.format("Connect of %s timed out after %s seconds",
                            eprAddress, maxWaitForFutures.toSeconds()
                        ));
                } catch (InterruptedException | ExecutionException | CancellationException e) {
                    instanceLogger.debug("Connecting to {} failed", eprAddress, e);
                    throw new RuntimeException(String.format("Connect of %s failed", eprAddress));
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                instanceLogger.trace("Connecting to endpoint {} failed", eprAddress, throwable);
                hspFuture.setException(throwable);
            }
        }, executorService.get());

        return hspFuture;
    }

    @Override
    protected void startUp() {
        discoveryMessageQueue.registerUdpMessageQueueObserver(msgProcessor);
        wsDiscoveryClient.registerHelloByeAndProbeMatchesObserver(helloByeAndProbeMatchesObserverImpl);
    }

    @Override
    protected void shutDown() {
        wsDiscoveryClient.unregisterHelloByeAndProbeMatchesObserver(helloByeAndProbeMatchesObserverImpl);
        discoveryMessageQueue.unregisterUdpMessageQueueObserver(msgProcessor);
    }


    @Override
    public void registerDiscoveryObserver(DiscoveryObserver observer) {
        helloByeAndProbeMatchesObserverImpl.registerDiscoveryObserver(observer);
    }

    @Override
    public void unregisterDiscoveryObserver(DiscoveryObserver observer) {
        helloByeAndProbeMatchesObserverImpl.unregisterDiscoveryObserver(observer);
    }

    private void checkRunning() {
        if (!isRunning()) {
            String msg = "Try to invoke method on non-running client";
            instanceLogger.warn(msg);
            throw new RuntimeException(msg);
        }
    }
}
