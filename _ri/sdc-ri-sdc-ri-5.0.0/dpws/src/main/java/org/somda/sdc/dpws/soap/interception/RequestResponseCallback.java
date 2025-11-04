package org.somda.sdc.dpws.soap.interception;

import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.exception.TransportException;

/**
 * Callback for network bindings to initiate a input-output message exchange.
 *
 * @see <a href="https://www.w3.org/2002/ws/cg/2/07/meps.html">Web Services message exchange patterns</a>
 */
public interface RequestResponseCallback {
    /**
     * Callback that is triggered after an interceptor chain succeeded on the outgoing direction.
     *
     * @param request the request to convey to the message receiver.
     * @return a SOAP message with the received response from the network.
     * @throws SoapFaultException   in case the server answers with a SOAP fault.
     * @throws TransportException   if any transport-related exception comes up during processing.
     *                              This will hinder the response from being received.
     * @throws MarshallingException if an error occurs during marshalling or unmarshalling of a SOAP message.
     */
    SoapMessage onRequestResponse(SoapMessage request) throws SoapFaultException, MarshallingException,
            TransportException;
}
