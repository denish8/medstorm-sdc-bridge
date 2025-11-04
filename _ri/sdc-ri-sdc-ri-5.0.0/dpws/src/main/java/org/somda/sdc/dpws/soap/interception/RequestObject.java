package org.somda.sdc.dpws.soap.interception;

import org.somda.sdc.dpws.soap.SoapMessage;

/**
 * Object passed to interceptors to provide a SOAP request message.
 */
public class RequestObject implements InterceptorCallbackType {
    private final SoapMessage request;

    public RequestObject(SoapMessage request) {
        this.request = request;
    }

    public SoapMessage getRequest() {
        return request;
    }
}
