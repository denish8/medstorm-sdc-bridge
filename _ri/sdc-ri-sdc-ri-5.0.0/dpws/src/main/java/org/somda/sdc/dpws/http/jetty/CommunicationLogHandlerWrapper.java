package org.somda.sdc.dpws.http.jetty;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@linkplain HandlerWrapper} which enables {@linkplain CommunicationLog} capabilities for requests and responses.
 */
public class CommunicationLogHandlerWrapper extends HandlerWrapper {
    private static final String TRANSACTION_ID_PREFIX_SERVER = "rrId:server:" + UUID.randomUUID() + ":";
    private static final AtomicLong TRANSACTION_ID = new AtomicLong(-1L);
    @Nullable
    private final CommunicationLog commLog;
    private final String frameworkIdentifier;

    CommunicationLogHandlerWrapper(@Nullable CommunicationLog commLog, String frameworkIdentifier) {
        this.frameworkIdentifier = frameworkIdentifier;
        this.commLog = commLog;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        var currentTransactionId = TRANSACTION_ID_PREFIX_SERVER + TRANSACTION_ID.incrementAndGet();
        baseRequest.setAttribute(CommunicationLog.MessageType.REQUEST.name(), currentTransactionId);

        var out = baseRequest.getResponse().getHttpOutput();
        HttpOutput.Interceptor previousInterceptor = out.getInterceptor();

        var requestHttpApplicationInfo = new HttpApplicationInfo(
                JettyUtil.getRequestHeaders(request),
                currentTransactionId,
                baseRequest.getRequestURI()
        );

        JettyUtil.handleCommlog(
                commLog,
                baseRequest,
                request,
                null,
                requestHttpApplicationInfo,
                frameworkIdentifier,
                previousInterceptor,
                out,
                currentTransactionId
        );

        try {
            // trigger request handling
            super.handle(target, baseRequest, request, response);
        } finally {
            // reset interceptor if request was not handled and we have a commlog here,
            // which means we fiddled with the interceptors
            if (!baseRequest.isHandled() && !baseRequest.isAsyncStarted() && commLog != null)
                out.setInterceptor(previousInterceptor);
        }
    }
}
