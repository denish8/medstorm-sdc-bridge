package org.somda.sdc.dpws.soap.wseventing.helper;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.dpws.soap.wseventing.EventSourceDialectHandler;
import org.somda.sdc.dpws.soap.wseventing.SourceSubscriptionManager;
import org.somda.sdc.dpws.soap.wseventing.Subscriptions;
import org.somda.sdc.dpws.soap.wseventing.event.SubscriptionAddedMessage;
import org.somda.sdc.dpws.soap.wseventing.event.SubscriptionRemovedMessage;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe provision of a set of a subscription managers plus tracking mechanism.
 */
public class SubscriptionRegistry implements Subscriptions {
    private static final Logger LOG = LogManager.getLogger(SubscriptionRegistry.class);
    private final EventSourceDialectHandler customStaleCallback;
    private final EventSourceUtil eventSourceUtil;
    private final EventBus eventBus;
    private final Map<String, SourceSubscriptionManager> subscriptionManagers;

    @AssistedInject
    SubscriptionRegistry(@Assisted EventSourceDialectHandler customStaleCallback,
                         EventSourceUtil eventSourceUtil,
                         EventBus eventBus) {
        this.customStaleCallback = customStaleCallback;
        this.eventSourceUtil = eventSourceUtil;
        this.eventBus = eventBus;
        this.subscriptionManagers = new ConcurrentHashMap<>();
    }

    @Inject
    SubscriptionRegistry(EventSourceUtil eventSourceUtil,
                         EventBus eventBus) {
        this.customStaleCallback = null;
        this.eventSourceUtil = eventSourceUtil;
        this.eventBus = eventBus;
        this.subscriptionManagers = new ConcurrentHashMap<>();
    }

    /**
     * Adds a subscription to the subscription registry.
     *
     * @param subscriptionManager the subscription manager to add to the registry.
     */
    public void addSubscription(SourceSubscriptionManager subscriptionManager) {
        subscriptionManagers.put(subscriptionManager.getSubscriptionId(), subscriptionManager);
        eventBus.post(new SubscriptionAddedMessage(subscriptionManager));
    }

    /**
     * Removes a subscription from the subscription registry.
     *
     * @param subscriptionId the identifier of the subscription to remove.
     * @return the removed {@link SourceSubscriptionManager} instance if found, otherwise {@link Optional#empty()}.
     */
    public Optional<SourceSubscriptionManager> removeSubscription(String subscriptionId) {
        SourceSubscriptionManager removedSub = subscriptionManagers.remove(subscriptionId);
        if (removedSub != null) {
            eventSourceUtil.unregisterHttpHandler(removedSub);
            eventBus.post(new SubscriptionRemovedMessage(removedSub));
        }
        return Optional.ofNullable(removedSub);
    }

    /**
     * Gets a subscription from the subscription registry.
     *
     * @param subscriptionId the identifier of the subscription to retrieve.
     * @return the {@link SourceSubscriptionManager} instance if found, otherwise {@link Optional#empty()}.
     */
    public Optional<SourceSubscriptionManager> getSubscription(String subscriptionId) {
        return Optional.ofNullable(removeStaleSubscriptions().get(subscriptionId));
    }

    /**
     * Returns a copied snapshot of all available subscription managers.
     *
     * @return all subscription managers as a copy.
     */
    public Map<String, SourceSubscriptionManager> getSubscriptions() {
        return removeStaleSubscriptions();
    }

    /**
     * Registers an {@link EventBus} observer to enable tracking of subscription insertion and deletion.
     *
     * @param observer an observer with {@link com.google.common.eventbus.Subscribe} annotated methods and
     *                 the first argument of type {@link SubscriptionAddedMessage} or
     *                 {@link SubscriptionRemovedMessage}.
     * @see EventBus#register(Object)
     */
    public void registerObserver(Object observer) {
        eventBus.register(observer);
    }

    /**
     * Removes an observer formerly registered via {@link #registerObserver(Object)}.
     *
     * @param observer the observer to unregister.
     * @see EventBus#unregister(Object)
     */
    public void unregisterObserver(Object observer) {
        eventBus.unregister(observer);
    }

    private Map<String, SourceSubscriptionManager> removeStaleSubscriptions() {
        return new HashMap<>(subscriptionManagers).entrySet().stream().filter(it -> {
            final var key = it.getKey();
            final var subMan = it.getValue();
            if (!subMan.isRunning() || isSubscriptionExpired(subMan)) {
                removeSubscription(key);
                if (customStaleCallback != null) {
                    customStaleCallback.setStale(subMan);
                }
                subMan.stopAsync().awaitTerminated();
                LOG.info("Removed expired subscription: {}", key);
                return false;
            } else {
                return true;
            }
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private boolean isSubscriptionExpired(SourceSubscriptionManager subMan) {

        final var expires = Duration.between(Instant.now(), subMan.getExpiresTimeout());
        return expires.isZero() || expires.isNegative();
    }

    @Override
    public Optional<SourceSubscriptionManager> get(String subscriptionId) {
        return getSubscription(subscriptionId);
    }

    @Override
    public Map<String, SourceSubscriptionManager> getAll() {
        return getSubscriptions();
    }
}
