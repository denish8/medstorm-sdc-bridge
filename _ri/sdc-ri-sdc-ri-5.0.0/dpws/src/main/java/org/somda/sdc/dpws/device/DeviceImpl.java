package org.somda.sdc.dpws.device;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.device.helper.DiscoveryDeviceUdpMessageProcessor;
import org.somda.sdc.dpws.device.helper.RequestResponseServerHttpHandler;
import org.somda.sdc.dpws.device.helper.UriBaseContextPath;
import org.somda.sdc.dpws.device.helper.factory.DeviceHelperFactory;
import org.somda.sdc.dpws.guice.DiscoveryUdpQueue;
import org.somda.sdc.dpws.helper.factory.DpwsHelperFactory;
import org.somda.sdc.dpws.http.HttpServerRegistry;
import org.somda.sdc.dpws.http.HttpUriBuilder;
import org.somda.sdc.dpws.model.ThisDeviceType;
import org.somda.sdc.dpws.model.ThisModelType;
import org.somda.sdc.dpws.network.NetworkInterfaceUtil;
import org.somda.sdc.dpws.service.HostedService;
import org.somda.sdc.dpws.service.HostedServiceInterceptor;
import org.somda.sdc.dpws.service.HostingService;
import org.somda.sdc.dpws.service.factory.HostedServiceFactory;
import org.somda.sdc.dpws.service.factory.HostedServiceInterceptorFactory;
import org.somda.sdc.dpws.service.factory.HostingServiceFactory;
import org.somda.sdc.dpws.soap.NotificationSource;
import org.somda.sdc.dpws.soap.RequestResponseServer;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.factory.NotificationSourceFactory;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryTargetService;
import org.somda.sdc.dpws.soap.wsdiscovery.factory.WsDiscoveryTargetServiceFactory;
import org.somda.sdc.dpws.soap.wseventing.EventSourceInterceptorDispatcher;
import org.somda.sdc.dpws.soap.wseventing.SubscriptionManager;
import org.somda.sdc.dpws.soap.wseventing.factory.EventSourceInterceptorDispatcherFactory;
import org.somda.sdc.dpws.udp.UdpMessageQueueService;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Default implementation of {@linkplain Device}, {@linkplain DiscoveryAccess} and {@linkplain HostingServiceAccess}.
 * <p>
 * todo DGr no support for hosting and hosted services being updated during runtime.
 */
public class DeviceImpl extends AbstractIdleService implements Device, Service, DiscoveryAccess, HostingServiceAccess {
    private static final Logger LOG = LogManager.getLogger(DeviceImpl.class);

    private final DeviceSettings deviceSettings;
    private final WsDiscoveryTargetServiceFactory targetServiceFactory;
    private final WsAddressingUtil wsaUtil;
    private final NotificationSourceFactory notificationSourceFactory;
    private final DeviceHelperFactory deviceHelperFactory;
    private final DpwsHelperFactory dpwsHelperFactory;
    private final HostingServiceFactory hostingServiceFactory;
    private final HttpServerRegistry httpServerRegistry;
    private final RequestResponseServer wsdRequestResponseInterceptorChain;
    private final UdpMessageQueueService discoveryMessageQueue;
    private final Provider<RequestResponseServerHttpHandler> reqResHandlerProvider;
    private final HostedServiceFactory hostedServiceFactory;
    private final HostedServiceInterceptorFactory hostedServiceInterceptorFactory;
    private final String eprAddress;
    private final EventSourceInterceptorDispatcherFactory eventSourceInterceptorDispatcherFactory;
    private final Logger instanceLogger;
    private final NetworkInterfaceUtil networkInterfaceUtil;
    private final HttpUriBuilder httpUriBuilder;
    private final boolean enableHttps;
    private final boolean enableHttp;

    private final List<HostedService> hostedServicesOnStartup;

    private WsDiscoveryTargetService wsdTargetService;
    private HostingService hostingService;
    private Collection<String> scopesOnStartup;
    private List<QName> typesOnStartup;
    private ThisDeviceType thisDeviceOnStartup;
    private ThisModelType thisModelOnStartup;
    private DiscoveryDeviceUdpMessageProcessor udpMsgProcessor;

    private final List<EventSourceInterceptorDispatcher> eventSourceDispatchers;

    @AssistedInject
    DeviceImpl(@Assisted DeviceSettings deviceSettings,
               WsDiscoveryTargetServiceFactory targetServiceFactory,
               WsAddressingUtil wsaUtil,
               NotificationSourceFactory notificationSourceFactory,
               DeviceHelperFactory deviceHelperFactory,
               DpwsHelperFactory dpwsHelperFactory,
               RequestResponseServer wsdRequestResponseInterceptorChain,
               HostingServiceFactory hostingServiceFactory,
               HttpServerRegistry httpServerRegistry,
               Provider<RequestResponseServerHttpHandler> reqResHandlerProvider,
               @DiscoveryUdpQueue UdpMessageQueueService discoveryMessageQueue,
               HostedServiceFactory hostedServiceFactory,
               HostedServiceInterceptorFactory hostedServiceInterceptorFactory,
               NetworkInterfaceUtil networkInterfaceUtil,
               HttpUriBuilder httpUriBuilder,
               EventSourceInterceptorDispatcherFactory eventSourceInterceptorDispatcherFactory,
               @Named(DpwsConfig.HTTPS_SUPPORT) boolean enableHttps,
               @Named(DpwsConfig.HTTP_SUPPORT) boolean enableHttp,
               @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.eventSourceInterceptorDispatcherFactory = eventSourceInterceptorDispatcherFactory;
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.deviceSettings = deviceSettings;
        this.targetServiceFactory = targetServiceFactory;
        this.wsaUtil = wsaUtil;
        this.notificationSourceFactory = notificationSourceFactory;
        this.deviceHelperFactory = deviceHelperFactory;
        this.dpwsHelperFactory = dpwsHelperFactory;
        this.hostingServiceFactory = hostingServiceFactory;
        this.httpServerRegistry = httpServerRegistry;
        this.wsdRequestResponseInterceptorChain = wsdRequestResponseInterceptorChain;
        this.discoveryMessageQueue = discoveryMessageQueue;
        this.reqResHandlerProvider = reqResHandlerProvider;
        this.hostedServiceFactory = hostedServiceFactory;
        this.hostedServiceInterceptorFactory = hostedServiceInterceptorFactory;
        this.networkInterfaceUtil = networkInterfaceUtil;
        this.httpUriBuilder = httpUriBuilder;
        this.enableHttps = enableHttps;
        this.enableHttp = enableHttp;
        this.hostedServicesOnStartup = new ArrayList<>();
        this.eventSourceDispatchers = new ArrayList<>();

        this.eprAddress = wsaUtil.getAddressUri(deviceSettings.getEndpointReference()).orElseThrow(() ->
                new RuntimeException("No valid endpoint reference found in device deviceSettings"));
    }

    @Override
    protected void startUp() throws Exception {
        EndpointReferenceType deviceEpr = deviceSettings.getEndpointReference();
        instanceLogger.info("Start device with EPR address '{}'", deviceEpr.getAddress().getValue());

        String hostingServerCtxtPath = buildContextPathBase(eprAddress);

        // Initialize HTTP servers
        List<String> actualHostingServiceBindings = resolveHostingServiceBindings().stream()
                .map(addr -> httpServerRegistry.initHttpServer(addr, false))
                // create http and https prefixed versions if needed
                .flatMap(uri -> {
                    var baseUri = URI.create(uri);
                    var resultUris = new ArrayList<String>(2);
                    if (enableHttps) {
                        try {
                            resultUris.add(replaceScheme(baseUri, "https"));
                        } catch (URISyntaxException e) {
                            instanceLogger.error("Error while creating https URI", e);
                        }
                    }
                    if (enableHttp) {
                        try {
                            resultUris.add(replaceScheme(baseUri, "http"));
                        } catch (URISyntaxException e) {
                            instanceLogger.error("Error while creating http URI", e);
                        }
                    }
                    return resultUris.stream();
                })
                .collect(Collectors.toList());

        /*
         * Configure WS-Discovery
         */

        // Bind notification source to discovery message queue to allow the target service to send notifications
        NotificationSource wsdNotificationSource = notificationSourceFactory.createNotificationSource(
                dpwsHelperFactory.createNotificationSourceUdpCallback(discoveryMessageQueue));

        // Create WS-Discovery target service
        wsdTargetService = targetServiceFactory.createWsDiscoveryTargetService(deviceEpr, wsdNotificationSource);
        wsdTargetService.setXAddrs(actualHostingServiceBindings.stream()
                .map(uriString -> uriString + hostingServerCtxtPath)
                .collect(Collectors.toList()));

        // Register target service to a request response server interceptor chain
        wsdRequestResponseInterceptorChain.register(wsdTargetService);

        // Create broker for request response server with UDP messages
        udpMsgProcessor = deviceHelperFactory
                .createDiscoveryDeviceUdpMessageProcessor(wsdRequestResponseInterceptorChain, discoveryMessageQueue);
        // Bind request response server to discovery message queue to get notified on incoming UDP messages
        discoveryMessageQueue.registerUdpMessageQueueObserver(udpMsgProcessor);



        /*
         * Configure Hosting Service
         */

        RequestResponseServerHttpHandler reqResHandler = reqResHandlerProvider.get();

        // Register HTTP bindings to HTTP context registry; append EPR UUID from host as context path
        actualHostingServiceBindings.forEach(uri -> httpServerRegistry.registerContext(uri, true,
                hostingServerCtxtPath, null, null, reqResHandler));
        // Create hosting service
        hostingService = hostingServiceFactory.createHostingService(wsdTargetService);
        // Register request-response hosting service interceptor to receive incoming request-response messages
        reqResHandler.register(hostingService);
        // Allow WS-Discovery to react on incoming HTTP requests
        reqResHandler.register(wsdTargetService);

        hostedServicesOnStartup.forEach(this::addHostedServiceToHostingService);
        eventSourceDispatchers.forEach(dispatcher -> dispatcher.startAsync().awaitRunning());
        hostingService.getHostedServices().forEach(hostedService ->
                hostedService.getWebService().startAsync().awaitRunning());
        Optional.ofNullable(thisDeviceOnStartup).ifPresent(thisDeviceType ->
                hostingService.setThisDevice(thisDeviceType));
        Optional.ofNullable(thisModelOnStartup).ifPresent(thisModelType ->
                hostingService.setThisModel(thisModelType));

        wsdTargetService.setTypes(appendDpwsType(typesOnStartup));
        wsdTargetService.setScopes(scopesAsStrs(scopesOnStartup));

        instanceLogger.info("Device {} is running", hostingService);

        wsdTargetService.sendHello();
    }

    private List<String> resolveHostingServiceBindings() {
        InetAddress address = networkInterfaceUtil.getFirstIpV4Address(deviceSettings.getNetworkInterface())
                .orElseThrow(() ->
                        new RuntimeException(
                                String.format("No required IPv4 address found in configured network interface %s",
                                        deviceSettings.getNetworkInterface())));

        final List<String> hostingServiceBindings = new ArrayList<>();

        // prefer https over http
        if (enableHttps) {
            hostingServiceBindings.add(httpUriBuilder.buildSecuredUri(address.getHostAddress(), 0));
        } else {
            hostingServiceBindings.add(httpUriBuilder.buildUri(address.getHostAddress(), 0));
        }

        return hostingServiceBindings;
    }

    @Override
    protected void shutDown() throws Exception {
        instanceLogger.info("Shut down device {}", hostingService);
        wsdTargetService.sendBye();
        eventSourceDispatchers.forEach(it -> it.stopAsync().awaitTerminated());
        hostingService.getHostedServices().forEach(hostedService ->
                hostedService.getWebService().stopAsync().awaitTerminated());
        httpServerRegistry.stopAsync().awaitTerminated();
        discoveryMessageQueue.unregisterUdpMessageQueueObserver(udpMsgProcessor);
        instanceLogger.info("Device {} shut down", hostingService);
    }

    @Override
    public Map<String, SubscriptionManager> getActiveSubscriptions() {
        // merge all subscriptions
        return eventSourceDispatchers.stream()
                .flatMap(source -> source.getActiveSubscriptions().entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public DiscoveryAccess getDiscoveryAccess() {
        return this;
    }

    @Override
    public HostingServiceAccess getHostingServiceAccess() {
        return this;
    }

    @Override
    public String getEprAddress() {
        return eprAddress;
    }

    @Override
    public void setTypes(Collection<QName> types) {
        var typesWithDpwsQName = appendDpwsType(types);
        if (isRunning()) {
            wsdTargetService.setTypes(typesWithDpwsQName);
        } else {
            typesOnStartup = typesWithDpwsQName;
        }
    }

    private List<QName> appendDpwsType(@Nullable Collection<QName> types) {
        var copyTypes = types;
        if (copyTypes == null) {
            copyTypes = Collections.emptyList();
        }
        var tmpTypes = new ArrayList<QName>(copyTypes.size() + 1);
        if (copyTypes.stream().filter(qName -> qName.equals(DpwsConstants.DEVICE_TYPE)).findAny().isEmpty()) {
            tmpTypes.add(DpwsConstants.DEVICE_TYPE);
        }
        tmpTypes.addAll(copyTypes);
        return tmpTypes;
    }

    @Override
    public void setScopes(Collection<String> scopes) {
        if (isRunning()) {
            wsdTargetService.setScopes(scopesAsStrs(scopes));
        } else {
            scopesOnStartup = scopes;
        }
    }

    @Override
    public void sendHello() {
        if (isRunning()) {
            try {
                wsdTargetService.sendHello(false);
            } catch (MarshallingException | TransportException | InterceptorException e) {
                instanceLogger.warn("Send Hello failed.", e);
            }
        }
    }

    private List<String> scopesAsStrs(@Nullable Collection<String> scopes) {
        var copyScopes = scopes;
        if (copyScopes == null) {
            copyScopes = Collections.emptyList();
        }
        return new ArrayList<>(copyScopes);
    }

    @Override
    public void setThisDevice(ThisDeviceType thisDevice) {
        if (isRunning()) {
            hostingService.setThisDevice(thisDevice);
        } else {
            thisDeviceOnStartup = thisDevice;
        }

    }

    @Override
    public void setThisModel(ThisModelType thisModel) {
        if (isRunning()) {
            hostingService.setThisModel(thisModel);
        } else {
            thisModelOnStartup = thisModel;
        }
    }

    @Override
    public void addHostedService(HostedService hostedService) {
        if (isRunning()) {
            addHostedServiceToHostingService(hostedService);
        } else {
            hostedServicesOnStartup.add(hostedService);
        }
    }

    private void addHostedServiceToHostingService(HostedService hostedService) {
        var copyHostedService = hostedService;

        final var dispatcher = eventSourceInterceptorDispatcherFactory.create(
                copyHostedService.getWebService().getEventSources()
        );
        eventSourceDispatchers.add(dispatcher);

        // Create request response handler interceptor specific to the added hosted service
        RequestResponseServerHttpHandler hsReqResHandler = reqResHandlerProvider.get();
        // Add event source dispatcher to HTTP req-res-server for event source management
        hsReqResHandler.register(dispatcher);


        // Create Web Service access path
        String contextPathPart = buildContextPathPart(copyHostedService.getType().getServiceId());

        // If no EPR addresses are given already, create one/some from hosting service
        if (copyHostedService.getType().getEndpointReference().isEmpty()) {
            List<String> uris = hostingService.getXAddrs().stream()
                    .map(uri -> uri + contextPathPart)
                    .collect(Collectors.toList());

            copyHostedService = hostedServiceFactory.createHostedService(copyHostedService.getType().getServiceId(),
                    copyHostedService.getType().getTypes(), uris, copyHostedService.getWebService(),
                    copyHostedService.getWsdlDocument());
        }

        String contextPath = buildContextPathBase(hostingService.getEndpointReferenceAddress()) + contextPathPart;
        for (EndpointReferenceType epr : copyHostedService.getType().getEndpointReference()) {
            var uri = wsaUtil.getAddressUri(epr).orElseThrow(() ->
                    new RuntimeException("Invalid EPR detected when trying to add hosted service"));
            httpServerRegistry.registerContext(uri, true, contextPath, null, null, hsReqResHandler);
        }

        // Create hosted service interceptor to access GetMetadata requests
        HostedServiceInterceptor hsInterceptor = hostedServiceInterceptorFactory.createHostedServiceInterceptor(
                copyHostedService, wsdTargetService);
        // Register interceptor at HTTP req-res-handler
        hsReqResHandler.register(hsInterceptor);

        // Register Web Service interceptor at HTTP req-res-handler
        hsReqResHandler.register(copyHostedService.getWebService());

        // Add hosted service to hosting service to get metadata descriptions updated
        hostingService.addHostedService(copyHostedService);

        // @todo Send out Hello with metadata version increment
    }

    private String buildContextPathPart(String serviceId) {
        return "/" + serviceId;
    }

    private String buildContextPathBase(String uri) {
        final String basePath = new UriBaseContextPath(uri).get();
        return basePath.isEmpty() ? "" : "/" + basePath;
    }

    private String replaceScheme(URI baseUri, String scheme) throws URISyntaxException {
        return new URI(scheme, baseUri.getUserInfo(),
                baseUri.getHost(), baseUri.getPort(),
                baseUri.getPath(), baseUri.getQuery(),
                baseUri.getFragment()).toString();
    }
}
