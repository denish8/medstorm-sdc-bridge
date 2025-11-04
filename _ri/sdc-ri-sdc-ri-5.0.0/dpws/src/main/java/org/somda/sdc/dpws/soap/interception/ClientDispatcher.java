package org.somda.sdc.dpws.soap.interception;

import com.google.inject.Inject;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import java.util.Optional;

/**
 * Interceptor dispatcher designed for outgoing calls on clients.
 */
public class ClientDispatcher {
    private final InterceptorProcessor interceptorProcessor;

    @Inject
    ClientDispatcher(InterceptorProcessor interceptorProcessor) {
        this.interceptorProcessor = interceptorProcessor;
    }

    /**
     * Starts dispatching a SOAP message along an interceptor chain.
     *
     * @param direction                 the communication direction used for dispatching.
     * @param registry                  the interceptor registry used to seek interceptors.
     * @param soapMessage               the SOAP message to dispatch.
     * @param interceptorCallbackObject the object where to dispatch the message to.
     * @throws InterceptorException in case any exception is thrown.
     */
    public void invokeDispatcher(Direction direction,
                                 InterceptorRegistry registry,
                                 SoapMessage soapMessage,
                                 InterceptorCallbackType interceptorCallbackObject) throws InterceptorException {
        Optional<AttributedURIType> action = soapMessage.getWsAddressingHeader().getAction();
        String actionUri = null;
        if (action.isPresent()) {
            actionUri = action.get().getValue();
        }

        interceptorProcessor.dispatch(direction, registry, actionUri, interceptorCallbackObject);
    }
}
