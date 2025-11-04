package org.somda.sdc.dpws.guice;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Named;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.CommunicationLogDummyImpl;
import org.somda.sdc.dpws.CommunicationLogSink;
import org.somda.sdc.dpws.CommunicationLogSinkImpl;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.DpwsFrameworkImpl;
import org.somda.sdc.dpws.client.Client;
import org.somda.sdc.dpws.client.ClientImpl;
import org.somda.sdc.dpws.client.helper.DiscoveredDeviceResolver;
import org.somda.sdc.dpws.client.helper.DiscoveryClientUdpProcessor;
import org.somda.sdc.dpws.client.helper.HelloByeAndProbeMatchesObserverImpl;
import org.somda.sdc.dpws.client.helper.factory.ClientHelperFactory;
import org.somda.sdc.dpws.device.Device;
import org.somda.sdc.dpws.device.DeviceImpl;
import org.somda.sdc.dpws.device.factory.DeviceFactory;
import org.somda.sdc.dpws.device.helper.DiscoveryDeviceUdpMessageProcessor;
import org.somda.sdc.dpws.device.helper.factory.DeviceHelperFactory;
import org.somda.sdc.dpws.factory.CommunicationLogFactory;
import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.helper.JaxbMarshalling;
import org.somda.sdc.dpws.helper.NotificationSourceUdpCallback;
import org.somda.sdc.dpws.helper.factory.DpwsHelperFactory;
import org.somda.sdc.dpws.http.HttpServerRegistry;
import org.somda.sdc.dpws.http.apache.ApacheTransportBindingFactoryImpl;
import org.somda.sdc.dpws.http.apache.ClientTransportBindingFactory;
import org.somda.sdc.dpws.http.factory.HttpClientFactory;
import org.somda.sdc.dpws.http.jetty.JettyHttpServerRegistry;
import org.somda.sdc.dpws.http.jetty.factory.JettyHttpServerHandlerFactory;
import org.somda.sdc.dpws.network.LocalAddressResolver;
import org.somda.sdc.dpws.network.LocalAddressResolverImpl;
import org.somda.sdc.dpws.service.HostedService;
import org.somda.sdc.dpws.service.HostedServiceImpl;
import org.somda.sdc.dpws.service.HostedServiceInterceptor;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostedServiceProxyImpl;
import org.somda.sdc.dpws.service.HostedServiceTransportBinding;
import org.somda.sdc.dpws.service.HostingService;
import org.somda.sdc.dpws.service.HostingServiceInterceptor;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.service.HostingServiceProxyImpl;
import org.somda.sdc.dpws.service.factory.HostedServiceFactory;
import org.somda.sdc.dpws.service.factory.HostedServiceInterceptorFactory;
import org.somda.sdc.dpws.service.factory.HostedServiceTransportBindingFactory;
import org.somda.sdc.dpws.service.factory.HostingServiceFactory;
import org.somda.sdc.dpws.soap.JaxbSoapMarshalling;
import org.somda.sdc.dpws.soap.NotificationSink;
import org.somda.sdc.dpws.soap.NotificationSinkImpl;
import org.somda.sdc.dpws.soap.NotificationSource;
import org.somda.sdc.dpws.soap.NotificationSourceImpl;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.dpws.soap.RequestResponseClientImpl;
import org.somda.sdc.dpws.soap.RequestResponseServer;
import org.somda.sdc.dpws.soap.RequestResponseServerImpl;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.factory.NotificationSinkFactory;
import org.somda.sdc.dpws.soap.factory.NotificationSourceFactory;
import org.somda.sdc.dpws.soap.factory.RequestResponseClientFactory;
import org.somda.sdc.dpws.soap.factory.SoapMessageFactory;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingClientInterceptor;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingServerInterceptor;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryClient;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryClientInterceptor;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryTargetService;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryTargetServiceInterceptor;
import org.somda.sdc.dpws.soap.wsdiscovery.factory.WsDiscoveryClientFactory;
import org.somda.sdc.dpws.soap.wsdiscovery.factory.WsDiscoveryTargetServiceFactory;
import org.somda.sdc.dpws.soap.wseventing.EventSink;
import org.somda.sdc.dpws.soap.wseventing.EventSinkImpl;
import org.somda.sdc.dpws.soap.wseventing.EventSourceInterceptorDispatcher;
import org.somda.sdc.dpws.soap.wseventing.GenericEventSource;
import org.somda.sdc.dpws.soap.wseventing.SinkSubscriptionManager;
import org.somda.sdc.dpws.soap.wseventing.SinkSubscriptionManagerImpl;
import org.somda.sdc.dpws.soap.wseventing.SourceSubscriptionManager;
import org.somda.sdc.dpws.soap.wseventing.SourceSubscriptionManagerImpl;
import org.somda.sdc.dpws.soap.wseventing.factory.EventSourceInterceptorDispatcherFactory;
import org.somda.sdc.dpws.soap.wseventing.factory.GenericEventSourceInterceptorFactory;
import org.somda.sdc.dpws.soap.wseventing.factory.SubscriptionManagerFactory;
import org.somda.sdc.dpws.soap.wseventing.factory.SubscriptionRegistryFactory;
import org.somda.sdc.dpws.soap.wseventing.factory.WsEventingEventSinkFactory;
import org.somda.sdc.dpws.soap.wseventing.helper.SubscriptionRegistry;
import org.somda.sdc.dpws.soap.wsmetadataexchange.GetMetadataClient;
import org.somda.sdc.dpws.soap.wsmetadataexchange.GetMetadataClientImpl;
import org.somda.sdc.dpws.soap.wstransfer.TransferGetClient;
import org.somda.sdc.dpws.soap.wstransfer.TransferGetClientImpl;
import org.somda.sdc.dpws.udp.UdpBindingService;
import org.somda.sdc.dpws.udp.UdpBindingServiceImpl;
import org.somda.sdc.dpws.udp.UdpMessageQueueService;
import org.somda.sdc.dpws.udp.UdpMessageQueueServiceImpl;
import org.somda.sdc.dpws.udp.factory.UdpBindingServiceFactory;
import org.somda.sdc.dpws.wsdl.JaxbWsdlMarshalling;
import org.somda.sdc.dpws.wsdl.WsdlMarshalling;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Default Guice module to bind all interfaces and factories used by the DPWS implementation.
 */
public class DefaultDpwsModule extends AbstractModule {

    private ExecutorWrapperService<ScheduledExecutorService> appDelayExecutor;
    private ExecutorWrapperService<ListeningExecutorService> networkJobThreadPoolExecutor;
    private ExecutorWrapperService<ListeningExecutorService> wsDiscoveryExecutor;
    private ExecutorWrapperService<ListeningExecutorService> resolveExecutor;

    /**
     * Constructor.
     */
    public DefaultDpwsModule() {
        appDelayExecutor = null;
        networkJobThreadPoolExecutor = null;
        wsDiscoveryExecutor = null;
        resolveExecutor = null;
    }

    @Override
    protected void configure() {
        configureMarshalling();
        configureUdp();
        configureHttpServer();
        configureSoapEngine();
        configureWsAddressing();
        configureWsDiscovery();
        configureWsEventing();
        configureWsMetadataExchange();
        configureWsTransfer();

        configureDevice();
        configureClient();

        configureService();

        install(new FactoryModuleBuilder()
                .implement(NotificationSourceUdpCallback.class, NotificationSourceUdpCallback.class)
                .build(DpwsHelperFactory.class));

        bind(DpwsFramework.class).to(DpwsFrameworkImpl.class).in(Singleton.class);

        bind(TransportBindingFactory.class)
                .to(ApacheTransportBindingFactoryImpl.class).asEagerSingleton();

        bind(HttpClientFactory.class)
                .to(ApacheTransportBindingFactoryImpl.class).asEagerSingleton();


        install(new FactoryModuleBuilder()
                .implement(UdpBindingService.class, UdpBindingServiceImpl.class)
                .build(UdpBindingServiceFactory.class));

        install(new FactoryModuleBuilder()
                .build(JettyHttpServerHandlerFactory.class));

        install(new FactoryModuleBuilder()
                .build(ClientTransportBindingFactory.class));

        bind(CommunicationLogSink.class).to(CommunicationLogSinkImpl.class).asEagerSingleton();

        install(new FactoryModuleBuilder()
                .implement(CommunicationLog.class, CommunicationLogDummyImpl.class)
                .build(CommunicationLogFactory.class));
    }

    private void configureService() {
        install(new FactoryModuleBuilder()
                .implement(HostingService.class, HostingServiceInterceptor.class)
                .implement(HostingServiceProxy.class, HostingServiceProxyImpl.class)
                .build(HostingServiceFactory.class));

        install(new FactoryModuleBuilder()
                .implement(HostedService.class, HostedServiceImpl.class)
                .implement(HostedServiceProxy.class, HostedServiceProxyImpl.class)
                .build(HostedServiceFactory.class));

        install(new FactoryModuleBuilder()
                .implement(HostedServiceInterceptor.class, HostedServiceInterceptor.class)
                .build(HostedServiceInterceptorFactory.class));

        install(new FactoryModuleBuilder()
                .implement(HostedServiceTransportBinding.class, HostedServiceTransportBinding.class)
                .build(HostedServiceTransportBindingFactory.class));
    }

    private void configureClient() {
        bind(Client.class)
                .to(ClientImpl.class);

        install(new FactoryModuleBuilder()
                .implement(DiscoveryClientUdpProcessor.class, DiscoveryClientUdpProcessor.class)
                .implement(DiscoveredDeviceResolver.class, DiscoveredDeviceResolver.class)
                .implement(HelloByeAndProbeMatchesObserverImpl.class, HelloByeAndProbeMatchesObserverImpl.class)
                .build(ClientHelperFactory.class));
    }

    private void configureDevice() {
        install(new FactoryModuleBuilder()
                .implement(DiscoveryDeviceUdpMessageProcessor.class, DiscoveryDeviceUdpMessageProcessor.class)
                .build(DeviceHelperFactory.class));

        install(new FactoryModuleBuilder()
                .implement(Device.class, DeviceImpl.class)
                .build(DeviceFactory.class));
    }

    private void configureWsMetadataExchange() {
        bind(GetMetadataClient.class).to(GetMetadataClientImpl.class);
    }

    private void configureWsTransfer() {
        bind(TransferGetClient.class).to(TransferGetClientImpl.class);
    }

    private void configureMarshalling() {
        bind(JaxbMarshalling.class).asEagerSingleton();
        bind(SoapMarshalling.class).to(JaxbSoapMarshalling.class).asEagerSingleton();
        bind(WsdlMarshalling.class).to(JaxbWsdlMarshalling.class).asEagerSingleton();
    }

    private void configureUdp() {
        install(new FactoryModuleBuilder()
                .implement(UdpBindingService.class, UdpBindingServiceImpl.class)
                .build(UdpBindingServiceFactory.class));
    }

    private void configureWsDiscovery() {

        bind(UdpMessageQueueService.class)
                .annotatedWith(DiscoveryUdpQueue.class)
                .to(UdpMessageQueueServiceImpl.class)
                .asEagerSingleton();

        install(new FactoryModuleBuilder()
                .implement(WsDiscoveryTargetService.class, WsDiscoveryTargetServiceInterceptor.class)
                .build(WsDiscoveryTargetServiceFactory.class));
        install(new FactoryModuleBuilder()
                .implement(WsDiscoveryClient.class, WsDiscoveryClientInterceptor.class)
                .build(WsDiscoveryClientFactory.class));
    }

    private void configureSoapEngine() {
        bind(RequestResponseServer.class)
                .to(RequestResponseServerImpl.class);

        install(new FactoryModuleBuilder()
                .implement(NotificationSink.class, NotificationSinkImpl.class)
                .build(NotificationSinkFactory.class));
        install(new FactoryModuleBuilder()
                .implement(SoapMessage.class, SoapMessage.class)
                .build(SoapMessageFactory.class));
        install(new FactoryModuleBuilder()
                .implement(RequestResponseClient.class, RequestResponseClientImpl.class)
                .build(RequestResponseClientFactory.class));
        install(new FactoryModuleBuilder()
                .implement(NotificationSource.class, NotificationSourceImpl.class)
                .build(NotificationSourceFactory.class));
    }

    private void configureWsAddressing() {
        bind(WsAddressingClientInterceptor.class).asEagerSingleton();
        bind(WsAddressingServerInterceptor.class)
                .annotatedWith(DeviceSpecific.class)
                .to(WsAddressingServerInterceptor.class)
                .asEagerSingleton();
        bind(WsAddressingServerInterceptor.class)
                .annotatedWith(ClientSpecific.class)
                .to(WsAddressingServerInterceptor.class)
                .asEagerSingleton();
    }

    private void configureWsEventing() {
        bind(LocalAddressResolver.class)
                .to(LocalAddressResolverImpl.class);

        install(new FactoryModuleBuilder()
                .implement(SubscriptionRegistry.class, SubscriptionRegistry.class)
                .build(SubscriptionRegistryFactory.class));

        install(new FactoryModuleBuilder()
                .implement(EventSourceInterceptorDispatcher.class, EventSourceInterceptorDispatcher.class)
                .build(EventSourceInterceptorDispatcherFactory.class));

        install(new FactoryModuleBuilder()
                .implement(GenericEventSource.class, GenericEventSource.class)
                .build(GenericEventSourceInterceptorFactory.class));

        install(new FactoryModuleBuilder()
                .implement(EventSink.class, EventSinkImpl.class)
                .build(WsEventingEventSinkFactory.class));

        install(new FactoryModuleBuilder()
                .implement(SourceSubscriptionManager.class, SourceSubscriptionManagerImpl.class)
                .implement(SinkSubscriptionManager.class, SinkSubscriptionManagerImpl.class)
                .build(SubscriptionManagerFactory.class));
    }

    private void configureHttpServer() {
        bind(HttpServerRegistry.class).to(JettyHttpServerRegistry.class).asEagerSingleton();
    }


    @Provides
    @AppDelayExecutor
    ExecutorWrapperService<ScheduledExecutorService> getAppDelayExecutor(@Named(CommonConfig.INSTANCE_IDENTIFIER)
                                                                         String frameworkIdentifier) {
        if (appDelayExecutor == null) {
            Callable<ScheduledExecutorService> executor = () -> Executors.newScheduledThreadPool(
                    10,
                    new ThreadFactoryBuilder()
                            .setNameFormat("AppDelayExecutor-thread-%d")
                            .setDaemon(true)
                            .build()
            );
            appDelayExecutor = new ExecutorWrapperService<>(executor, "AppDelayExecutor", frameworkIdentifier);
        }

        return appDelayExecutor;
    }

    @Provides
    @NetworkJobThreadPool
    ExecutorWrapperService<ListeningExecutorService> getNetworkJobThreadPool(@Named(CommonConfig.INSTANCE_IDENTIFIER)
                                                                             String frameworkIdentifier) {
        if (networkJobThreadPoolExecutor == null) {
            Callable<ListeningExecutorService> executor = () -> MoreExecutors.listeningDecorator(Executors
                    .newFixedThreadPool(
                            10,
                            new ThreadFactoryBuilder()
                                    .setNameFormat("NetworkJobThreadPool-thread-%d")
                                    .setDaemon(true)
                                    .build()
                    ));
            networkJobThreadPoolExecutor = new ExecutorWrapperService<>(executor, "NetworkJobThreadPool",
                    frameworkIdentifier);
        }

        return networkJobThreadPoolExecutor;
    }

    @Provides
    @WsDiscovery
    ExecutorWrapperService<ListeningExecutorService> getWsDiscoveryExecutor(@Named(CommonConfig.INSTANCE_IDENTIFIER)
                                                                            String frameworkIdentifier) {
        if (wsDiscoveryExecutor == null) {
            Callable<ListeningExecutorService> executor = () -> MoreExecutors.listeningDecorator(Executors
                    .newFixedThreadPool(
                            10,
                            new ThreadFactoryBuilder()
                                    .setNameFormat("WsDiscovery-thread-%d")
                                    .setDaemon(true)
                                    .build()
                    ));

            wsDiscoveryExecutor = new ExecutorWrapperService<>(executor, "WsDiscovery", frameworkIdentifier);
        }

        return wsDiscoveryExecutor;
    }

    @Provides
    @ResolverThreadPool
    ExecutorWrapperService<ListeningExecutorService> getResolverThreadPool(@Named(CommonConfig.INSTANCE_IDENTIFIER)
                                                                           String frameworkIdentifier) {
        if (resolveExecutor == null) {
            Callable<ListeningExecutorService> executor = () -> MoreExecutors.listeningDecorator(Executors
                    .newFixedThreadPool(
                            10,
                            new ThreadFactoryBuilder()
                                    .setNameFormat("ResolverThreadPool-thread-%d")
                                    .setDaemon(true)
                                    .build()
                    ));
            resolveExecutor = new ExecutorWrapperService<>(executor, "ResolverThreadPool",
                    frameworkIdentifier);
        }

        return resolveExecutor;
    }
}
