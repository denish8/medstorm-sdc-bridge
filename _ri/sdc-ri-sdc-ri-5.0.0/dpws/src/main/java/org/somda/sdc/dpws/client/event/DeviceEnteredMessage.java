package org.somda.sdc.dpws.client.event;

import org.somda.sdc.common.event.AbstractEventMessage;
import org.somda.sdc.dpws.client.DiscoveredDevice;

/**
 * Provides device proxy information that is delivered through a Hello message.
 */
public class DeviceEnteredMessage extends AbstractEventMessage<DiscoveredDevice> {
    public DeviceEnteredMessage(DiscoveredDevice payload) {
        super(payload);
    }
}
