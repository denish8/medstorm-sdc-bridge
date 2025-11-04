package org.somda.sdc.dpws.soap;

import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.interception.InterceptorHandler;

/**
 * Interface for network bindings to process incoming SOAP request messages and their responses.
 */
public interface RequestResponseServer extends InterceptorHandler {
    /**
     * Starts processing of an incoming SOAP request message.
     *
     * @param request              incoming request message.
     * @param response             outgoing response message.
     * @param communicationContext information from transport and application layer (scheme, host, port, ...).
     * @throws SoapFaultException if a SOAP fault comes up during processing.
     */
    void receiveRequestResponse(SoapMessage request, SoapMessage response, CommunicationContext communicationContext)
            throws SoapFaultException;
}
