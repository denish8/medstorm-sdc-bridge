package org.somda.sdc.glue.consumer;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.common.access.MdibAccessObserver;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.consumer.access.factory.RemoteMdibAccessFactory;
import org.somda.sdc.biceps.model.message.AbstractReport;
import org.somda.sdc.biceps.model.message.GetContextStatesResponse;
import org.somda.sdc.biceps.model.message.GetMdibResponse;
import org.somda.sdc.biceps.model.message.ObjectFactory;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.biceps.model.participant.Mdib;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.NotificationObject;
import org.somda.sdc.dpws.soap.wseventing.SubscribeResult;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.glue.common.MdibVersionUtil;
import org.somda.sdc.glue.common.ModificationsBuilder;
import org.somda.sdc.glue.common.SubscribableActionsMapping;
import org.somda.sdc.glue.common.WsdlConstants;
import org.somda.sdc.glue.common.factory.ModificationsBuilderFactory;
import org.somda.sdc.glue.consumer.event.RemoteDeviceConnectedMessage;
import org.somda.sdc.glue.consumer.event.RemoteDeviceDisconnectedMessage;
import org.somda.sdc.glue.consumer.event.WatchdogMessage;
import org.somda.sdc.glue.consumer.factory.SdcRemoteDeviceFactory;
import org.somda.sdc.glue.consumer.factory.SdcRemoteDeviceWatchdogFactory;
import org.somda.sdc.glue.consumer.helper.HostingServiceLogger;
import org.somda.sdc.glue.consumer.localization.LocalizationServiceProxy;
import org.somda.sdc.glue.consumer.localization.factory.LocalizationServiceProxyFactory;
import org.somda.sdc.glue.consumer.report.ReportProcessingException;
import org.somda.sdc.glue.consumer.report.ReportProcessor;
import org.somda.sdc.glue.consumer.sco.ScoController;
import org.somda.sdc.glue.consumer.sco.factory.ScoControllerFactory;
import org.somda.sdc.glue.guice.Consumer;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.namespace.QName;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SdcRemoteDevicesConnectorImpl extends AbstractIdleService
        implements SdcRemoteDevicesConnector, WatchdogObserver {
    private static final Logger LOG = LogManager.getLogger(SdcRemoteDevicesConnectorImpl.class);

    private final ExecutorWrapperService<ListeningExecutorService> executorService;
    private final Map<String, SdcRemoteDevice> sdcRemoteDevices;
    private final EventBus eventBus;
    private final Logger instanceLogger;
    private final Provider<ReportProcessor> reportProcessorProvider;
    private final ScoControllerFactory scoControllerFactory;
    private final LocalizationServiceProxyFactory localizationServiceProxyFactory;
    private final Duration requestedExpires;
    private final Duration responseWaitingTime;
    private final SoapUtil soapUtil;
    private final ModificationsBuilderFactory modificationsBuilderFactory;
    private final RemoteMdibAccessFactory remoteMdibAccessFactory;
    private final ObjectFactory messageModelFactory;
    private final MdibVersionUtil mdibVersionUtil;
    private final SdcRemoteDeviceFactory sdcRemoteDeviceFactory;
    private final SdcRemoteDeviceWatchdogFactory watchdogFactory;
    private final String frameworkIdentifier;

    @Inject
    SdcRemoteDevicesConnectorImpl(@Consumer ExecutorWrapperService<ListeningExecutorService> executorService,
                                  ConcurrentHashMap<String, SdcRemoteDevice> sdcRemoteDevices,
                                  EventBus eventBus,
                                  Provider<ReportProcessor> reportProcessorProvider,
                                  ScoControllerFactory scoControllerFactory,
                                  LocalizationServiceProxyFactory localizationServiceProxyFactory,
                                  @Named(ConsumerConfig.REQUESTED_EXPIRES) Duration requestedExpires,
                                  @Named(DpwsConfig.MAX_WAIT_FOR_FUTURES) Duration responseWaitingTime,
                                  SoapUtil soapUtil,
                                  ModificationsBuilderFactory modificationsBuilderFactory,
                                  RemoteMdibAccessFactory remoteMdibAccessFactory,
                                  ObjectFactory messageModelFactory,
                                  MdibVersionUtil mdibVersionUtil,
                                  SdcRemoteDeviceFactory sdcRemoteDeviceFactory,
                                  SdcRemoteDeviceWatchdogFactory watchdogFactory,
                                  DpwsFramework dpwsFramework,
                                  @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.executorService = executorService;
        this.sdcRemoteDevices = sdcRemoteDevices;
        this.eventBus = eventBus;
        this.reportProcessorProvider = reportProcessorProvider;
        this.scoControllerFactory = scoControllerFactory;
        this.localizationServiceProxyFactory = localizationServiceProxyFactory;
        this.requestedExpires = requestedExpires;
        this.responseWaitingTime = responseWaitingTime;
        this.soapUtil = soapUtil;
        this.modificationsBuilderFactory = modificationsBuilderFactory;
        this.remoteMdibAccessFactory = remoteMdibAccessFactory;
        this.messageModelFactory = messageModelFactory;
        this.mdibVersionUtil = mdibVersionUtil;
        this.sdcRemoteDeviceFactory = sdcRemoteDeviceFactory;
        this.watchdogFactory = watchdogFactory;
        this.frameworkIdentifier = frameworkIdentifier;

        dpwsFramework.registerService(List.of(executorService, this));
    }

    @Override
    public ListenableFuture<SdcRemoteDevice> connect(HostingServiceProxy hostingServiceProxy,
                                                     ConnectConfiguration connectConfiguration)
            throws PrerequisitesException {
        return connect(hostingServiceProxy, connectConfiguration, null);
    }

    @Override
    public ListenableFuture<SdcRemoteDevice> connect(HostingServiceProxy hostingServiceProxy,
                                                     ConnectConfiguration connectConfiguration,
                                                     @Nullable MdibAccessObserver mdibAccessObserver)
            throws PrerequisitesException {
        // Early exit if there is a connection already
        checkExistingConnection(hostingServiceProxy);

        // precheck: necessary services present?
        checkRequiredServices(hostingServiceProxy, connectConfiguration.getRequiredPortTypes());

        return executorService.get().submit(() -> {
            try {
                final Logger tempLog = HostingServiceLogger.getLogger(LOG, hostingServiceProxy, frameworkIdentifier);
                tempLog.info("Start connecting");
                ReportProcessor reportProcessor = createReportProcessor();
                Optional<ScoController> scoController = createScoController(hostingServiceProxy);
                LocalizationServiceProxy localizationServiceProxy = createLocalizationServiceProxy(hostingServiceProxy);
                // Map<ServiceId, SubscribeResult>
                // use these later for watchdog, which is in charge of automatic renew
                final Map<String, SubscribeResult> subscribeResults = subscribeServices(hostingServiceProxy,
                        connectConfiguration.getActions(), reportProcessor, scoController.orElse(null));

                // retrieve MDIB after subscribing
                RemoteMdibAccess mdibAccess = createRemoteMdibAccess(hostingServiceProxy, mdibAccessObserver);

                GetContextStatesResponse getContextStatesResponse = null;
                if (mdibAccess.getContextStates().isEmpty()) {
                    tempLog.info("No context states found, try to request separately");
                    getContextStatesResponse = requestContextStates(hostingServiceProxy);
                }

                try {
                    tempLog.info("Start applying reports");
                    reportProcessor.startApplyingReportsOnMdib(mdibAccess, getContextStatesResponse);
                } catch (ReportProcessingException | PreprocessingException e) {
                    throw new PrerequisitesException("Could not start applying reports on remote MDIB access", e);
                }

                tempLog.info("Start watchdog");
                var watchdog =
                        watchdogFactory.createSdcRemoteDeviceWatchdog(hostingServiceProxy, subscribeResults, this);

                tempLog.info("Create and run remote device structure");
                final SdcRemoteDevice sdcRemoteDevice = sdcRemoteDeviceFactory.createSdcRemoteDevice(
                        hostingServiceProxy,
                        mdibAccess,
                        reportProcessor,
                        scoController.orElse(null),
                        watchdog,
                        localizationServiceProxy);
                sdcRemoteDevice.startAsync().awaitRunning();
                tempLog.info("Remote device is running");

                if (sdcRemoteDevices.putIfAbsent(hostingServiceProxy
                        .getEndpointReferenceAddress(), sdcRemoteDevice) == null) {
                    eventBus.post(new RemoteDeviceConnectedMessage(sdcRemoteDevice));
                } else {
                    throw new PrerequisitesException(String.format(
                            "A remote device with EPR address %s was already connected",
                            hostingServiceProxy.getEndpointReferenceAddress()));
                }

                return sdcRemoteDevice;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void checkExistingConnection(HostingServiceProxy hostingServiceProxy) throws PrerequisitesException {
        if (sdcRemoteDevices.get(hostingServiceProxy.getEndpointReferenceAddress()) != null) {
            throw new PrerequisitesException(String.format("A remote device with EPR address %s was already connected",
                    hostingServiceProxy.getEndpointReferenceAddress()));
        }
    }

    private ReportProcessor createReportProcessor() {
        return reportProcessorProvider.get();
    }

    private Optional<ScoController> createScoController(HostingServiceProxy hostingServiceProxy) {
        HostedServiceProxy contextServiceProxy = null;
        try {
            contextServiceProxy = findHostedServiceProxy(hostingServiceProxy, WsdlConstants.PORT_TYPE_CONTEXT_QNAME);
        } catch (PrerequisitesException e) {
            // ignore and proceed with empty context service
        }

        HostedServiceProxy setServiceProxy = null;
        try {
            setServiceProxy = findHostedServiceProxy(hostingServiceProxy, WsdlConstants.PORT_TYPE_SET_QNAME);

        } catch (PrerequisitesException e) {
            // ignore and proceed with empty set service
        }

        if (contextServiceProxy == null && setServiceProxy == null) {
            return Optional.empty();
        }

        return Optional.of(scoControllerFactory.createScoController(
                hostingServiceProxy, setServiceProxy, contextServiceProxy));
    }

    private LocalizationServiceProxy createLocalizationServiceProxy(HostingServiceProxy hostingServiceProxy) {

        HostedServiceProxy localizationServiceProxy = null;
        try {
            localizationServiceProxy = findHostedServiceProxy(hostingServiceProxy,
                    WsdlConstants.PORT_TYPE_LOCALIZATION_QNAME);

        } catch (PrerequisitesException e) {
            // ignore in case localization service is not provided
        }
        return localizationServiceProxyFactory.createLocalizationServiceProxy(hostingServiceProxy,
                localizationServiceProxy);
    }

    @Override
    public ListenableFuture<?> disconnect(String eprAddress) {
        SdcRemoteDevice sdcRemoteDevice = sdcRemoteDevices.remove(eprAddress);
        if (sdcRemoteDevice != null) {
            if (sdcRemoteDevice.isRunning()) {
                return executorService.get().submit(() -> {
                    // invalidate sdcRemoteDevice
                    // unsubscribe everything
                    sdcRemoteDevice.stopAsync().awaitTerminated();
                    eventBus.post(new RemoteDeviceDisconnectedMessage(URI.create(eprAddress)));
                });
            }
        } else {
            instanceLogger.info("disconnect() called for unknown epr address {}, device already disconnected?",
                    eprAddress);
        }
        return Futures.immediateCancelledFuture();
    }

    @Override
    public Collection<SdcRemoteDevice> getConnectedDevices() {
        return new ArrayList<>(sdcRemoteDevices.values());
    }

    @Override
    public Optional<SdcRemoteDevice> getConnectedDevice(String eprAddress) {
        return Optional.ofNullable(sdcRemoteDevices.get(eprAddress));
    }

    @Override
    public void registerObserver(SdcRemoteDevicesObserver observer) {
        eventBus.register(observer);
    }

    @Override
    public void unregisterObserver(SdcRemoteDevicesObserver observer) {
        eventBus.unregister(observer);
    }

    private void checkRequiredServices(HostingServiceProxy hostingServiceProxy,
                                       Collection<QName> requiredPortTypes) throws PrerequisitesException {
        List<QName> nonFoundPortTypes = new LinkedList<>(requiredPortTypes);
        hostingServiceProxy.getHostedServices().values().forEach(hostedServiceProxy ->
                nonFoundPortTypes.removeAll(hostedServiceProxy.getType().getTypes()));
        if (!nonFoundPortTypes.isEmpty()) {
            throw new PrerequisitesException(String.format("Required port types not found: %s", nonFoundPortTypes));
        }
    }

    private Map<String, SubscribeResult> subscribeServices(HostingServiceProxy hostingServiceProxy,
                                                           Collection<String> actionsToSubscribe,
                                                           ReportProcessor reportProcessor,
                                                           @Nullable ScoController scoController)
            throws PrerequisitesException {
        // Multimap<ServiceId, ActionUri>
        final Multimap<String, String> subscriptions =
                getServiceIdWithActionsToSubscribe(hostingServiceProxy, actionsToSubscribe);
        Map<String, SubscribeResult> subscribeResults = new HashMap<>(subscriptions.size());

        for (String serviceId : subscriptions.keySet()) {
            final Collection<String> actions = subscriptions.get(serviceId);
            if (actions.isEmpty()) {
                instanceLogger.warn("Expect to find at least one action to subscribe for service id {}, " +
                        "but none found", serviceId);
                continue;
            }
            final HostedServiceProxy hostedServiceProxy = hostingServiceProxy.getHostedServices().get(serviceId);
            if (hostedServiceProxy == null) {
                instanceLogger.warn("Expect to found a hosted service proxy to access for service id {}, " +
                        "but none found", serviceId);
                continue;
            }

            final ListenableFuture<SubscribeResult> subscribeResult = hostedServiceProxy.getEventSinkAccess().subscribe(
                    new ArrayList<>(actions),
                    requestedExpires,
                    new Interceptor() {
                        @MessageInterceptor
                        void onNotification(NotificationObject notificationObject) {
                            final AbstractReport report = soapUtil.getBody(
                                notificationObject.getNotification(),
                                AbstractReport.class
                            ).orElseThrow(
                                () -> new RuntimeException(
                                    String.format("Received unexpected report message from service %s", serviceId)
                                ));
                            instanceLogger.debug("Incoming SOAP/HTTP notification: {}", report);
                            if (report instanceof OperationInvokedReport) {
                                if (scoController != null) {
                                    scoController.processOperationInvokedReport((OperationInvokedReport) report);
                                }
                            } else {
                                reportProcessor.processReport(report);
                            }
                        }
                    });

            try {
                subscribeResults.put(serviceId, subscribeResult.get(responseWaitingTime.toSeconds(), TimeUnit.SECONDS));
            } catch (TimeoutException e) {
                subscribeResult.cancel(true);
                throw new PrerequisitesException(
                    String.format(
                        "Subscribe request towards service with service id %s failed after %ss. " +
                            "Physical target address: %s",
                        serviceId,
                        responseWaitingTime.toSeconds(),
                        hostedServiceProxy.getActiveEprAddress()
                    ), e);
            } catch (InterruptedException | ExecutionException e) {
                throw new PrerequisitesException(
                        String.format(
                            "Subscribe request towards service with service id %s failed. " +
                                "Physical target address: %s",
                                serviceId,
                                hostedServiceProxy.getActiveEprAddress()
                        ), e);
            }
        }

        return subscribeResults;
    }

    private Multimap<String, String> getServiceIdWithActionsToSubscribe(HostingServiceProxy hostingServiceProxy,
                                                                        Collection<String> actionsToSubscribe) {
        Multimap<String, String> subscriptions = ArrayListMultimap.create();
        for (String action : actionsToSubscribe) {
            final QName targetPortType = SubscribableActionsMapping.TARGET_QNAMES.get(action);
            if (targetPortType == null) {
                instanceLogger.warn("Found an action that could not be mapped to a target port type: {}", action);
                continue;
            }

            for (HostedServiceProxy hostedServiceProxy : hostingServiceProxy.getHostedServices().values()) {
                if (hostedServiceProxy.getType().getTypes().contains(targetPortType)) {
                    subscriptions.put(hostedServiceProxy.getType().getServiceId(), action);
                }
            }
        }

        return subscriptions;
    }

    private RemoteMdibAccess createRemoteMdibAccess(HostingServiceProxy hostingServiceProxy, @Nullable MdibAccessObserver mdibAccessObserver)
            throws PrerequisitesException {
        // find get service
        final HostedServiceProxy getServiceProxy =
                findHostedServiceProxy(hostingServiceProxy, WsdlConstants.PORT_TYPE_GET_QNAME);

        final RemoteMdibAccess mdibAccess = remoteMdibAccessFactory.createRemoteMdibAccess();
        if (mdibAccessObserver != null) {
            mdibAccess.registerObserver(mdibAccessObserver);
        }

        try {
            final SoapMessage getMdibResponseMessage = getServiceProxy.getRequestResponseClient().sendRequestResponse(
                    soapUtil.createMessage(ActionConstants.ACTION_GET_MDIB, messageModelFactory.createGetMdib()));
            final GetMdibResponse getMdibResponse = soapUtil.getBody(getMdibResponseMessage,
                    GetMdibResponse.class).orElseThrow(() -> new PrerequisitesException(
                    "Remote endpoint did not send a GetMdibResponse message in response to " +
                            String.format("a GetMdib to service %s with physical address %s",
                                    getServiceProxy.getType().getServiceId(), getServiceProxy.getActiveEprAddress())));

            final Mdib mdib = getMdibResponse.getMdib();
            final ModificationsBuilder modBuilder = modificationsBuilderFactory.createModificationsBuilder(mdib);

            mdibAccess.writeDescription(
                    mdibVersionUtil.getMdibVersion(getMdibResponse),
                    mdib.getMdDescription().getDescriptionVersion(),
                    mdib.getMdState().getStateVersion(),
                    modBuilder.get());

        } catch (MarshallingException | InterceptorException | TransportException | SoapFaultException e) {
            throw new PrerequisitesException(String.format(
                    "Could not send a GetMdib request to service %s with physical address %s",
                    getServiceProxy.getType().getServiceId(), getServiceProxy.getActiveEprAddress()), e);
        } catch (PreprocessingException e) {
            throw new PrerequisitesException("Could not write initial MDIB to remote MDIB access", e);
        }

        return mdibAccess;
    }

    private HostedServiceProxy findHostedServiceProxy(HostingServiceProxy hostingServiceProxy, QName portType)
            throws PrerequisitesException {
        HostedServiceProxy foundProxy = null;
        for (HostedServiceProxy hostedServiceProxy : hostingServiceProxy.getHostedServices().values()) {
            if (hostedServiceProxy.getType().getTypes().contains(portType)) {
                foundProxy = hostedServiceProxy;
                break;
            }
        }

        if (foundProxy == null) {
            throw new PrerequisitesException(
                    String.format("Service port type %s not found for remote device with UUID %s and " +
                                    "physical target address %s",
                            portType,
                            hostingServiceProxy.getEndpointReferenceAddress(),
                            hostingServiceProxy.getActiveXAddr()));
        }

        return foundProxy;
    }

    private GetContextStatesResponse requestContextStates(HostingServiceProxy hostingServiceProxy)
            throws PrerequisitesException {
        final HostedServiceProxy contextServiceProxy =
                findHostedServiceProxy(hostingServiceProxy, WsdlConstants.PORT_TYPE_CONTEXT_QNAME);
        try {
            final SoapMessage getContextStatesResponseMessage =
                    contextServiceProxy.getRequestResponseClient().sendRequestResponse(
                            soapUtil.createMessage(ActionConstants.ACTION_GET_CONTEXT_STATES,
                                    messageModelFactory.createGetContextStates()));
            return soapUtil.getBody(getContextStatesResponseMessage,
                    GetContextStatesResponse.class).orElseThrow(() -> new PrerequisitesException(
                    "Remote endpoint did not send a GetContextStatesResponse message in response to " +
                            String.format("a GetContextStates to service %s with physical address %s",
                                    contextServiceProxy.getType().getServiceId(),
                                    contextServiceProxy.getActiveEprAddress())));
        } catch (MarshallingException | InterceptorException | TransportException | SoapFaultException e) {
            throw new PrerequisitesException(String.format(
                    "Could not send a GetContextStates request to service %s with physical address %s",
                    contextServiceProxy.getType().getServiceId(), contextServiceProxy.getActiveEprAddress()), e);
        }
    }

    @Subscribe
    void onConnectionLoss(WatchdogMessage watchdogMessage) {
        instanceLogger.info("Lost connection to device {}. Reason: {}", watchdogMessage.getPayload(),
                watchdogMessage.getReason().getMessage());
        disconnect(watchdogMessage.getPayload());
    }

    @Override
    protected void startUp() throws Exception {
        // nothing to do here
    }

    @Override
    protected void shutDown() throws Exception {
        instanceLogger.info("Shutting down, disconnecting all devices");
        List.copyOf(sdcRemoteDevices.keySet()).forEach(this::disconnect);
    }
}

