package org.somda.sdc.dpws.http;

import com.google.common.util.concurrent.Service;
import org.somda.sdc.dpws.CommunicationLogContext;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.SoapConstants;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Simple HTTP server registry service.
 * <p>
 * Creates HTTP servers and registers handlers for context paths.
 * For proper shutdown of all servers {@link Service#stopAsync()} should be called.
 */
public interface HttpServerRegistry extends Service {
    /**
     * Creates an HTTP server at given URI.
     * <p>
     * If there was no running HTTP server found under the passed scheme and authority, this function starts a new one.
     * <em>If the HTTP server supports both http and https schemes,
     * the return value scheme will match the requested value.</em>
     *
     * @param schemeAndAuthority the scheme and authority where to access the HTTP server. If port number is
     *                           0, then a random open port is selected and will be part of the returned URI.
     * @param allowReuse         if the port given in schemeAndAuthority is 0, setting this to true will allow returning
     *                           any already running server, while false will create a new server.
     *                           Has no effect if port is not 0.
     * @return the actual assigned URI of the HTTP server.
     */
    String initHttpServer(String schemeAndAuthority, boolean allowReuse);

    /**
     * Registers a handler for HTTP requests destined to the given scheme, authority and context path.
     * <p>
     * <em>If the HTTP server supports both http and https schemes,
     * the return value scheme will match the requested value.</em>
     *
     * @param schemeAndAuthority      scheme and authority used to start a new or re-use an existing HTTP server.
     * @param allowReuse              if the port given in schemeAndAuthority is 0, settings this to true will allow
     *                                returning any already running server, while false will create a new server.
     *                                Has no effect if port is not 0.
     * @param contextPath             the context path where the given registry shall listen to.<br>
     *                                <em>Important note: the context path needs to start with a slash.</em>
     * @param mediaType               the media type of the response the handler will produce.
     *                                Null defaults to {@link SoapConstants#MEDIA_TYPE_SOAP}.
     * @param communicationLogContext additional information made available in the communication log of the registered
     *                                context.
     *                                Only has an effect if
     *                                {@link org.somda.sdc.dpws.DpwsConfig#HTTP_SERVER_COMMLOG_IN_HANDLER} is enabled
     * @param handler                 the handler callback that is invoked on a request to the given context path.
     * @return the actual full path of the HTTP server address the given handler listens to.
     * @see #initHttpServer(String, boolean)
     */
    String registerContext(String schemeAndAuthority,
                           boolean allowReuse,
                           String contextPath,
                           @Nullable String mediaType,
                           @Nullable CommunicationLogContext communicationLogContext,
                           HttpHandler handler);

    /**
     * Removes a handler for the given scheme, authority and context path.
     * <p>
     * {@link HttpHandler#handle(InputStream, OutputStream, CommunicationContext)}
     * will not be called for any request destined to the corresponding handler.
     * Requests to the corresponding are answered with an HTTP 404.
     *
     * @param schemeAndAuthority scheme and authority where the context shall be removed.
     * @param contextPath        the context path to remove.
     *                           <em>The context path needs to start with a slash.</em>
     */
    void unregisterContext(String schemeAndAuthority, String contextPath);
}
