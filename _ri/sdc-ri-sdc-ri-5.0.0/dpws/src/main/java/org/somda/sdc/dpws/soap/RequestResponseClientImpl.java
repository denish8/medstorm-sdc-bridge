package org.somda.sdc.dpws.soap;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.interception.ClientDispatcher;
import org.somda.sdc.dpws.soap.interception.Direction;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.dpws.soap.interception.InterceptorRegistry;
import org.somda.sdc.dpws.soap.interception.RequestObject;
import org.somda.sdc.dpws.soap.interception.RequestResponseCallback;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingClientInterceptor;

import java.util.Collections;

/**
 * Default implementation of {@linkplain RequestResponseClient}.
 */
public class RequestResponseClientImpl implements RequestResponseClient {

    private final InterceptorRegistry interceptorRegistry;
    private final RequestResponseCallback networkCallback;
    private final ClientDispatcher clientDispatcher;

    @AssistedInject
    RequestResponseClientImpl(@Assisted RequestResponseCallback networkCallback,
                              ClientDispatcher clientDispatcher,
                              InterceptorRegistry interceptorRegistry,
                              WsAddressingClientInterceptor wsaClientInterceptor) {
        this.networkCallback = networkCallback;
        this.clientDispatcher = clientDispatcher;
        this.interceptorRegistry = interceptorRegistry;

        // Enable WS-Addressing commons on this client
        register(wsaClientInterceptor);
    }

    @Override
    public void register(Interceptor interceptor) {
        interceptorRegistry.addInterceptor(interceptor);
    }

    @Override
    public SoapMessage sendRequestResponse(SoapMessage request)
            throws SoapFaultException, MarshallingException, TransportException, InterceptorException {
        RequestObject rObj = new RequestObject(request);
        clientDispatcher.invokeDispatcher(Direction.REQUEST, interceptorRegistry, request, rObj);

        SoapMessage response = networkCallback.onRequestResponse(request);
        if (response.isFault()) {
            throw new SoapFaultException(response, request.getWsAddressingHeader().getMessageId().orElse(null));
        }

        // create dummy object as actual transport info has to be populated by the ultimate segment in the interceptor
        // chain
        final var transportInfo = new TransportInfo(
                "",
                null,
                null,
                null,
                null,
                Collections.emptyList()
        );

        RequestResponseObject rrObj = new RequestResponseObject(
                request,
                response,
                new CommunicationContext(new ApplicationInfo(), transportInfo, null));
        clientDispatcher.invokeDispatcher(Direction.RESPONSE, interceptorRegistry, response, rrObj);

        return response;
    }
}
