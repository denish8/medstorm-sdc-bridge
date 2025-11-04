package org.somda.sdc.dpws.soap.wseventing.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.soap.wseventing.EventSourceDialectHandler;
import org.somda.sdc.dpws.soap.wseventing.EventSourceInterceptorDispatcher;

import java.util.Collection;

/**
 * Factory for EventSourceInterceptorDispatcher instances.
 */
public interface EventSourceInterceptorDispatcherFactory {
    /**
     * Creates a new instance of {@linkplain EventSourceInterceptorDispatcher}.
     *
     * @param eventSources all event sources handled by the created dispatcher.
     * @return a new instance.
     */
    EventSourceInterceptorDispatcher create(@Assisted Collection<EventSourceDialectHandler> eventSources);
}
