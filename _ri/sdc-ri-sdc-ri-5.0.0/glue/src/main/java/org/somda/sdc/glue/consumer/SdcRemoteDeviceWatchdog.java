package org.somda.sdc.glue.consumer;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.client.Client;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ProbeMatchesType;
import org.somda.sdc.dpws.soap.wseventing.SubscribeResult;
import org.somda.sdc.glue.consumer.event.WatchdogMessage;
import org.somda.sdc.glue.consumer.helper.HostingServiceLogger;
import org.somda.sdc.glue.guice.WatchdogScheduledExecutor;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Sends WS-Eventing Renew or DirectedProbe requests and informs in case of an error.
 * <p>
 * {@linkplain SdcRemoteDeviceWatchdog} is a Guava service that requests a remote device in a given periodicity once
 * started.
 * <ul>
 * <li>If at least one subscription exists,
 * {@linkplain SdcRemoteDeviceWatchdog} tries to renew this subscription in order
 * to check if the remote device is still reachable.
 * <li>If multiple subscriptions exist, all are renewed.
 * <li>If no subscription exists, {@linkplain SdcRemoteDeviceWatchdog} sends a directed probe.
 * </ul>
 * The automatic renew spares enabling auto-renew for subscriptions.
 * The requested expires value for Subscribe requests is {@code ConsumerConfig#WATCHDOG_PERIOD * 3}.
 * <p>
 * Watchdog events are distributed only if the service is running.
 *
 * @see ConsumerConfig#WATCHDOG_PERIOD
 */
public class SdcRemoteDeviceWatchdog extends AbstractIdleService {
    private static final Logger LOG = LogManager.getLogger(SdcRemoteDeviceWatchdog.class);

    private final HostingServiceProxy hostingServiceProxy;
    private final Map<String, SubscribeResult> subscriptions;
    private final ExecutorWrapperService<ScheduledExecutorService> watchdogExecutor;
    private final Duration watchdogPeriod;
    private final Duration requestedExpires;
    private final Duration waitForEventQueueShutdown;
    private final ExecutorService eventBusExecutor;
    private final EventBus eventBus;
    private final Client client;
    private final Logger instanceLogger;
    private Future<?> currentJob = null;

    @AssistedInject
    SdcRemoteDeviceWatchdog(@Assisted HostingServiceProxy hostingServiceProxy,
                            @Assisted Map<String, SubscribeResult> subscriptions,
                            @Assisted @Nullable WatchdogObserver initialWatchdogObserver,
                            @WatchdogScheduledExecutor ExecutorWrapperService<ScheduledExecutorService> watchdogExecutor,
                            @Named(ConsumerConfig.WATCHDOG_PERIOD) Duration watchdogPeriod,
                            @Named(DpwsConfig.MAX_WAIT_FOR_FUTURES) Duration waitForEventQueueShutdown,
                            DpwsFramework dpwsFramework,
                            Client client,
                            @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = HostingServiceLogger.getLogger(LOG, hostingServiceProxy, frameworkIdentifier);
        this.hostingServiceProxy = hostingServiceProxy;
        this.subscriptions = new HashMap<>(subscriptions);
        this.watchdogExecutor = watchdogExecutor;
        this.watchdogPeriod = watchdogPeriod;
        this.waitForEventQueueShutdown = waitForEventQueueShutdown;
        this.requestedExpires = watchdogPeriod.multipliedBy(3);
        this.eventBusExecutor = Executors.newSingleThreadExecutor();
        this.eventBus = new AsyncEventBus(eventBusExecutor);
        this.client = client;
        dpwsFramework.registerService(List.of(watchdogExecutor));

        if (initialWatchdogObserver != null) {
            registerObserver(initialWatchdogObserver);
        }
    }

    /**
     * Registers a watchdog observer.
     *
     * @param watchdogObserver the watchdog observer.
     */
    public void registerObserver(WatchdogObserver watchdogObserver) {
        eventBus.register(watchdogObserver);
    }

    /**
     * Unregisters a watchdog observer.
     *
     * @param watchdogObserver the watchdog observer.
     */
    public void unregisterObserver(WatchdogObserver watchdogObserver) {
        eventBus.unregister(watchdogObserver);
    }

    @Override
    protected void startUp() {
        currentJob = watchdogExecutor.get()
                .schedule(new WatchdogJob(), watchdogPeriod.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    protected void shutDown() {
        if (currentJob != null && !currentJob.isDone()) {
            currentJob.cancel(true);
        }

        shutDownEventBusExecutor();
    }

    private void shutDownEventBusExecutor() {
        eventBusExecutor.shutdown();
        try {
            if (!eventBusExecutor.awaitTermination(waitForEventQueueShutdown.toMillis(), TimeUnit.MILLISECONDS)) {
                instanceLogger.warn("Watchdog AsyncEventBus executor did not terminate: message processing is pending");
            }
        } catch (InterruptedException e) {
            instanceLogger.warn("Interrupted while stopping Watchdog AsyncEventBus executor!");
        }
    }

    private class WatchdogJob implements Runnable {
        @Override
        public void run() {
            Duration timeout = watchdogPeriod;
            boolean watchdogRequestSent = false;
            for (var entry : subscriptions.entrySet()) {
                final String serviceId = entry.getKey();
                final SubscribeResult subscribeResult = entry.getValue();
                final Instant start = Instant.now();
                final HostedServiceProxy hostedServiceProxy = hostingServiceProxy.getHostedServices().get(serviceId);
                if (hostedServiceProxy == null) {
                    instanceLogger.warn("Could not find expected hosted service with id {}", serviceId);
                    postWatchdogMessage(new Exception(String.format(
                            "Could not find expected hosted service with id %s", serviceId)));
                    return;
                }

                final ListenableFuture<Duration> renewFuture = hostedServiceProxy.getEventSinkAccess()
                        .renew(subscribeResult.getSubscriptionId(), requestedExpires);

                try {
                    final Duration grantedExpires = renewFuture.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                    if (grantedExpires.compareTo(watchdogPeriod) < 0) {
                        instanceLogger.warn("Too little time granted for subscription on service {} " +
                                        "(expected at least {}, got {})",
                                serviceId, watchdogPeriod, grantedExpires);
                        postWatchdogMessage(new Exception(String.format(
                                "Too little time granted for subscription on service %s (expected at least %s, got %s)",
                                serviceId, watchdogPeriod, grantedExpires)));
                        return;
                    }
                } catch (Exception e) {
                    if (renewFuture != null) {
                        renewFuture.cancel(true);
                    }
                    instanceLogger.warn("Trying to renew subscription running on service {} failed", serviceId, e);
                    postWatchdogMessage(new Exception(String.format(
                            "Trying to renew subscription running on service %s failed", serviceId), e));
                    return;
                }

                final Instant finish = Instant.now();
                timeout = timeout.minus(Duration.between(start, finish));
                if (timeout.toMillis() < 0) {
                    instanceLogger.warn("Watchdog timeout exceeded. " +
                            "Could not get watchdog triggers served in time.");
                    postWatchdogMessage(new Exception("Watchdog timeout exceeded. " +
                            "Could not get watchdog triggers served in time."));
                    return;
                }

                watchdogRequestSent = true;
            }

            if (!watchdogRequestSent) {
                final Instant start = Instant.now();
                final ListenableFuture<ProbeMatchesType> probeFuture =
                        client.directedProbe(hostingServiceProxy.getActiveXAddr());
                try {
                    probeFuture.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                    final Instant finish = Instant.now();
                    timeout = timeout.minus(Duration.between(start, finish));
                } catch (Exception e) {
                    instanceLogger.warn("Trying to request a directed probe failed");
                    postWatchdogMessage(new Exception("Trying to request a directed probe failed", e));
                    return;
                }
            }

            if (isRunning() && watchdogExecutor.isRunning()) {
                currentJob =
                        watchdogExecutor.get().schedule(new WatchdogJob(), timeout.toMillis(), TimeUnit.MILLISECONDS);
            } else {
                currentJob = null;
                instanceLogger.info(
                        "WatchdogJob has ended, SdcRemoteDeviceWatchdog ({}) or WatchdogExecutor ({}) have ended",
                        state(), watchdogExecutor.state()
                );
            }
        }

        private void postWatchdogMessage(Exception reason) {
            if (isRunning()) {
                eventBus.post(new WatchdogMessage(hostingServiceProxy.getEndpointReferenceAddress(), reason));
            }
        }
    }
}
