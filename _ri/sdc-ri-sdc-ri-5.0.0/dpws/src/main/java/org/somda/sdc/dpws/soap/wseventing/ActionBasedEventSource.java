package org.somda.sdc.dpws.soap.wseventing;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.AutoLock;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.wseventing.helper.EventSourceUtil;
import org.somda.sdc.dpws.soap.wseventing.model.Notification;
import org.somda.sdc.dpws.soap.wseventing.model.WsEventingStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Interceptor that handles an action-based event source's incoming subscription requests and facilitates sending
 * notifications.
 */
public class ActionBasedEventSource
        extends AbstractIdleService
        implements EventSource, EventSourceDialectHandler {
    private static final Logger LOG = LogManager.getLogger(ActionBasedEventSource.class);
    private Subscriptions subscriptions;
    private final Multimap<String, String> subscribedActionsToSubManIds;
    private final Lock subscribedActionsLock;
    private final EventSourceUtil eventSourceUtil;
    private final Logger instanceLogger;

    @Inject
    ActionBasedEventSource(
            EventSourceUtil eventSourceUtil,
            @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.eventSourceUtil = eventSourceUtil;
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.subscriptions = new Subscriptions() {
        };
        this.subscribedActionsToSubManIds = LinkedListMultimap.create();
        this.subscribedActionsLock = new ReentrantLock();
    }

    @Override
    public void sendNotification(String action, Object payload) {
        // Find subscription ids that are affected by the action
        Set<String> affectedSubscriptionIds;
        try (var ignored = AutoLock.lock(subscribedActionsLock)) {
            affectedSubscriptionIds = new HashSet<>(subscribedActionsToSubManIds.get(action));
            if (affectedSubscriptionIds.isEmpty()) {
                instanceLogger.debug("SendNotification: no recipient found for action {}", action);
                return;
            }
        }

        // For each affected subscription manager create a SOAP message and add it as a Notification object to the
        // subscription manager's notification queue
        for (String subId : affectedSubscriptionIds) {
            subscriptions.get(subId).ifPresent(subscriptionManager -> {
                SoapMessage notifyTo = eventSourceUtil.createForNotifyTo(action, payload, subscriptionManager);
                subscriptionManager.offerNotification(new Notification(notifyTo));
            });
        }
    }

    @Override
    public void subscriptionEndToAll(WsEventingStatus status) {
        subscriptions.getAll().forEach((uri, subMan) -> {
            subMan.offerEndTo(status);
        });
    }

    @Override
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
        final var uris = parseUriList(subscriptionManager.getFilters());
        try (var ignored = AutoLock.lock(subscribedActionsLock)) {
            uris.forEach(uri -> subscribedActionsToSubManIds.put(uri, subscriptionManager.getSubscriptionId()));
        }
    }

    @Override
    public void unsubscribe(SourceSubscriptionManager subscriptionManager) {
        try (var ignored = AutoLock.lock(subscribedActionsLock)) {
            final var uris = new HashSet<>(subscribedActionsToSubManIds.keySet());
            uris.forEach(uri -> subscribedActionsToSubManIds.remove(uri, subscriptionManager.getSubscriptionId()));
        }
    }

    @Override
    public void setStale(SourceSubscriptionManager subscriptionManager) {
        unsubscribe(subscriptionManager);
    }

    @Override
    public String getDialect() {
        return DpwsConstants.WS_EVENTING_SUPPORTED_DIALECT;
    }

    @Override
    protected void startUp() {
    }

    @Override
    protected void shutDown() {
        subscriptionEndToAll(WsEventingStatus.STATUS_SOURCE_SHUTTING_DOWN);
    }

    private List<String> parseUriList(List<Object> filters) {
        final var result = new ArrayList<String>();
        if (filters.size() != 1) {
            return result;
        }

        if (!String.class.isAssignableFrom(filters.get(0).getClass())) {
            return result;
        }

        final var listOfAnyUri = (String) filters.get(0);
        result.addAll(Arrays.asList(listOfAnyUri.split("\\s+")));

        return result;
    }
}
