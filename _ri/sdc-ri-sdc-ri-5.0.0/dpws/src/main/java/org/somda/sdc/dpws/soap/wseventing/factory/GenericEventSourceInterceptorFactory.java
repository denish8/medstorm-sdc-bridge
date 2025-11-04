package org.somda.sdc.dpws.soap.wseventing.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.soap.wseventing.GenericEventSource;
import org.somda.sdc.dpws.soap.wseventing.IndividualSubscriptionHandler;

import javax.annotation.Nullable;

/**
 * Factory to create {@linkplain GenericEventSource} instances.
 */
public interface GenericEventSourceInterceptorFactory {

    /**
     * Creates a new instance based on a filter dialect and an optional callback for individual subscription handling.
     *
     * @param filterDialect the filter dialect that is handled by this event source.
     * @param handler       a handler for individual subscription handling or null if no customized handling is needed.
     * @return a new instance.
     */
    GenericEventSource create(@Assisted String filterDialect,
                              @Assisted @Nullable IndividualSubscriptionHandler handler);

    /**
     * Creates a new instance based on a filter dialect.
     *
     * @param filterDialect the filter dialect that is handled by this event source.
     * @return a new instance.
     */
    GenericEventSource create(@Assisted String filterDialect);
}
