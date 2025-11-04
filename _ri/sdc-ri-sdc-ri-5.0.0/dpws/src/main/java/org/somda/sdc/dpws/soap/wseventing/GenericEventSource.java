package org.somda.sdc.dpws.soap.wseventing;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.wseventing.helper.EventSourceUtil;
import org.somda.sdc.dpws.soap.wseventing.helper.SubscriptionRegistry;
import org.somda.sdc.dpws.soap.wseventing.model.Notification;
import org.somda.sdc.dpws.soap.wseventing.model.WsEventingStatus;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBElement;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Interceptor that handles an event source's incoming subscription requests and facilitates sending notifications.
 */
public class GenericEventSource implements EventSource, EventSourceDialectHandler {
    private Subscriptions subscriptions;
    private final String filterDialect;
    private final EventSourceUtil eventSourceUtil;
    private final IndividualSubscriptionHandler handler;

    @AssistedInject
    GenericEventSource(
            @Assisted String filterDialect,
            @Assisted @Nullable IndividualSubscriptionHandler handler,
            SubscriptionRegistry subscriptions,
            EventSourceUtil eventSourceUtil) {
        this.filterDialect = filterDialect;
        this.handler = handler;
        this.subscriptions = subscriptions;
        this.eventSourceUtil = eventSourceUtil;
    }

    @AssistedInject
    GenericEventSource(
            @Assisted String filterDialect,
            SubscriptionRegistry subscriptions,
            EventSourceUtil eventSourceUtil) {
        this(filterDialect, null, subscriptions, eventSourceUtil);
    }

    @Override
    public void sendNotification(String action, Object payload) {
        subscriptions.getAll().forEach((uri, subscriptionManager) -> {
            final var notifyTo = eventSourceUtil.createForNotifyTo(action, payload, subscriptionManager);
            subscriptionManager.offerNotification(new Notification(notifyTo));
        });
    }

    @Override
    public void subscriptionEndToAll(WsEventingStatus status) {
        subscriptions.getAll().forEach((uri, subscriptionManager) -> {
            subscriptionManager.offerEndTo(status);
        });
    }

    /**
     * Additional method to send out a notification to a single subscription.
     *
     * @param subscriptionId the subscription id for which a notification will be sent.
     * @param action         the WS-Addressing action header URI.
     * @param payload        the notification payload as {@link JAXBElement} or an object that can be marshalled.
     */
    public void sendNotificationFor(String subscriptionId, String action, Object payload) {
        subscriptions.get(subscriptionId).ifPresent(subscriptionManager -> {
            SoapMessage notifyTo = eventSourceUtil.createForNotifyTo(action, payload, subscriptionManager);
            subscriptionManager.offerNotification(new Notification(notifyTo));
        });
    }

    /**
     * Additional method to send out a subscription end to a single subscription.
     *
     * @param subscriptionId the subscription id for which the end will be announced.
     */
    public void endSubscriptionFor(String subscriptionId) {
        subscriptions.get(subscriptionId).ifPresent(subscriptionManager -> {
            subscriptionManager.offerEndTo(WsEventingStatus.STATUS_SOURCE_CANCELLING);
        });
    }

    public Map<String, SubscriptionManager> getActiveSubscriptions() {
        return subscriptions.getAll().entrySet()
                .stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public void init(Subscriptions subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Override
    public void subscribe(SourceSubscriptionManager subscriptionManager) {
        if (handler != null) {
            handler.startStream(subscriptionManager);
        }
    }

    @Override
    public void unsubscribe(SourceSubscriptionManager subscriptionManager) {
        if (handler != null) {
            handler.endStream(subscriptionManager);
        }
    }

    @Override
    public void setStale(SourceSubscriptionManager subscriptionManager) {
        unsubscribe(subscriptionManager);
    }

    @Override
    public String getDialect() {
        return filterDialect;
    }
}
