package org.somda.sdc.dpws.soap.wstransfer;

import com.google.common.util.concurrent.ListenableFuture;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.wsaddressing.model.ReferenceParametersType;

/**
 * Interface to send WS-Transfer Get requests.
 */
public interface TransferGetClient {
    /**
     * Sends a WS-Transfer Get request.
     *
     * @param requestResponseClient the request response client where to send the request to.
     * @param wsaTo                 WS-Addressing wsa:To field content.
     * @return a future object that in case of a success includes the SOAP response message or throws
     * <ul>
     * <li>{@link org.somda.sdc.dpws.soap.exception.SoapFaultException}
     * <li>{@link org.somda.sdc.dpws.soap.exception.MarshallingException}
     * <li>{@link org.somda.sdc.dpws.soap.exception.TransportException}
     * <li>{@link org.somda.sdc.dpws.soap.interception.InterceptorException}
     * </ul>
     */
    ListenableFuture<SoapMessage> sendTransferGet(RequestResponseClient requestResponseClient, String wsaTo);

    /**
     * Sends a WS-Transfer Get request with reference parameters.
     *
     * @param requestResponseClient the request response client where to send the request to
     * @param wsaTo                 WS-Addressing wsa:To field content
     * @param referenceParameters   reference parameters to include in the message
     * @return a future object that in case of a success includes the SOAP response message or throws
     * <ul>
     * <li>{@link org.somda.sdc.dpws.soap.exception.SoapFaultException}
     * <li>{@link org.somda.sdc.dpws.soap.exception.MarshallingException}
     * <li>{@link org.somda.sdc.dpws.soap.exception.TransportException}
     * <li>{@link org.somda.sdc.dpws.soap.interception.InterceptorException}
     * </ul>
     */
    ListenableFuture<SoapMessage> sendTransferGet(RequestResponseClient requestResponseClient, String wsaTo,
                                                  ReferenceParametersType referenceParameters);
}
