package org.somda.sdc.common.event;

/**
 * Simple message container to ease use with {@linkplain com.google.common.eventbus.EventBus}.
 * <p>
 * Google Guava's event bus library dispatches events based on function parametrization.
 * Given a class with a {@link com.google.common.eventbus.Subscribe} annotation:
 * <pre>
 * public class Foo {
 *     &#64;Subscribe
 *     void doSomethingWith(Bar payload) {
 *         // ...
 *     }
 * }
 * </pre>
 * If an instance of Foo is registered for an event bus and {@link com.google.common.eventbus.EventBus#post(Object)}
 * with a Bar instance is invoked, Guava knows which function to call by looking at the parameters of all
 * Subsribe-annotated functions.
 * <p>
 * The abstract event message helps in strong-typing EventBus-accepted functions and encapsulates a default payload
 * that is requestable by using the {@link #getPayload()} method.
 *
 * @param <T> payload type.
 */
public abstract class AbstractEventMessage<T> implements EventMessage {
    private final T payload;

    protected AbstractEventMessage(T payload) {
        this.payload = payload;
    }

    public T getPayload() {
        return payload;
    }
}
