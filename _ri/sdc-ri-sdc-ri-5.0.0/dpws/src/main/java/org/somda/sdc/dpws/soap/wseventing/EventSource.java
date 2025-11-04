package org.somda.sdc.dpws.soap.wseventing;

import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.wseventing.model.WsEventingStatus;

import java.util.Map;

/**
 * Interface to provide WS-Eventing event source functions.
 */
public interface EventSource extends Interceptor {
    /**
     * Sends a notification to all event sinks.
     *
     * @param action  the action URI used for dispatching to event sinks.
     * @param payload a JAXB element or JAXB generated class object to transport.
     */
    void sendNotification(String action, Object payload);

    /**
     * Sends a subscription end to all event sinks.
     *
     * @param status the subscription end reason.
     */
    void subscriptionEndToAll(WsEventingStatus status);

    /**
     * Returns all active subscription ids with their {@linkplain SubscriptionManager}.
     *
     * @return Map of subscription ids and {@linkplain SubscriptionManager}s.
     */
    Map<String, SubscriptionManager> getActiveSubscriptions();

}
