package org.somda.sdc.dpws;

import com.google.common.io.ByteStreams;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.soap.CommunicationContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Default implementation of {@linkplain CommunicationLog}.
 */
public class CommunicationLogImpl implements CommunicationLog {
    private static final Logger LOG = LogManager.getLogger(CommunicationLogImpl.class);

    private final CommunicationLogSink logSink;
    private final Logger instanceLogger;

    @AssistedInject
    CommunicationLogImpl(CommunicationLogSink sink,
                         @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.logSink = sink;
    }

    @Override
    public TeeOutputStream logMessage(Direction direction,
                                      TransportType transportType,
                                      MessageType messageType,
                                      CommunicationContext communicationContext,
                                      OutputStream message) {
        OutputStream logFile = this.logSink.createTargetStream(
                transportType,
                direction,
                messageType,
                communicationContext);
        return new TeeOutputStream(message, logFile);
    }

    @Override
    public OutputStream logMessage(Direction direction,
                                   TransportType transportType,
                                   MessageType messageType,
                                   CommunicationContext communicationContext) {
        return this.logSink.createTargetStream(transportType, direction, messageType, communicationContext);
    }

    @Override
    public InputStream logMessage(Direction direction,
                                  TransportType transportType,
                                  MessageType messageType,
                                  CommunicationContext communicationContext,
                                  InputStream message) {
        return writeLogFile(transportType, direction, messageType, communicationContext, message);
    }

    private InputStream writeLogFile(TransportType transportType,
                                     Direction direction,
                                     MessageType messageType,
                                     CommunicationContext communicationContext,
                                     InputStream inputStream) {
        try {
            final byte[] bytes = ByteStreams.toByteArray(inputStream);

            try (OutputStream targetStream = this.logSink.createTargetStream(transportType, direction, messageType,
                    communicationContext)) {
                new ByteArrayInputStream(bytes).transferTo(targetStream);
            }
            return new ByteArrayInputStream(bytes);
        } catch (IOException e) {
            instanceLogger.warn("Could not write to communication log file", e);
        }
        return inputStream;
    }
}
