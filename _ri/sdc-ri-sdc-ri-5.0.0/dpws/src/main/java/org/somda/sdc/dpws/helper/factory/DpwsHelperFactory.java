package org.somda.sdc.dpws.helper.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.helper.NotificationSourceUdpCallback;
import org.somda.sdc.dpws.udp.UdpMessageQueueService;

/**
 * Factory to create instances of {@link NotificationSourceUdpCallback}.
 */
public interface DpwsHelperFactory {
    /**
     * Creates a {@linkplain NotificationSourceUdpCallback} instance.
     *
     * @param msgQueue UDP message queue to send SOAP messages over UDP.
     * @return a configured {@link NotificationSourceUdpCallback} instance.
     */
    NotificationSourceUdpCallback createNotificationSourceUdpCallback(@Assisted UdpMessageQueueService msgQueue);
}
