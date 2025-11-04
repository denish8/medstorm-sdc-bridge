package org.somda.sdc.dpws.client.event;

import org.somda.sdc.common.event.AbstractEventMessage;
import org.somda.sdc.dpws.client.DiscoveredDevice;

/**
 * Provides a {@linkplain DiscoveredDevice} object that is delivered through a ProbeMatches message for a certain Probe
 * request.
 */
public class ProbedDeviceFoundMessage extends AbstractEventMessage<DiscoveredDevice> {
    private final String discoveryId;

    public ProbedDeviceFoundMessage(DiscoveredDevice payload, String discoveryId) {
        super(payload);
        this.discoveryId = discoveryId;
    }

    /**
     * Gets the discovery identifier of the Probe request that has resolved the discovered device.
     *
     * @return the discovery identifier of the discovery process that the discovered device relates to.
     */
    public String getDiscoveryId() {
        return discoveryId;
    }
}
