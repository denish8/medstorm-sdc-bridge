package org.somda.sdc.glue.consumer.event;

import org.somda.sdc.common.event.AbstractEventMessage;

import java.net.URI;

public class RemoteDeviceDisconnectedMessage extends AbstractEventMessage<URI> {
    public RemoteDeviceDisconnectedMessage(URI payload) {
        super(payload);
    }
}
