package org.somda.sdc.dpws.udp;

import com.google.common.util.concurrent.Service;

/**
 * Holds two message queues to send and receive UDP messages.
 * <p>
 * The {@linkplain UdpMessageQueueService} instance will use the UDP binding set via
 * {@link #setUdpBinding(UdpBindingService)}.
 * <ul>
 * <li>Use {@link #sendMessage(UdpMessage)} to send a UDP message.
 * <li>In order to receive messages, add observers to the {@linkplain UdpMessageQueueService} by using
 * {@link #registerUdpMessageQueueObserver(UdpMessageQueueObserver)}.
 * </ul>
 */
public interface UdpMessageQueueService extends Service, UdpMessageReceiverCallback {
    /**
     * Injects the UDP binding service.
     * <p>
     * Without a UDP binding service the message queue cannot send and receive messages.
     * Make sure the UDP binding is injected <em>before</em> the service is started.
     *
     * @param udpBinding the UDP binding service to inject.
     */
    void setUdpBinding(UdpBindingService udpBinding);

    /**
     * Queues an outgoing UDP message.
     *
     * @param message the message to be send.
     * @return true if the message could be queued, otherwise false (queue overflow).
     */
    boolean sendMessage(UdpMessage message);

    /**
     * Registers an observer to receive incoming UDP messages.
     *
     * @param observer the observer to register.
     */
    void registerUdpMessageQueueObserver(UdpMessageQueueObserver observer);

    /**
     * Unregisters an observer to stop receiving incoming UDP messages.
     *
     * @param observer the observer to unregister.
     */
    void unregisterUdpMessageQueueObserver(UdpMessageQueueObserver observer);
}
