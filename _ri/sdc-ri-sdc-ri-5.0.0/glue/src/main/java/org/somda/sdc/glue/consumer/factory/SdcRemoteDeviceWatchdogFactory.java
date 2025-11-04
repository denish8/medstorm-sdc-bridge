package org.somda.sdc.glue.consumer.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.wseventing.SubscribeResult;
import org.somda.sdc.glue.consumer.SdcRemoteDeviceWatchdog;
import org.somda.sdc.glue.consumer.WatchdogObserver;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Factory to create {@linkplain SdcRemoteDeviceWatchdog} instances.
 */
public interface SdcRemoteDeviceWatchdogFactory {
    /**
     * Creates a {@linkplain SdcRemoteDeviceWatchdog} instance.
     *
     * @param hostingServiceProxy     the hosting service proxy this watchdog is watching.
     * @param subscriptions           subscriptions used for auto-renew.
     * @param initialWatchdogObserver optional initial watchdog observer.
     * @return a new {@link SdcRemoteDeviceWatchdog} instance.
     */
    SdcRemoteDeviceWatchdog createSdcRemoteDeviceWatchdog(@Assisted HostingServiceProxy hostingServiceProxy,
                                                          @Assisted Map<String, SubscribeResult> subscriptions,
                                                          @Assisted @Nullable WatchdogObserver initialWatchdogObserver);
}
