package org.somda.sdc.dpws.soap.wseventing;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Interface to access subscriptions.
 */
public interface Subscriptions {
    /**
     * Gets a specific subscription.
     *
     * @param subscriptionId the identifier of the subscription to retrieve.
     * @return the {@link SourceSubscriptionManager} instance if found, otherwise {@link Optional#empty()}.
     */
     default Optional<SourceSubscriptionManager> get(String subscriptionId) {
         return Optional.empty();
     };

    /**
     * Returns an immutable map of all active subscriptions.
     *
     * @return all subscription managers.
     */
    default Map<String, SourceSubscriptionManager> getAll() {
        return Collections.emptyMap();
    }
}
