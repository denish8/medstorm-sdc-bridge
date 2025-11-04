package org.somda.sdc.dpws.device;

import com.google.common.util.concurrent.Service;
import org.somda.sdc.dpws.soap.wseventing.SubscriptionManager;

import java.util.Map;

/**
 * Core class to create a device that exposes itself to the network.
 * <p>
 * In order to get a device up and running, perform the following steps
 * <ol>
 * <li>Configure the device appropriately by using
 * {@link org.somda.sdc.dpws.device.factory.DeviceFactory#createDevice(DeviceSettings)},
 * {@link #getDiscoveryAccess()} and {@link #getHostingServiceAccess()}.
 * <li>Use {@link #startAsync()} to start the device and send a WS-Discovery Hello.
 * <li>To stop the device, invoke {@link #stopAsync()}. This will send a WS-Discovery Bye.
 * </ol>
 */
public interface Device extends Service {
    /**
     * Gets access to WS-Discovery in order to configure types and scopes.
     *
     * @return discovery access.
     */
    DiscoveryAccess getDiscoveryAccess();

    /**
     * Gets access to Hosting Service metadata and Hosted Services.
     *
     * @return hosting service access.
     */
    HostingServiceAccess getHostingServiceAccess();

    String getEprAddress();

    Map<String, SubscriptionManager> getActiveSubscriptions();
}
