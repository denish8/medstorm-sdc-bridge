package org.somda.sdc.dpws.device;

import com.google.common.util.concurrent.AbstractIdleService;
import org.somda.sdc.dpws.service.HostedService;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.wseventing.EventSourceDialectHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Web Service base class.
 * <p>
 * The Web Service is a server interceptor to process incoming requests of a certain Web Service.
 * Moreover, the Web Service base class is capable of providing an event source to send notifications if needed.
 * <p>
 * The event source is only set if a hosted service has been registered at the Web Service. The hosted service can be
 * registered by first getting the hosting service access followed by adding a hosted service:
 * <ol>
 * <li>{@link Device#getHostingServiceAccess()} to get hosting service access, and then
 * <li>{@link HostingServiceAccess#addHostedService(HostedService)} to add the service to a hosting service.
 * </ol>
 * Use this class as a server interceptor when calling {@link HostingServiceAccess#addHostedService(HostedService)}.
 */
public abstract class WebService extends AbstractIdleService implements Interceptor {
    private final List<EventSourceDialectHandler> eventSources = new ArrayList<>();

    /**
     * Default constructor that initializes a non-functioning event source stub.
     */
    protected WebService() {
    }

    @Override
    protected void startUp() throws Exception {
    }

    @Override
    protected void shutDown() throws Exception {
    }

    /**
     * Allows to register an event source from outside.
     * <p>
     * This method is not thread-safe and should only be invoked initially by the user prior to start up.
     *
     * @param eventSource the event source to inject or null to reset.
     * @throws IllegalStateException if the Web Service is running.
     */
    public void registerEventSource(EventSourceDialectHandler eventSource) throws IllegalStateException {
        if (isRunning()) {
            throw new IllegalStateException("Event source cannot be set while web service is running");
        }

        this.eventSources.add(eventSource);
    }

    /**
     * Gets the event sources of this Web Service.
     *
     * @return the event sources.
     */
    public Collection<EventSourceDialectHandler> getEventSources() {
        return Collections.unmodifiableCollection(this.eventSources);
    }
}
