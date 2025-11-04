package org.somda.sdc.dpws.udp;

/**
 * Callback to process UDP messages received by {@link UdpBindingService}.
 */
public interface UdpMessageReceiverCallback {
    /**
     * Receive one incoming UDP message.
     * @param udpMessage UDP data as received from the network. The message contains the payload as well as the sender's
     *                   address and port.
     */
    void receive(UdpMessage udpMessage);
}
