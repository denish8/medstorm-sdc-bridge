package org.somda.sdc.dpws.soap.wseventing;

import org.somda.sdc.dpws.soap.NotificationSink;

import java.time.Duration;
import java.util.List;

/**
 * Subscription information container retrieved from a Subscribe response in
 * {@link EventSink#subscribe(List, Duration, NotificationSink)}.
 */
public class SubscribeResult {
    private final String subscriptionId;
    private final Duration grantedExpires;

    public SubscribeResult(String subscriptionId, Duration grantedExpires) {
        this.subscriptionId = subscriptionId;
        this.grantedExpires = grantedExpires;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public Duration getGrantedExpires() {
        return grantedExpires;
    }
}
