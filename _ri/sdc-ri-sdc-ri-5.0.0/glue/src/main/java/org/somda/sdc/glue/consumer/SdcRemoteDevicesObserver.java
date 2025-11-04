package org.somda.sdc.glue.consumer;

/**
 * Listens to connecting and disconnecting devices of a {@linkplain SdcRemoteDevicesConnector}.
 * <p>
 * In order to receive connection status messages, subscribe ({@link com.google.common.eventbus.Subscribe}) to an
 * {@link SdcRemoteDevicesConnector} and use a {@link org.somda.sdc.glue.consumer.event.RemoteDeviceConnectedMessage} or
 * {@link org.somda.sdc.glue.consumer.event.RemoteDeviceDisconnectedMessage} as first method parameter.
 */
public interface SdcRemoteDevicesObserver {
}
