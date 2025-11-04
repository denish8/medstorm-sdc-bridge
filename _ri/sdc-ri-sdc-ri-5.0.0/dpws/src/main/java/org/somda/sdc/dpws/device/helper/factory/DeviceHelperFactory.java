package org.somda.sdc.dpws.device.helper.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.device.helper.DiscoveryDeviceUdpMessageProcessor;
import org.somda.sdc.dpws.soap.RequestResponseServer;
import org.somda.sdc.dpws.udp.UdpMessageQueueService;
import org.somda.sdc.dpws.soap.SoapMessage;

/**
 * Factory to create instances of device util classes.
 */
public interface DeviceHelperFactory {

    /**
     * Creates a {@linkplain DiscoveryDeviceUdpMessageProcessor} instance.
     *
     * @param rrServer           request-response server that processes incoming and outgoing
     *                           {@link SoapMessage} instances.
     * @param udpMsgQueueService UDP message queue where to receive incoming UDP messages from and send outgoing UDP
     *                           messages to.
     * @return a configured {@link DiscoveryDeviceUdpMessageProcessor} instance.
     */
    DiscoveryDeviceUdpMessageProcessor createDiscoveryDeviceUdpMessageProcessor(
            @Assisted RequestResponseServer rrServer,
            @Assisted UdpMessageQueueService udpMsgQueueService);
}
