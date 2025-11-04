package org.somda.sdc.dpws.soap.interception;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.factory.SoapFaultFactory;
import org.somda.sdc.dpws.soap.wsaddressing.model.AttributedURIType;

import java.util.Optional;

/**
 * Interceptor dispatcher designed for incoming messages on servers.
 */
public class ServerDispatcher {
    private static final Logger LOG = LogManager.getLogger(ServerDispatcher.class);

    private final InterceptorProcessor interceptorProcessor;
    private final SoapFaultFactory soapFaultFactory;
    private final Logger instanceLogger;

    @Inject
    public ServerDispatcher(InterceptorProcessor interceptorProcessor,
                            SoapFaultFactory soapFaultFactory,
                            @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.interceptorProcessor = interceptorProcessor;
        this.soapFaultFactory = soapFaultFactory;
    }

    /**
     * Starts dispatching a SOAP message along an interceptor chain.
     *
     * @param direction                 the communication direction used for dispatching.
     * @param registry                  the interceptor registry used to seek interceptors.
     * @param soapMessage               the SOAP message to dispatch.
     * @param interceptorCallbackObject the object where to dispatch the message to.
     * @throws SoapFaultException if the interceptor was cancelled
     *                            (in order to communicate this as an error to the client).
     */
    public void invokeDispatcher(Direction direction,
                                 InterceptorRegistry registry,
                                 SoapMessage soapMessage,
                                 InterceptorCallbackType interceptorCallbackObject) throws SoapFaultException {
        Optional<AttributedURIType> action = soapMessage.getWsAddressingHeader().getAction();
        String actionUri = null;
        if (action.isPresent()) {
            actionUri = action.get().getValue();
        }

        try {
            interceptorProcessor.dispatch(direction, registry, actionUri, interceptorCallbackObject);
        } catch (InterceptorException e) {
            instanceLogger.debug("Caught interceptor exception from {} with message: {}",
                    e.getInterceptor(), e.getMessage());
            if (e.getCause() instanceof SoapFaultException) {
                throw (SoapFaultException) e.getCause();
            }
            throw new SoapFaultException(soapFaultFactory.createReceiverFault(
                    String.format("Server fault information: %s", e.getCause().getMessage())),
                    soapMessage.getWsAddressingHeader().getMessageId().orElse(null));
            // CHECKSTYLE.OFF: IllegalCatch
        } catch (Exception e) {
            // CHECKSTYLE.ON: IllegalCatch
            instanceLogger.warn("Unexpected exception thrown during dispatcher invocation routine: {}", e.getMessage());
            throw new SoapFaultException(soapFaultFactory.createReceiverFault(
                    String.format("Server fault information: %s", e.getMessage())),
                    soapMessage.getWsAddressingHeader().getMessageId().orElse(null));
        }
    }
}
