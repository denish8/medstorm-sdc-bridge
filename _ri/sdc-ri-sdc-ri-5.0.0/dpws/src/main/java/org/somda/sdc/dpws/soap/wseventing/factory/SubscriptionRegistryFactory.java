package org.somda.sdc.dpws.soap.wseventing.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.soap.wseventing.EventSourceDialectHandler;
import org.somda.sdc.dpws.soap.wseventing.helper.SubscriptionRegistry;

/**
 * Factory to create subscription registries.
 */
public interface SubscriptionRegistryFactory {

    /**
     * Creates an instance.
     *
     * @param customStaleCallback a dialect handler to deliver updates on expired subscriptions.
     * @return a new instance.
     */
    SubscriptionRegistry create(@Assisted EventSourceDialectHandler customStaleCallback);
}
