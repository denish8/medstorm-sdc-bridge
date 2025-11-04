package org.somda.sdc.dpws.soap.wsdiscovery.event;

import org.somda.sdc.common.event.AbstractEventMessage;
import org.somda.sdc.dpws.soap.wsdiscovery.model.HelloType;

/**
 * Message to represent a WS-Discovery Hello.
 */
public class HelloMessage extends AbstractEventMessage<HelloType> {
    public HelloMessage(HelloType payload) {
        super(payload);
    }
}
