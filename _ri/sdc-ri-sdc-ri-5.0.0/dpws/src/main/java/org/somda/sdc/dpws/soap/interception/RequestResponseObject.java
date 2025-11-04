package org.somda.sdc.dpws.soap.interception;

import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.SoapMessage;

/**
 * Object passed to interceptors to provide both a SOAP request and response message.
 */
public class RequestResponseObject implements InterceptorCallbackType {
    private final SoapMessage request;
    private final SoapMessage response;
    private final CommunicationContext communicationContext;

    public RequestResponseObject(SoapMessage request, SoapMessage response, CommunicationContext communicationContext) {
        this.request = request;
        this.response = response;
        this.communicationContext = communicationContext;
    }

    public SoapMessage getRequest() {
        return request;
    }

    public SoapMessage getResponse() {
        return response;
    }

    public CommunicationContext getCommunicationContext() {
        return communicationContext;
    }
}
