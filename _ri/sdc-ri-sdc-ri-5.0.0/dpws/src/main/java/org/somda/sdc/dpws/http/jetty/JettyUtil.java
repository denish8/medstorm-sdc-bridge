package org.somda.sdc.dpws.http.jetty;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Request;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.CommunicationLogContext;
import org.somda.sdc.dpws.soap.ApplicationInfo;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.TransportInfo;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.stream.Stream;

/**
 * Jetty utilities.
 */
public class JettyUtil {
    /**
     * Returns all available headers from an incoming request.
     *
     * @param request to extract headers from.
     * @return extracted headers as a multimap, without duplicates.
     */
    static ListMultimap<String, String> getRequestHeaders(HttpServletRequest request) {
        ListMultimap<String, String> requestHeaderMap = ArrayListMultimap.create();
        var nameIter = request.getHeaderNames().asIterator();
        Stream.generate(() -> null) // what
                .takeWhile(x -> nameIter.hasNext())
                .map(n -> nameIter.next().toLowerCase())
                // filter duplicates which occur because of capitalization
                .distinct()
                .forEach(
                        headerName -> {
                            var headers = request.getHeaders(headerName);
                            headers.asIterator().forEachRemaining(header ->
                                    requestHeaderMap.put(headerName, header)
                            );
                        }
                );
        return requestHeaderMap;
    }

    // CHECKSTYLE.OFF: ParameterNumber
    static void handleCommlog(
            @Nullable CommunicationLog communicationLog,
            Request baseRequest,
            HttpServletRequest request,
            @Nullable CommunicationLogContext communicationLogContext,
            ApplicationInfo requestHttpApplicationInfo,
            String frameworkIdentifier,
            HttpOutput.Interceptor previousInterceptor,
            HttpOutput out,
            String transactionId
    ) throws IOException {
        // CHECKSTYLE.ON: ParameterNumber
        if (communicationLog != null) {

            // collect information for TransportInfo
            var requestCertificates = JettyHttpServerHandler.getX509Certificates(request, baseRequest.isSecure());
            var transportInfo = new TransportInfo(
                    request.getScheme(),
                    request.getLocalAddr(),
                    request.getLocalPort(),
                    request.getRemoteAddr(),
                    request.getRemotePort(),
                    requestCertificates
            );

            var requestCommContext = new CommunicationContext(
                    requestHttpApplicationInfo,
                    transportInfo,
                    communicationLogContext
            );

            OutputStream commlogInput = communicationLog.logMessage(
                    CommunicationLog.Direction.INBOUND,
                    CommunicationLog.TransportType.HTTP,
                    CommunicationLog.MessageType.REQUEST,
                    requestCommContext);

            // attach interceptor to log request
            baseRequest.getHttpInput().addInterceptor(
                    new CommunicationLogInputInterceptor(commlogInput, frameworkIdentifier)
            );

            // attach interceptor to log response
            var outInterceptor = new CommunicationLogOutputInterceptor(
                    baseRequest.getHttpChannel(),
                    previousInterceptor,
                    communicationLog,
                    communicationLogContext,
                    transportInfo,
                    frameworkIdentifier,
                    transactionId
            );
            out.setInterceptor(outInterceptor);
        }
    }
}
