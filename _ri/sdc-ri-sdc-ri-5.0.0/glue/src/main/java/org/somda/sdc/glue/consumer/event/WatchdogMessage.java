package org.somda.sdc.glue.consumer.event;

import org.somda.sdc.common.event.AbstractEventMessage;

/**
 * Message that is delivered to a {@linkplain org.somda.sdc.glue.consumer.WatchdogObserver} if something went wrong.
 */
public class WatchdogMessage extends AbstractEventMessage<String> {
    private final Exception reason;

    /**
     * Constructs a new instance.
     *
     * @param payload the endpoint reference address this watchdog message belongs to
     * @param reason the reason the watchdog sends this message.
     */
    public WatchdogMessage(String payload, Exception reason) {
        super(payload);
        this.reason = reason;
    }

    public Exception getReason() {
        return reason;
    }
}
