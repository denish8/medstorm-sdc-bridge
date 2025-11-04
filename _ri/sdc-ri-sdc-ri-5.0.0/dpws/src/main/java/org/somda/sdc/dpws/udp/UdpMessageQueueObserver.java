package org.somda.sdc.dpws.udp;

/**
 * Observer that is fed by {@link UdpMessageQueueService}.
 *
 * Use {@link com.google.common.eventbus.Subscribe} to annotate a callback method to receive {@link UdpMessage}
 * instances from {@link UdpMessageQueueService}. The callback method signature returns void and accepts a
 * {@link UdpMessage} instance as the only parameter.
 */
public interface UdpMessageQueueObserver {
}
