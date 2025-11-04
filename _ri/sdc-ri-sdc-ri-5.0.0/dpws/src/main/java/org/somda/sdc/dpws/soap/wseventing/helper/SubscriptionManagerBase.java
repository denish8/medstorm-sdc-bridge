package org.somda.sdc.dpws.soap.wseventing.helper;

import org.somda.sdc.common.util.AutoLock;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wseventing.SubscriptionManager;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Helper class that collects data shared between source and sink subscription managers.
 */
public class SubscriptionManagerBase implements SubscriptionManager {

    private final EndpointReferenceType notifyTo;
    private final EndpointReferenceType endTo;
    private final List<Object> filters;
    private final String filterDialect;
    private Instant expiresTimeout;
    private final String subscriptionId;
    private Duration expires;
    private final EndpointReferenceType subscriptionManagerEpr;
    private final Lock expiresLock;

    public SubscriptionManagerBase(EndpointReferenceType notifyTo,
                                   @Nullable EndpointReferenceType endTo,
                                   String subscriptionId,
                                   Duration expires,
                                   EndpointReferenceType subscriptionManagerEpr,
                                   List<Object> filters,
                                   String filterDialect) {
        this.notifyTo = notifyTo;
        this.endTo = endTo;
        this.expiresTimeout = calculateTimeout(expires);
        this.subscriptionId = subscriptionId;
        this.expires = expires;
        this.subscriptionManagerEpr = subscriptionManagerEpr;
        this.expiresLock = new ReentrantLock();
        this.filters = filters;
        this.filterDialect = filterDialect;
    }

    @Override
    public String getSubscriptionId() {
        return subscriptionId;
    }

    @Override
    public Instant getExpiresTimeout() {
        try (var ignored = AutoLock.lock(expiresLock)) {
            return expiresTimeout;
        }
    }

    @Override
    public EndpointReferenceType getNotifyTo() {
        return notifyTo;
    }

    @Override
    public Optional<EndpointReferenceType> getEndTo() {
        return Optional.ofNullable(endTo);
    }

    @Override
    public Duration getExpires() {
        try (var ignored = AutoLock.lock(expiresLock)) {
            return expires;
        }
    }

    @Override
    public EndpointReferenceType getSubscriptionManagerEpr() {
        return subscriptionManagerEpr;
    }

    @Override
    public List<Object> getFilters() {
        return Collections.unmodifiableList(filters);
    }

    @Override
    public String getFilterDialect() {
        return filterDialect;
    }

    /**
     * Updates the expiration of the subscription by a duration.
     *
     * @param expires new duration
     */
    public void renew(Duration expires) {
        try (var ignored = AutoLock.lock(expiresLock)) {
            this.expires = expires;
            this.expiresTimeout = calculateTimeout(expires);
        }
    }

    private Instant calculateTimeout(Duration expires) {
        final var t = Instant.now();
        return t.plus(expires);
    }
}
