package org.somda.sdc.common.event;

import com.google.inject.Inject;

import java.util.HashSet;
import java.util.Set;

/**
 * Default implementation of {@linkplain EventBus}.
 */
public class EventBusImpl implements EventBus {
    private final com.google.common.eventbus.EventBus guiceEventBus;

    private final Set<Object> subscribers;

    @Inject
    EventBusImpl() {
        this.guiceEventBus = new com.google.common.eventbus.EventBus();
        this.subscribers = new HashSet<>();
    }

    @Override
    public String identifier() {
        return guiceEventBus.identifier();
    }

    @Override
    public synchronized void register(Object object) {
        guiceEventBus.register(object);
        subscribers.add(object);
    }

    @Override
    public synchronized void unregister(Object object) {
        guiceEventBus.unregister(object);
        subscribers.remove(object);
    }

    @Override
    public synchronized void unregisterAll() {
        subscribers.forEach(guiceEventBus::unregister);
        subscribers.clear();
    }

    @Override
    public void post(Object event) {
        guiceEventBus.post(event);
    }
}
