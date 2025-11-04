package org.somda.sdc.dpws.client.event;

import org.somda.sdc.common.event.EventMessage;

/**
 * Indicates a probe end of a previously started discovery process.
 */
public class DeviceProbeTimeoutMessage implements EventMessage {
    private final Integer foundDevicesCount;
    private final String discoveryId;

    /**
     * Constructor.
     *
     * @param foundDevicesCount the amount of devices that were found during a probe.
     * @param discoveryId the discovery id this message belongs to.
     */
    public DeviceProbeTimeoutMessage(Integer foundDevicesCount, String discoveryId) {
        this.foundDevicesCount = foundDevicesCount;
        this.discoveryId = discoveryId;
    }

    /**
     * Gets the discovery identifier of the discovery process that has ended.
     *
     * @return the discovery id of the probe request this message belongs to.
     */
    public String getDiscoveryId() {
        return discoveryId;
    }

    /**
     * Gets the amount of devices that were found for the discovery phase that matches {@link #getDiscoveryId()}.
     *
     * @return the number of found devices.
     */
    public Integer getFoundDevicesCount() {
        return foundDevicesCount;
    }
}
