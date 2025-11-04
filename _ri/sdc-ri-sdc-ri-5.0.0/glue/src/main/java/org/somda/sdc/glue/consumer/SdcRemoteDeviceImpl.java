package org.somda.sdc.glue.consumer;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.common.access.MdibAccessObservable;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.message.AbstractSet;
import org.somda.sdc.biceps.model.message.AbstractSetResponse;
import org.somda.sdc.biceps.model.message.GetLocalizedText;
import org.somda.sdc.biceps.model.message.GetLocalizedTextResponse;
import org.somda.sdc.biceps.model.message.GetSupportedLanguages;
import org.somda.sdc.biceps.model.message.GetSupportedLanguagesResponse;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.glue.consumer.helper.HostingServiceLogger;
import org.somda.sdc.glue.consumer.localization.LocalizationServiceAccess;
import org.somda.sdc.glue.consumer.localization.LocalizationServiceProxy;
import org.somda.sdc.glue.consumer.report.ReportProcessor;
import org.somda.sdc.glue.consumer.sco.ScoController;
import org.somda.sdc.glue.consumer.sco.ScoTransaction;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Default implementation of {@linkplain SdcRemoteDevice}.
 */
public class SdcRemoteDeviceImpl extends AbstractIdleService implements SdcRemoteDevice {
    private static final Logger LOG = LogManager.getLogger(SdcRemoteDeviceImpl.class);
    private final RemoteMdibAccess remoteMdibAccess;
    private final ReportProcessor reportProcessor;
    private final ScoController scoController;
    private final HostingServiceProxy hostingServiceProxy;
    private final LocalizationServiceProxy localizationServiceProxy;
    private final SdcRemoteDeviceWatchdog watchdog;
    private final Duration maxWait;
    private final Logger instanceLogger;

    @AssistedInject
    SdcRemoteDeviceImpl(@Assisted HostingServiceProxy hostingServiceProxy,
                        @Assisted RemoteMdibAccess remoteMdibAccess,
                        @Assisted ReportProcessor reportProcessor,
                        @Assisted @Nullable ScoController scoController,
                        @Assisted @Nullable LocalizationServiceProxy localizationServiceProxy,
                        @Assisted SdcRemoteDeviceWatchdog watchdog,
                        @Named(DpwsConfig.MAX_WAIT_FOR_FUTURES) Duration maxWait,
                        @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = HostingServiceLogger.getLogger(LOG, hostingServiceProxy, frameworkIdentifier);
        this.remoteMdibAccess = remoteMdibAccess;
        this.reportProcessor = reportProcessor;
        this.scoController = scoController;
        this.hostingServiceProxy = hostingServiceProxy;
        this.localizationServiceProxy = localizationServiceProxy;
        this.watchdog = watchdog;
        this.maxWait = maxWait;
    }

    @Override
    public HostingServiceProxy getHostingServiceProxy() {
        checkRunning();
        return hostingServiceProxy;
    }

    @Override
    public RemoteMdibAccess getMdibAccess() {
        checkRunning();
        return remoteMdibAccess;
    }

    @Override
    public MdibAccessObservable getMdibAccessObservable() {
        return remoteMdibAccess;
    }

    @Override
    public SetServiceAccess getSetServiceAccess() {
        checkRunning();
        if (scoController == null) {
            final String message = "Remote device does not provide a set service. {} refused.";
            return new SetServiceAccess() {
                @Override
                public <T extends AbstractSet, V extends AbstractSetResponse> ListenableFuture<ScoTransaction<V>>
                invoke(T setRequest, Class<V> responseClass) {
                    instanceLogger.warn(message, setRequest.getClass().getSimpleName());
                    return Futures.immediateCancelledFuture();
                }

                @Override
                public <T extends AbstractSet, V extends AbstractSetResponse> ListenableFuture<ScoTransaction<V>>
                invoke(T setRequest, @Nullable Consumer<Pair<OperationInvokedReport.ReportPart, MdibVersion>> reportListener,
                       Class<V> responseClass) {
                    instanceLogger.warn(message, setRequest.getClass().getSimpleName());
                    return Futures.immediateCancelledFuture();
                }
            };
        }

        return scoController;
    }

    @Override
    public void registerWatchdogObserver(WatchdogObserver watchdogObserver) {
        checkRunning();
        this.watchdog.registerObserver(watchdogObserver);
    }

    @Override
    public void unregisterWatchdogObserver(WatchdogObserver watchdogObserver) {
        checkRunning();
        this.watchdog.unregisterObserver(watchdogObserver);
    }

    @Override
    public LocalizationServiceAccess getLocalizationServiceAccess() {
        checkRunning();
        if (localizationServiceProxy == null) {
            final String message = "Remote device does not provide a localization service. {} refused.";
            return new LocalizationServiceAccess() {
                @Override
                public ListenableFuture<GetLocalizedTextResponse> getLocalizedText(
                        GetLocalizedText getLocalizedTextRequest) {
                    instanceLogger.warn(message, "GetLocalizedText");
                    return Futures.immediateCancelledFuture();
                }

                @Override
                public ListenableFuture<GetSupportedLanguagesResponse> getSupportedLanguages(
                        GetSupportedLanguages getSupportedLanguagesRequest) {
                    instanceLogger.warn(message, "GetSupportedLanguages");
                    return Futures.immediateCancelledFuture();
                }
            };
        }

        return localizationServiceProxy;
    }

    @Override
    protected void startUp() throws TimeoutException {
        if (this.watchdog != null) {
            watchdog.startAsync().awaitRunning(maxWait.getSeconds(), TimeUnit.SECONDS);
        }
    }

    @Override
    protected void shutDown() {
        if (this.watchdog != null) {
            try {
                watchdog.stopAsync().awaitTerminated(maxWait.getSeconds(), TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                instanceLogger.error("Could not stop the remote device watchdog", e);
            }
        }

        try {
            reportProcessor.stopAsync().awaitTerminated(maxWait.getSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            instanceLogger.error("Could not stop the report processor", e);
        }

        final var hostedServices = new ArrayList<>(hostingServiceProxy.getHostedServices().values());
        for (HostedServiceProxy hostedService : hostedServices) {
            hostedService.getEventSinkAccess().unsubscribeAll();
        }

        remoteMdibAccess.unregisterAllObservers();
    }

    private void checkRunning() {
        if (!isRunning()) {
            throw new RuntimeException(String.format("Tried to access a disconnected SDC remote device instance " +
                            "with EPR address %s",
                    hostingServiceProxy.getEndpointReferenceAddress()));
        }
    }
}
