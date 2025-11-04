package org.somda.sdc.dpws.device.helper;

import com.google.common.collect.ListMultimap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.http.ContentType;
import org.somda.sdc.dpws.http.HttpException;
import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;
import org.somda.sdc.dpws.soap.MarshallingService;
import org.somda.sdc.dpws.soap.RequestResponseServer;
import org.somda.sdc.dpws.soap.SoapDebug;
import org.somda.sdc.dpws.soap.SoapFaultHttpStatusCodeMapping;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.interception.InterceptorHandler;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;

/**
 * {@linkplain RequestResponseServer} that is invoked on SOAP {@linkplain HttpHandler} callbacks.
 * <p>
 * The handler is an {@link InterceptorHandler}.
 * All objects registered via {@link #register(Interceptor)} receive the requests delivered to
 * {@link HttpHandler#handle(InputStream, OutputStream, CommunicationContext)}.
 */
public class RequestResponseServerHttpHandler implements HttpHandler, InterceptorHandler {
    static final String NO_CONTENT_TYPE_MESSAGE = "Could not parse Content-Type header element";

    private static final Logger LOG = LogManager.getLogger(RequestResponseServerHttpHandler.class);

    private final RequestResponseServer reqResServer;
    private final MarshallingService marshallingService;
    private final SoapUtil soapUtil;
    private final Logger instanceLogger;

    @Inject
    RequestResponseServerHttpHandler(RequestResponseServer reqResServer,
                                     MarshallingService marshallingService,
                                     SoapUtil soapUtil,
                                     @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.reqResServer = reqResServer;
        this.marshallingService = marshallingService;
        this.soapUtil = soapUtil;
    }

    @Override
    public void handle(InputStream inStream, OutputStream outStream, CommunicationContext communicationContext)
            throws HttpException {
        SoapMessage requestMsg;
        ListMultimap<String, String> headers;
        try {
            headers = ((HttpApplicationInfo) communicationContext.getApplicationInfo()).getHeaders();
        } catch (ClassCastException e) {
            var errorText = "Unexpected ApplicationInfo received, expected HttpApplicationInfo.";
            instanceLogger.error(errorText);
            throw new HttpException(HttpStatus.INTERNAL_SERVER_ERROR_500, errorText);
        }
        var contentTypeOpt = ContentType.fromListMultimap(headers);
        if (contentTypeOpt.isEmpty()) {
            throw new HttpException(HttpStatus.BAD_REQUEST_400, NO_CONTENT_TYPE_MESSAGE);
        }
        var contentType = contentTypeOpt.get();
        try {
            // wrap up content with a known or forced charset into a reader
            if (contentType.getCharset() != null) {
                Reader reader = new InputStreamReader(inStream, contentType.getCharset());
                requestMsg = marshallingService.unmarshal(reader);
            } else {
                // let jaxb figure it out otherwise
                requestMsg = marshallingService.unmarshal(inStream);
            }
        } catch (MarshallingException e) {
            throw new HttpException(HttpStatus.BAD_REQUEST_400,
                    String.format("Error unmarshalling HTTP input stream: %s", e.getMessage()));
        }

        instanceLogger.debug("Incoming SOAP/HTTP request: {}", () -> SoapDebug.get(requestMsg));

        SoapMessage responseMsg = soapUtil.createMessage();

        // Postpone throw of exception which in case of a SoapFaultException allows to
        // marshal response and make debug output
        HttpException httpExceptionToThrow = null;
        try {
            reqResServer.receiveRequestResponse(requestMsg, responseMsg, communicationContext);
        } catch (SoapFaultException e) {
            responseMsg = e.getFaultMessage();
            instanceLogger.warn("{}: Processing message triggered soap fault: {}",
                    communicationContext.getTransportInfo().getRemoteNodeInfo(),
                    e.getMessage());
            instanceLogger.trace("Processing message triggered soap fault", e);
            httpExceptionToThrow = new HttpException(SoapFaultHttpStatusCodeMapping.get(e.getFault()));
        }

        try {
            marshallingService.marshal(responseMsg, outStream);
        } catch (MarshallingException e) {
            throw new HttpException(HttpStatus.INTERNAL_SERVER_ERROR_500,
                    String.format("Error marshalling HTTP output stream: %s", e.getMessage()));
        }

        if (instanceLogger.isDebugEnabled()) {
            instanceLogger.debug("Outgoing SOAP/HTTP response: {}", SoapDebug.get(responseMsg));
        }

        if (httpExceptionToThrow != null) {
            throw httpExceptionToThrow;
        }
    }

    @Override
    public void register(Interceptor interceptor) {
        reqResServer.register(interceptor);
    }
}
