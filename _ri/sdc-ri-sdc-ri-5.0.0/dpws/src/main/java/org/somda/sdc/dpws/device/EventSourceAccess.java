package org.somda.sdc.dpws.device;

import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.wseventing.model.WsEventingStatus;

/**
 * Interface to access event source functionality provided by a {@linkplain Device}.
 */
public interface EventSourceAccess {
    /**
     * Sends a notification to all subscribers.
     *
     * @param action  the action the notification complies with.
     * @param payload the message payload that is tried to be marshalled and delivered.
     * @throws TransportException   on any transport-related exception during processing.
     *                              This will hinder the request from being sent.
     * @throws MarshallingException on any exception that occurs during marshalling or unmarshalling of SOAP messages.
     */
    void sendNotification(String action, Object payload) throws MarshallingException, TransportException;

    /**
     * Sends a subscription end message to all subscribers and shut down connected subscriptions.
     *
     * @param status reason for ending the subscriptions.
     * @throws TransportException   on any transport-related exception during processing.
     *                              This will hinder the request from being sent.
     */
    void subscriptionEndToAll(WsEventingStatus status) throws TransportException;
}
