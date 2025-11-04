package org.somda.sdc.dpws.soap;

import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.dpws.soap.interception.InterceptorHandler;

/**
 * Interface to send notifications to event sinks.
 */
public interface NotificationSource extends InterceptorHandler {
    /**
     * Sends a SOAP notification message.
     * <p>
     * This method returns as soon as the message left the application process.
     *
     * @param notification outgoing notification message.
     * @throws TransportException   if any transport-related exception comes up during processing. This will hinder the
     *                              response from being sent.
     * @throws MarshallingException if any exception occurs during marshalling or unmarshalling of SOAP messages.
     * @throws InterceptorException if one of the interceptors pops up with an error.
     */
    void sendNotification(SoapMessage notification) throws MarshallingException, TransportException,
            InterceptorException;
}
