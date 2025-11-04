package org.somda.sdc.dpws.http.jetty;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.CommunicationLogContext;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.http.HttpException;
import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;
import org.somda.sdc.dpws.soap.TransportInfo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;


/**
 * {@linkplain AbstractHandler} implementation based on Jetty HTTP servers.
 */
public class JettyHttpServerHandler extends AbstractHandler {
    @SuppressWarnings("UastIncorrectHttpHeaderInspection")
    public static final String SERVER_HEADER_KEY = "X-Server";
    public static final String SERVER_HEADER_VALUE = "SDCri";

    private static final Logger LOG = LogManager.getLogger(JettyHttpServerHandler.class);

    private final String mediaType;
    private final HttpHandler handler;
    private final Logger instanceLogger;
    private final boolean chunkedTransfer;
    private final String charset;
    @Nullable
    private final CommunicationLog communicationLog;
    private final String frameworkIdentifier;
    @Nullable
    private final CommunicationLogContext communicationLogContext;


    @AssistedInject
    JettyHttpServerHandler(@Assisted String mediaType,
                           @Assisted HttpHandler handler,
                           @Assisted @Nullable CommunicationLog communicationLog,
                           @Assisted @Nullable CommunicationLogContext communicationLogContext,
                           @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                           @Named(DpwsConfig.ENFORCE_HTTP_CHUNKED_TRANSFER) boolean chunkedTransfer,
                           @Named(DpwsConfig.HTTP_CONTENT_TYPE_CHARSET) String charset) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.mediaType = mediaType;
        this.handler = handler;
        this.chunkedTransfer = chunkedTransfer;
        this.charset = charset;
        this.communicationLog = communicationLog;
        this.frameworkIdentifier = frameworkIdentifier;
        this.communicationLogContext = communicationLogContext;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        final Supplier<String> remoteNodeInfo = () -> getRemoteNodeInfo(request);
        var transactionIdOpt = Optional.ofNullable(
                baseRequest.getAttribute(CommunicationLog.MessageType.REQUEST.name()));
        var transactionId = (String) transactionIdOpt.orElse("");

        instanceLogger.debug("{}: Request to {}", remoteNodeInfo::get, request::getRequestURL);

        var out = baseRequest.getResponse().getHttpOutput();
        HttpOutput.Interceptor previousInterceptor = out.getInterceptor();

        var requestHttpApplicationInfo = new HttpApplicationInfo(
                JettyUtil.getRequestHeaders(request),
                transactionId,
                baseRequest.getRequestURI()
        );

        JettyUtil.handleCommlog(
                communicationLog,
                baseRequest,
                request,
                communicationLogContext,
                requestHttpApplicationInfo,
                frameworkIdentifier,
                previousInterceptor,
                out,
                transactionId
        );

        response.setStatus(HttpStatus.OK_200);
        response.setContentType(mediaType);
        response.setCharacterEncoding(charset);
        response.setHeader(SERVER_HEADER_KEY, SERVER_HEADER_VALUE);

        ByteArrayOutputStream tempOut = new ByteArrayOutputStream();

        var input = request.getInputStream();
        try {
            handler.handle(input, tempOut,
                    new CommunicationContext(requestHttpApplicationInfo,
                            new TransportInfo(
                                    request.getScheme(),
                                    request.getLocalAddr(),
                                    request.getLocalPort(),
                                    request.getRemoteAddr(),
                                    request.getRemotePort(),
                                    getX509Certificates(request, baseRequest.isSecure())
                            ),
                            communicationLogContext
                    )
            );

        } catch (HttpException e) {
            instanceLogger.warn("{}: An HTTP exception occurred during HTTP request processing. Error message: {}",
                    remoteNodeInfo::get,
                    e::getMessage);
            instanceLogger.trace(() -> String.format(
                    "%s: An HTTP exception occurred during HTTP request processing",
                    remoteNodeInfo.get()),
                    e);
            response.setStatus(e.getStatusCode());
            if (!e.getMessage().isEmpty()) {
                tempOut.write(e.getMessage().getBytes());
            }
        } finally {
            baseRequest.setHandled(true);
        }

        final byte[] tempOutValue = tempOut.toByteArray();

        if (this.chunkedTransfer) {
            response.setHeader("Transfer-Encoding", "chunked");
        } else {
            response.setHeader("Content-Length", String.valueOf(tempOutValue.length));
        }

        OutputStream output = response.getOutputStream();
        output.write(tempOutValue);

        try {
            input.close();
            output.flush();
            output.close();
        } catch (IOException e) {
            instanceLogger.error(
                    "{}: Could not close input/output streams from incoming HTTP request to {}. Reason: {}",
                    remoteNodeInfo::get,
                    request::getRequestURL,
                    e::getMessage);
            instanceLogger.trace(() -> String.format(
                    "%s: Could not close input/output streams from incoming HTTP request to %s",
                    remoteNodeInfo.get(),
                    request.getRequestURL()),
                    e);
        } finally {
            // reset interceptor if we have a commlog here, which means we fiddled with the interceptors
            if (communicationLog != null) {
                out.setInterceptor(previousInterceptor);
            }
        }
    }

    /**
     * Static helper function to get X509 certificate information from an HTTP servlet.
     *
     * @param request   servlet request data.
     * @param expectTLS causes this function to return an empty list if set to false.
     * @return a list of {@link X509Certificate} containers.
     * @throws IOException in case the certificate information does not match the expected type, which is an array of
     *                     {@link X509Certificate}.
     */
    static List<X509Certificate> getX509Certificates(HttpServletRequest request, boolean expectTLS)
            throws IOException {
        if (!expectTLS) {
            return Collections.emptyList();
        }

        var anonymousCertificates = request.getAttribute("jakarta.servlet.request.X509Certificate");
        if (anonymousCertificates == null) {
            LOG.error("{}: Certificate information is missing from HTTP request data",
                    () -> getRemoteNodeInfo(request));
            throw new IOException("Certificate information is missing from HTTP request data");
        } else {
            if (anonymousCertificates instanceof X509Certificate[]) {
                return List.of((X509Certificate[]) anonymousCertificates);
            } else {
                LOG.error("Certificate information is of an unexpected type: {}", anonymousCertificates.getClass());
                throw new IOException(String.format("Certificate information is of an unexpected type: %s",
                        anonymousCertificates.getClass()));
            }
        }
    }

    private static String getRemoteNodeInfo(HttpServletRequest request) {
        return String.format("%s://%s:%s", request.getScheme(), request.getRemoteAddr(), request.getRemotePort());
    }
}
