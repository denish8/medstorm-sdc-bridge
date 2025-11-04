package org.somda.sdc.dpws.client;

import org.somda.sdc.dpws.client.event.DeviceEnteredMessage;
import org.somda.sdc.dpws.client.event.DeviceLeftMessage;
import org.somda.sdc.dpws.client.event.DeviceProbeTimeoutMessage;
import org.somda.sdc.dpws.client.event.ProbedDeviceFoundMessage;

/**
 * Indicates class as a discovery observer.
 *
 * Annotate method with {@link com.google.common.eventbus.Subscribe} to
 *
 * - {@link DeviceEnteredMessage}
 * - {@link DeviceLeftMessage}
 * - {@link ProbedDeviceFoundMessage}
 * - {@link DeviceProbeTimeoutMessage}
 */
public interface DiscoveryObserver {
}
