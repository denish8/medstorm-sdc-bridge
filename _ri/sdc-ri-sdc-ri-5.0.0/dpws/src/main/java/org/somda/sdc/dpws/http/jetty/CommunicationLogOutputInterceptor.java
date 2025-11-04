package org.somda.sdc.dpws.http.jetty;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.util.Callback;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.CommunicationLogContext;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;
import org.somda.sdc.dpws.soap.TransportInfo;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

/**
 * {@linkplain HttpOutput.Interceptor} which logs messages to a stream.
 */
public class CommunicationLogOutputInterceptor implements HttpOutput.Interceptor {
    private static final Logger LOG = LogManager.getLogger(CommunicationLogOutputInterceptor.class);

    private final HttpChannel channel;
    private final CommunicationLog communicationLog;
    private final TransportInfo transportInfo;
    private final Logger instanceLogger;
    private OutputStream commlogStream;
    private HttpOutput.Interceptor nextInterceptor;
    private String currentTransactionId;
    @Nullable
    private CommunicationLogContext communicationLogContext;

    CommunicationLogOutputInterceptor(HttpChannel channel,
                                      HttpOutput.Interceptor nextInterceptor,
                                      CommunicationLog communicationLog,
                                      @Nullable CommunicationLogContext communicationLogContext,
                                      TransportInfo transportInfo,
                                      String frameworkIdentifier,
                                      String currentTransactionId) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.channel = channel;
        this.communicationLog = communicationLog;
        this.nextInterceptor = nextInterceptor;
        this.transportInfo = transportInfo;
        this.commlogStream = null;
        this.currentTransactionId = currentTransactionId;
        this.communicationLogContext = communicationLogContext;
    }

    /***
     * Closes the underlying communication log stream if present.
     */
    public void close() {
        try {
            if (commlogStream != null) {
                commlogStream.close();
            }
        } catch (IOException e) {
            instanceLogger.error("Error while closing communication log stream", e);
        }
    }

    @Override
    public void write(ByteBuffer content, boolean last, Callback callback) {
        if (content == null) {
            nextInterceptor.write(content, last, callback);
            return;
        }

        // get commlog handle
        if (this.commlogStream == null) {
            this.commlogStream = getCommlogStream();
        }

        int oldPosition = content.position();
        try {
            WritableByteChannel writableByteChannel = Channels.newChannel(commlogStream);
            writableByteChannel.write(content);
            if (last) {
                close();
            }
        } catch (IOException e) {
            instanceLogger.error("Error while writing to commlog", e);
        }

        // rewind the byte buffer we just went through
        content.position(oldPosition);
        nextInterceptor.write(content, last, callback);
    }

    private OutputStream getCommlogStream() {
        var response = channel.getResponse();
        ListMultimap<String, String> responseHeaderMap = ArrayListMultimap.create();
        response.getHeaderNames().stream()
                .map(String::toLowerCase)
                // filter duplicates which occur because of capitalization
                .distinct()
                .forEach(
                        headerName -> {
                            var headers = response.getHeaders(headerName);
                            headers.forEach(header ->
                                    responseHeaderMap.put(headerName, header)
                            );
                        }
                );

        var responseHttpApplicationInfo = new HttpApplicationInfo(
                responseHeaderMap,
                currentTransactionId,
                null
        );

        var responseCommContext = new CommunicationContext(
                responseHttpApplicationInfo,
                transportInfo,
                communicationLogContext
        );

        return communicationLog.logMessage(
                CommunicationLog.Direction.OUTBOUND,
                CommunicationLog.TransportType.HTTP,
                CommunicationLog.MessageType.RESPONSE,
                responseCommContext);
    }

    @Override
    public HttpOutput.Interceptor getNextInterceptor() {
        return nextInterceptor;
    }

}
