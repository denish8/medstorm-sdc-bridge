package org.somda.sdc.dpws.soap.wseventing;

/**
 * Callback handler for subscriptions that handle notifications individually per manager.
 */
public interface IndividualSubscriptionHandler {

    /**
     * Indicates the start of a sequence of notifications for a specific manager.
     *
     * @param subscriptionManager the subscription manager for which individual messages are to be sent.
     */
    void startStream(SourceSubscriptionManager subscriptionManager);

    /**
     * Indicates the end of a sequence of notifications for a specific manager.
     *
     * This function is only called if a subscription expires or is actively unsubscribed by the event sink.
     *
     * @param subscriptionManager the subscription manager.
     */
    void endStream(SourceSubscriptionManager subscriptionManager);
}
