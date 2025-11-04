package org.somda.sdc.glue.consumer;

/**
 * Listens to a Watchdog and receives information if something goes wrong.
 * <p>
 * In order to receive error messages that relate to the watchdog, use {@link com.google.common.eventbus.Subscribe}
 * with a {@link org.somda.sdc.glue.consumer.event.WatchdogMessage} object and subscribe at an {@link SdcRemoteDevice}.
 * Watchdog Events are distributed asynchronously, however it is recommended to block the
 * delivering thread as little as possible.
 */
public interface WatchdogObserver {
}
