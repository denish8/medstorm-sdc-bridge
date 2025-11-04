package org.somda.sdc.dpws.soap.wseventing;

import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * General WS-Eventing Subscription Manager information.
 */
public interface SubscriptionManager {
    String getSubscriptionId();

    Instant getExpiresTimeout();

    EndpointReferenceType getNotifyTo();

    Optional<EndpointReferenceType> getEndTo();

    Duration getExpires();

    EndpointReferenceType getSubscriptionManagerEpr();

    List<Object> getFilters();

    String getFilterDialect();
}
