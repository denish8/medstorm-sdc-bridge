package org.somda.sdc.dpws.soap.wsdiscovery.event;

import org.somda.sdc.common.event.AbstractEventMessage;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ByeType;

/**
 * Message to represent a WS-Discovery Bye.
 */
public class ByeMessage extends AbstractEventMessage<ByeType> {
    public ByeMessage(ByeType payload) {
        super(payload);
    }
}
