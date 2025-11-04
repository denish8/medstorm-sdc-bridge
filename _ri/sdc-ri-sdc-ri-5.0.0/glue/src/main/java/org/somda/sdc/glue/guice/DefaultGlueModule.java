package org.somda.sdc.glue.guice;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Named;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.glue.common.MdibMapper;
import org.somda.sdc.glue.common.ModificationsBuilder;
import org.somda.sdc.glue.common.factory.MdibMapperFactory;
import org.somda.sdc.glue.common.factory.ModificationsBuilderFactory;
import org.somda.sdc.glue.consumer.SdcRemoteDevice;
import org.somda.sdc.glue.consumer.SdcRemoteDeviceImpl;
import org.somda.sdc.glue.consumer.SdcRemoteDeviceWatchdog;
import org.somda.sdc.glue.consumer.SdcRemoteDevicesConnector;
import org.somda.sdc.glue.consumer.SdcRemoteDevicesConnectorImpl;
import org.somda.sdc.glue.consumer.factory.SdcRemoteDeviceFactory;
import org.somda.sdc.glue.consumer.factory.SdcRemoteDeviceWatchdogFactory;
import org.somda.sdc.glue.consumer.localization.LocalizationServiceProxy;
import org.somda.sdc.glue.consumer.localization.factory.LocalizationServiceProxyFactory;
import org.somda.sdc.glue.consumer.sco.factory.OperationInvocationDispatcherFactory;
import org.somda.sdc.glue.consumer.sco.helper.OperationInvocationDispatcher;
import org.somda.sdc.glue.provider.SdcDevice;
import org.somda.sdc.glue.provider.factory.SdcDeviceFactory;
import org.somda.sdc.glue.provider.localization.LocalizationService;
import org.somda.sdc.glue.provider.localization.LocalizationServiceImpl;
import org.somda.sdc.glue.provider.localization.factory.LocalizationServiceFactory;
import org.somda.sdc.glue.provider.sco.Context;
import org.somda.sdc.glue.provider.sco.ScoController;
import org.somda.sdc.glue.provider.sco.factory.ContextFactory;
import org.somda.sdc.glue.provider.sco.factory.ScoControllerFactory;
import org.somda.sdc.glue.provider.services.HighPriorityServices;
import org.somda.sdc.glue.provider.services.factory.ServicesFactory;
import org.somda.sdc.glue.provider.services.helper.ReportGenerator;
import org.somda.sdc.glue.provider.services.helper.factory.ReportGeneratorFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Default Glue module.
 */
public class DefaultGlueModule extends AbstractModule {

    private ExecutorWrapperService<ListeningExecutorService> consumerExecutor;
    private ExecutorWrapperService<ScheduledExecutorService> watchdogScheduledExecutor;

    public DefaultGlueModule() {
        consumerExecutor = null;
        watchdogScheduledExecutor = null;
    }

    @Override
    protected void configure() {
        configureCommon();
        configureConsumer();
        configureProvider();
    }

    private void configureCommon() {
        install(new FactoryModuleBuilder()
                .implement(MdibMapper.class, MdibMapper.class)
                .build(MdibMapperFactory.class));
        install(new FactoryModuleBuilder()
                .implement(ModificationsBuilder.class, ModificationsBuilder.class)
                .build(ModificationsBuilderFactory.class));
    }

    private void configureConsumer() {
        bind(SdcRemoteDevicesConnector.class).to(SdcRemoteDevicesConnectorImpl.class);

        install(new FactoryModuleBuilder()
                .implement(OperationInvocationDispatcher.class, OperationInvocationDispatcher.class)
                .build(OperationInvocationDispatcherFactory.class));

        install(new FactoryModuleBuilder()
                .implement(SdcRemoteDevice.class, SdcRemoteDeviceImpl.class)
                .build(SdcRemoteDeviceFactory.class));

        install(new FactoryModuleBuilder()
                .implement(org.somda.sdc.glue.consumer.sco.ScoController.class,
                        org.somda.sdc.glue.consumer.sco.ScoController.class)
                .build(org.somda.sdc.glue.consumer.sco.factory.ScoControllerFactory.class));

        install(new FactoryModuleBuilder()
                .implement(SdcRemoteDeviceWatchdog.class, SdcRemoteDeviceWatchdog.class)
                .build(SdcRemoteDeviceWatchdogFactory.class));

        install(new FactoryModuleBuilder()
                .implement(LocalizationServiceProxy.class, LocalizationServiceProxy.class)
                .build(LocalizationServiceProxyFactory.class));
    }

    private void configureProvider() {
        install(new FactoryModuleBuilder()
                .implement(ScoController.class, ScoController.class)
                .build(ScoControllerFactory.class));

        install(new FactoryModuleBuilder()
                .implement(HighPriorityServices.class, HighPriorityServices.class)
                .build(ServicesFactory.class));

        install(new FactoryModuleBuilder()
                .implement(SdcDevice.class, SdcDevice.class)
                .build(SdcDeviceFactory.class));

        install(new FactoryModuleBuilder()
                .implement(Context.class, Context.class)
                .build(ContextFactory.class));

        install(new FactoryModuleBuilder()
                .implement(ReportGenerator.class, ReportGenerator.class)
                .build(ReportGeneratorFactory.class));

        install(new FactoryModuleBuilder()
                .implement(LocalizationService.class, LocalizationServiceImpl.class)
                .build(LocalizationServiceFactory.class));

    }

    @Provides
    @Consumer
    ExecutorWrapperService<ListeningExecutorService> getConsumerExecutor(
            @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        if (consumerExecutor == null) {
            Callable<ListeningExecutorService> executor =
                    () -> MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(
                            10,
                            new ThreadFactoryBuilder()
                                    .setNameFormat("Consumer-thread-%d")
                                    .setDaemon(true)
                                    .build()
                    ));
            consumerExecutor = new ExecutorWrapperService<>(executor, "Consumer", frameworkIdentifier);
        }
        return consumerExecutor;
    }

    @Provides
    @WatchdogScheduledExecutor
    ExecutorWrapperService<ScheduledExecutorService> getWatchdogScheduledExecutor(
            @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        if (watchdogScheduledExecutor == null) {
            Callable<ScheduledExecutorService> executor = () -> Executors.newScheduledThreadPool(
                    10,
                    new ThreadFactoryBuilder()
                            .setNameFormat("WatchdogScheduledExecutor-thread-%d")
                            .setDaemon(true)
                            .build()
            );

            watchdogScheduledExecutor =
                    new ExecutorWrapperService<>(executor, "WatchdogScheduledExecutor", frameworkIdentifier);
        }

        return watchdogScheduledExecutor;
    }
}
