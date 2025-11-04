package org.somda.sdc.dpws.service;

import com.google.common.util.concurrent.ListenableFuture;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.wseventing.EventSink;
import org.somda.sdc.dpws.soap.wseventing.SubscribeResult;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.List;

/**
 * Offers access to event sink functionality in accordance with WS-Eventing.
 */
public interface EventSinkAccess {
    /**
     * Subscribes to a set of actions using an action-based filter.
     *
     * This is a convenience function to subscribe for action-based filters, which is required to be supported by DPWS.
     *
     * @param actions          a list of operation actions to subscribe for.
     * @param expires          the desired expiration time (the hosted service may decide to grant less than this).
     *                         If none is given, the hosting service will take decision.
     * @param notificationSink the sink where to deliver notifications.
     * @return a future object that either includes subscription information or throws
     * <ul>
     * <li>{@link SoapFaultException}
     * <li>{@link MarshallingException}
     * <li>{@link TransportException}
     * <li>{@link org.somda.sdc.dpws.soap.interception.InterceptorException}
     * </ul>
     */
    ListenableFuture<SubscribeResult> subscribe(List<String> actions,
                                                @Nullable Duration expires,
                                                Interceptor notificationSink);

    /**
     * Generically subscribes to an event source.
     *
     * Use {@link #subscribe(List, Duration, Interceptor)} if action-based filtering is used.
     *
     * @param filterDialect    the filter dialect URI.
     * @param filters          a list of filter objects towards the subscription.
     * @param expires          the desired expiration time (the hosted service may decide to grant less or
     *                         more than this).
     *                         If none is given, the hosting service will take decision.
     * @param notificationSink the sink where to deliver notifications.
     * @return a future object that either includes subscription information or throws
     * <ul>
     * <li>{@link SoapFaultException}
     * <li>{@link MarshallingException}
     * <li>{@link TransportException}
     * <li>{@link org.somda.sdc.dpws.soap.interception.InterceptorException}
     * </ul>
     */
    ListenableFuture<SubscribeResult> subscribe(String filterDialect,
                                                List<Object> filters,
                                                @Nullable Duration expires,
                                                Interceptor notificationSink);

    /**
     * Renews a subscription.
     *
     * @param subscriptionId the subscription id obtained in the {@link SubscribeResult} of
     *                       {@link #subscribe(List, Duration, Interceptor)}.
     * @param expires        the desired new expiration duration.
     * @return a future object that either includes a granted expires duration or throws
     * <ul>
     * <li>{@link SoapFaultException}
     * <li>{@link MarshallingException}
     * <li>{@link TransportException}
     * <li>{@link org.somda.sdc.dpws.soap.interception.InterceptorException}
     * </ul>
     */
    ListenableFuture<Duration> renew(String subscriptionId, Duration expires);

    /**
     * Gets the status of a subscription.
     *
     * @param subscriptionId the subscription id obtained in the {@link SubscribeResult} of
     *                       {@link #subscribe(List, Duration, Interceptor)}.
     * @return a future object that either includes the remaining subscription time or throws
     * <ul>
     * <li>{@link SoapFaultException}
     * <li>{@link MarshallingException}
     * <li>{@link TransportException}
     * <li>{@link org.somda.sdc.dpws.soap.interception.InterceptorException}
     * </ul>
     */
    ListenableFuture<Duration> getStatus(String subscriptionId);

    /**
     * Unsubscribes from a subscription.
     *
     * @param subscriptionId the subscription id obtained in the {@link SubscribeResult} of
     *                       {@link #subscribe(List, Duration, Interceptor)}.
     * @return a future object that either delivers an empty object in case of successful unsubscribe or throws
     * <ul>
     * <li>{@link SoapFaultException}
     * <li>{@link MarshallingException}
     * <li>{@link TransportException}
     * <li>{@link org.somda.sdc.dpws.soap.interception.InterceptorException}
     * </ul>
     */
    ListenableFuture unsubscribe(String subscriptionId);

    /**
     * Synchronously unsubscribes all subscriptions.
     *
     * @see EventSink#unsubscribeAll()
     */
    void unsubscribeAll();
}
