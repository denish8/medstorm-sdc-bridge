package org.somda.sdc.dpws.http.apache;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.CommunicationLogContext;
import org.somda.sdc.dpws.http.apache.helper.ApacheClientHelper;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;
import org.somda.sdc.dpws.soap.TransportInfo;

import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.somda.sdc.dpws.http.apache.ClientTransportBinding.COMMUNICATION_LOG_CONTEXT_KEY;

/**
 * Request interceptor which writes the outgoing request message and headers into the {@linkplain CommunicationLog}.
 */
public class CommunicationLogHttpRequestInterceptor implements HttpRequestInterceptor {
    private static final Logger LOG = LogManager.getLogger(CommunicationLogHttpRequestInterceptor.class);
    private static final String TRANSACTION_ID_PREFIX_CLIENT = "rrId:client:" + UUID.randomUUID() + ":";
    private static final AtomicLong TRANSACTION_ID = new AtomicLong(-1L);

    private final CommunicationLog commlog;
    private final Logger instanceLogger;
    private final List<X509Certificate> certificates;

    CommunicationLogHttpRequestInterceptor(CommunicationLog communicationLog, String frameworkIdentifier,
                                           List<X509Certificate> certificateList) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.commlog = communicationLog;
        this.certificates = certificateList;
    }

    @Override
    public void process(HttpRequest request, HttpContext context) {
        instanceLogger.debug("Processing request: {}", request.getRequestLine());

        HttpHost target = (HttpHost) context.getAttribute(
                HttpCoreContext.HTTP_TARGET_HOST);
        CommunicationLogContext communicationLogContext = (CommunicationLogContext)
                context.getAttribute(COMMUNICATION_LOG_CONTEXT_KEY);

        var currentTransactionId = TRANSACTION_ID_PREFIX_CLIENT + TRANSACTION_ID.incrementAndGet();
        context.setAttribute(CommunicationLog.MessageType.REQUEST.name(), currentTransactionId);
        var requestHttpApplicationInfo = new HttpApplicationInfo(
                ApacheClientHelper.allHeadersToMultimap(request.getAllHeaders()),
                currentTransactionId,
                request.getRequestLine().getUri()
        );

        // collect information for TransportInfo
        var requestTransportInfo = new TransportInfo(
                target.getSchemeName(),
                null,
                null,
                target.getHostName(),
                target.getPort(),
                certificates
        );

        var requestCommContext = new CommunicationContext(
                requestHttpApplicationInfo,
                requestTransportInfo,
                communicationLogContext
        );

        OutputStream commlogStream = commlog.logMessage(
                CommunicationLog.Direction.OUTBOUND,
                CommunicationLog.TransportType.HTTP,
                CommunicationLog.MessageType.REQUEST,
                requestCommContext);

        if (!(request instanceof HttpEntityEnclosingRequest)) {
            // GET doesn't have any entity, but still has headers to save
            instanceLogger.debug("Request doesn't have a body {}, closing stream", request.getRequestLine());
            try {
                commlogStream.close();
            } catch (IOException e) {
                // not totally harmful, nothing was inside the body
                LOG.warn("Could not close empty output stream. {}", e.getMessage());
                LOG.trace("Could not close empty output stream.", e);
            }
            return;
        }

        var entityRequest = (HttpEntityEnclosingRequest) request;

        HttpEntity oldMessageEntity = entityRequest.getEntity();

        entityRequest.setEntity(new CommunicationLogEntity(oldMessageEntity, commlogStream));

        instanceLogger.debug("Processing request done: {}", request.getRequestLine());
    }
}
