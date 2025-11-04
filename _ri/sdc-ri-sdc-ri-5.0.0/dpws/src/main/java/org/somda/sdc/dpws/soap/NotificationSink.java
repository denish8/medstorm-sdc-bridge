package org.somda.sdc.dpws.soap;

import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.interception.InterceptorHandler;

/**
 * Interface for network bindings to invoke execution of incoming SOAP notification messages.
 */
public interface NotificationSink extends InterceptorHandler {
    /**
     * Starts processing of an incoming SOAP notification message.
     *
     * @param notification         incoming request message.
     * @param communicationContext transport and application layer information (addresses, security, etc).
     * @throws SoapFaultException  in case a soap fault should be communicated in response
     */
    void receiveNotification(SoapMessage notification, CommunicationContext communicationContext)
        throws SoapFaultException;
}
