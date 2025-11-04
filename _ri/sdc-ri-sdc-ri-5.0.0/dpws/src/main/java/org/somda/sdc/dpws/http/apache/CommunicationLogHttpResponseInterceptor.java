package org.somda.sdc.dpws.http.apache;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.conn.ManagedHttpClientConnection;
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

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.io.OutputStream;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.somda.sdc.dpws.http.apache.ClientTransportBinding.COMMUNICATION_LOG_CONTEXT_KEY;

/**
 * Response interceptor which writes the incoming response message and headers into the {@linkplain CommunicationLog}.
 */
public class CommunicationLogHttpResponseInterceptor implements HttpResponseInterceptor {
    private static final Logger LOG = LogManager.getLogger(CommunicationLogHttpResponseInterceptor.class);

    private final CommunicationLog commlog;
    private final Logger instanceLogger;

    CommunicationLogHttpResponseInterceptor(CommunicationLog communicationLog, String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.commlog = communicationLog;
    }

    @Override
    public void process(HttpResponse response, HttpContext context) {
        instanceLogger.debug("Processing response");

        HttpHost target = (HttpHost) context.getAttribute(
                HttpCoreContext.HTTP_TARGET_HOST);
        CommunicationLogContext communicationLogContext = (CommunicationLogContext)
                context.getAttribute(COMMUNICATION_LOG_CONTEXT_KEY);

        HttpEntity oldMessageEntity = response.getEntity();

        var currentTransactionOpt = Optional.of(context.getAttribute(CommunicationLog.MessageType.REQUEST.name()));
        var currentTransactionId = (String) currentTransactionOpt.orElse("");

        var requestHttpApplicationInfo = new HttpApplicationInfo(
                ApacheClientHelper.allHeadersToMultimap(response.getAllHeaders()),
                currentTransactionId,
                null
        );

        final List<X509Certificate> x509certificates = new ArrayList<>();
        ManagedHttpClientConnection routedConnection = null;
        try {
            routedConnection = (ManagedHttpClientConnection) context.getAttribute(
                    HttpCoreContext.HTTP_CONNECTION);
        } catch (ClassCastException e) {
            LOG.error("Error retrieving managed http client connection: {}", e.getMessage(), e);
        }
        if (routedConnection != null && routedConnection.isOpen()) {
            SSLSession sslSession = routedConnection.getSSLSession();
            try {
                if (sslSession != null
                        && sslSession.getPeerCertificates() != null) {
                    x509certificates.addAll(Arrays.stream(sslSession.getPeerCertificates())
                            .filter(certificate -> certificate instanceof X509Certificate)
                            .map(certificate -> (X509Certificate) certificate).collect(Collectors.toList()));
                }
            } catch (SSLPeerUnverifiedException e) {
                LOG.error("Error retrieving peer certificates: {} ", e.getMessage(), e);
            }
        }

        // collect information for TransportInfo
        var requestTransportInfo = new TransportInfo(
                target.getSchemeName(),
                null,
                null,
                target.getHostName(),
                target.getPort(),
                x509certificates
        );

        var requestCommContext = new CommunicationContext(
                requestHttpApplicationInfo,
                requestTransportInfo,
                communicationLogContext
        );

        OutputStream commlogStream = commlog.logMessage(
                CommunicationLog.Direction.INBOUND,
                CommunicationLog.TransportType.HTTP,
                CommunicationLog.MessageType.RESPONSE,
                requestCommContext);

        response.setEntity(new CommunicationLogEntity(oldMessageEntity, commlogStream));

        instanceLogger.debug("Processing response done");
    }
}
