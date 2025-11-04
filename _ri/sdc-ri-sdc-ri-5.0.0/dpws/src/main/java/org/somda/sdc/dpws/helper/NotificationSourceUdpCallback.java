package org.somda.sdc.dpws.helper;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.soap.MarshallingService;
import org.somda.sdc.dpws.soap.NotificationSource;
import org.somda.sdc.dpws.soap.SoapDebug;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.interception.NotificationCallback;
import org.somda.sdc.dpws.udp.UdpMessage;
import org.somda.sdc.dpws.udp.UdpMessageQueueService;

import java.io.ByteArrayOutputStream;

/**
 * Marshall {@link SoapMessage} and distribute them via {@link UdpMessageQueueService}.
 * <p>
 * {@linkplain NotificationSourceUdpCallback} implements {@link NotificationCallback} and is intended to be used by
 * a {@link NotificationSource} instance to be connected with a {@link UdpMessageQueueService}.
 */
public class NotificationSourceUdpCallback implements NotificationCallback {
    private static final Logger LOG = LogManager.getLogger(NotificationSourceUdpCallback.class);

    private final UdpMessageQueueService udpMessageQueue;
    private final MarshallingService marshallingService;
    private final Logger instanceLogger;

    @AssistedInject
    NotificationSourceUdpCallback(@Assisted UdpMessageQueueService udpMessageQueue,
                                  MarshallingService marshallingService,
                                  @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.udpMessageQueue = udpMessageQueue;
        this.marshallingService = marshallingService;
    }

    @Override
    public void onNotification(SoapMessage notification) throws MarshallingException {
        instanceLogger.debug("Outgoing SOAP/UDP message: {}", () -> SoapDebug.get(notification));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshallingService.marshal(notification, bos);
        byte[] data = bos.toByteArray();
        udpMessageQueue.sendMessage(new UdpMessage(data, data.length));
    }
}
