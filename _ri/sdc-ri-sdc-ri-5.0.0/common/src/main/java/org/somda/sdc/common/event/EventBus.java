package org.somda.sdc.common.event;

/**
 * An interface with extended functionality that substitutes {@linkplain com.google.common.eventbus.EventBus}.
 * <p>
 * Google Guice's {@link com.google.common.eventbus.EventBus} implementation lacks support for unregistering all
 * subscribers at once, which is added by providing the method {@link #unregisterAll()}.
 */
public interface EventBus {
    /**
     * Returns the name this event bus is identified by.
     *
     * @return the identifier of this event bus.
     * @see com.google.common.eventbus.EventBus#identifier()
     */
    String identifier();

    /**
     * Registers an object to the event bus.
     *
     * @param object the object to register.
     * @see com.google.common.eventbus.EventBus#register(Object)
     */
    void register(Object object);

    /**
     * Unregisters an object from the event bus.
     *
     * @param object the object to unregister.
     * @see com.google.common.eventbus.EventBus#unregister(Object)
     */
    void unregister(Object object);

    /**
     * Unregisters all observers currently registered at the event bus.
     */
    void unregisterAll();

    /**
     * Distributes an event to all registered observers.
     *
     * @param event the event to distribute.
     * @see com.google.common.eventbus.EventBus#post(Object)
     */
    void post(Object event);
}
