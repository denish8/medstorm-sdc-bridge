package org.somda.sdc.dpws.soap.wseventing;

import java.time.Duration;

/**
 * Subscription manager interface that is used by event sinks.
 */
public interface SinkSubscriptionManager extends SubscriptionManager {
    /**
     * Resets the expires duration.
     * <p>
     * This will also affect {@link #getExpiresTimeout()}.
     *
     * @param expires the duration to reset.
     */
    void renew(Duration expires);
}
