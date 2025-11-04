package org.somda.sdc.dpws.soap.interception;

import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;

/**
 * Callback for network bindings to initiate an output-only message exchange.
 *
 * @see <a href="https://www.w3.org/2002/ws/cg/2/07/meps.html">Web Services message exchange patterns</a>
 */
public interface NotificationCallback {
    /**
     * Callback that is triggered after an interceptor chain succeeded on the outgoing direction.
     * <p>
     * As notifications are handled as fire and forget, no faults and answers are accepted.
     * <p>
     * todo DGr may accept SOAP faults for better error analysis.
     *
     * @param notification the notification to push.
     * @throws TransportException   if any transport-related exception comes up during processing.
     *                              This will hinder the response from being received.
     * @throws MarshallingException if an error occurs during marshalling or unmarshalling of a SOAP message.
     */
    void onNotification(SoapMessage notification) throws MarshallingException, TransportException;
}
