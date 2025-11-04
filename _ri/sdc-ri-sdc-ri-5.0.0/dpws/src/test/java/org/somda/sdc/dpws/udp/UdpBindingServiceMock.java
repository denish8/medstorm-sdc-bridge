package org.somda.sdc.dpws.udp;

import com.google.common.util.concurrent.AbstractIdleService;

import java.io.IOException;

public class UdpBindingServiceMock extends AbstractIdleService implements UdpBindingService{
    private UdpMessageReceiverCallback receiver;

    @Override
    protected void startUp() {
    }

    @Override
    protected void shutDown() {
    }

    @Override
    public void setMessageReceiver(UdpMessageReceiverCallback receiver) {
        this.receiver = receiver;
    }

    @Override
    public void sendMessage(UdpMessage message) {
        if (receiver != null) {
            receiver.receive(message);
        } else {
            throw new RuntimeException("No UDP message receiver set");
        }
    }
}
