package org.somda.sdc.dpws.soap.wseventing;

import org.somda.sdc.dpws.soap.interception.Interceptor;

/**
 * Callback interface for the management of subscriptions of a specific filter dialect.
 */
public interface EventSourceDialectHandler extends Interceptor {
    /**
     * Called once for a filter dialect handler when the {@linkplain EventSourceInterceptorDispatcher} service starts.
     * <p>
     * This function is guaranteed to be called before any other callback of this interface is called.
     * <p>
     * The subscriptions are managed by the {@link EventSourceInterceptorDispatcher}!
     *
     * @param subscriptions the subscription registry that handles all subscriptions for the filter dialect
     */
    void init(Subscriptions subscriptions);

    /**
     * Called on an incoming subscribe request.
     *
     * @param subscriptionManager the subscription manager for this subscription.
     */
    void subscribe(SourceSubscriptionManager subscriptionManager);

    /**
     * Called on an incoming unsubscribe request.
     *
     * @param subscriptionManager the subscription manager for this subscription.
     */
    void unsubscribe(SourceSubscriptionManager subscriptionManager);

    /**
     * Called when a subscription has expired.
     *
     * @param subscriptionManager the subscription manager for this subscription.
     */
    void setStale(SourceSubscriptionManager subscriptionManager);

    /**
     * This function shall return the handled filter dialect.
     * <p>
     * It is used by the dispatcher helping to assign subscription activity.
     *
     * @return the filter dialect handled by this instance.
     */
    String getDialect();
}
