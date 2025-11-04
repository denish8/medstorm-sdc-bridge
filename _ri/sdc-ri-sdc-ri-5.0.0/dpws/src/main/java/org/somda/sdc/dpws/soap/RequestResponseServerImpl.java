package org.somda.sdc.dpws.soap;

import com.google.inject.Inject;
import org.somda.sdc.dpws.guice.DeviceSpecific;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.factory.SoapFaultFactory;
import org.somda.sdc.dpws.soap.interception.Direction;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.interception.InterceptorRegistry;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.dpws.soap.interception.ServerDispatcher;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingConstants;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingServerInterceptor;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.somda.sdc.dpws.soap.wsaddressing.model.ObjectFactory;

/**
 * Default implementation of {@linkplain RequestResponseServer}.
 */
public class RequestResponseServerImpl implements RequestResponseServer {

    private final InterceptorRegistry interceptorRegistry;
    private final SoapFaultFactory soapFaultFactory;
    private final ObjectFactory wsaObjectFactory;
    private final WsAddressingUtil wsAddressingUtil;
    private final WsAddressingServerInterceptor wsaServerInterceptor;
    private final ServerDispatcher serverDispatcher;

    @Inject
    RequestResponseServerImpl(ServerDispatcher serverDispatcher,
                              InterceptorRegistry interceptorRegistry,
                              SoapFaultFactory soapFaultFactory,
                              ObjectFactory wsaObjectFactory,
                              WsAddressingUtil wsAddressingUtil,
                              @DeviceSpecific WsAddressingServerInterceptor wsaServerInterceptor) {
        this.serverDispatcher = serverDispatcher;
        this.interceptorRegistry = interceptorRegistry;
        this.soapFaultFactory = soapFaultFactory;
        this.wsaObjectFactory = wsaObjectFactory;
        this.wsAddressingUtil = wsAddressingUtil;
        this.wsaServerInterceptor = wsaServerInterceptor;
        register(wsaServerInterceptor);
    }

    @Override
    public void register(Interceptor interceptor) {
        interceptorRegistry.addInterceptor(interceptor);
    }

    @Override
    public void receiveRequestResponse(SoapMessage request,
                                       SoapMessage response,
                                       CommunicationContext communicationContext) throws SoapFaultException {

        var actionHeader = request.getWsAddressingHeader().getAction();
        if (actionHeader.isEmpty()) {
            // See https://www.w3.org/TR/ws-addr-soap/#missingmapfault
            throw new SoapFaultException(soapFaultFactory.createFault(
                    WsAddressingConstants.FAULT_ACTION,
                    SoapConstants.SENDER,
                    WsAddressingConstants.MESSAGE_ADDRESSING_HEADER_REQUIRED,
                    "A required header representing a Message Addressing Property is not present",
                    wsaObjectFactory.createProblemHeaderQName(wsAddressingUtil.createAttributedQNameType(
                            WsAddressingConstants.QNAME_ACTION
                    ))), request.getWsAddressingHeader().getMessageId().orElse(null));
        }

        var action = wsAddressingUtil.getAddressUriString(actionHeader.orElse(new AttributedURIType()));
        if (interceptorRegistry.getInterceptors(Direction.REQUEST, action).isEmpty() &&
                interceptorRegistry.getInterceptors(Direction.ANY, action).isEmpty()) {
            // Action not supported as no matching interceptor handler could be found, hence throw SOAP fault
            // according to https://www.w3.org/TR/ws-addr-soap/#actionfault
            var problemActionType = wsaObjectFactory.createProblemActionType();
            problemActionType.setAction(wsAddressingUtil.createAttributedURIType(action));
            throw new SoapFaultException(soapFaultFactory.createFault(
                    WsAddressingConstants.FAULT_ACTION,
                    SoapConstants.SENDER,
                    WsAddressingConstants.ACTION_NOT_SUPPORTED,
                    "The [action] cannot be processed at the receiver",
                    wsaObjectFactory.createProblemAction(problemActionType)),
                    request.getWsAddressingHeader().getMessageId().orElse(null));
        }

        RequestResponseObject rrObj = new RequestResponseObject(request, response, communicationContext);
        serverDispatcher.invokeDispatcher(Direction.REQUEST, interceptorRegistry, request, rrObj);

        rrObj = new RequestResponseObject(request, response, communicationContext);
        serverDispatcher.invokeDispatcher(Direction.RESPONSE, interceptorRegistry, response, rrObj);
    }
}
