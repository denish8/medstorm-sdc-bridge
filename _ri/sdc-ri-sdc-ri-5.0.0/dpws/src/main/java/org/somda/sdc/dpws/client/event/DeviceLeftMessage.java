package org.somda.sdc.dpws.client.event;

import org.somda.sdc.common.event.AbstractEventMessage;

/**
 * Provides device proxy information that is delivered through a Bye message.
 */
public class DeviceLeftMessage extends AbstractEventMessage<String> {
    public DeviceLeftMessage(String payload) {
        super(payload);
    }
}
