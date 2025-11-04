package org.somda.sdc.dpws;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.helper.CommunicationLogFileName;
import org.somda.sdc.dpws.helper.CommunicationLogFileOutputStream;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * Default implementation of {@linkplain CommunicationLogSink}.
 */
public class CommunicationLogSinkImpl implements CommunicationLogSink {
    private static final Logger LOG = LogManager.getLogger(CommunicationLogSinkImpl.class);
    private final Boolean createHttpHeaders;
    private final Boolean createHttpRequestResponseId;
    private final Boolean prettyPrintXml;
    private final Logger instanceLogger;

    private final EnumMap<CommunicationLog.TransportType, File> dirMapping;

    @Inject
    CommunicationLogSinkImpl(@Named(DpwsConfig.COMMUNICATION_LOG_SINK_DIRECTORY) File logDirectory,
                             @Named(DpwsConfig.COMMUNICATION_LOG_WITH_HTTP_HEADERS) Boolean createHttpHeaders,
                             @Named(DpwsConfig.COMMUNICATION_LOG_WITH_HTTP_REQUEST_RESPONSE_ID)
                                     Boolean createHttpRequestResponseId,
                             @Named(DpwsConfig.COMMUNICATION_LOG_PRETTY_PRINT_XML) Boolean prettyPrintXml,
                             @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.createHttpRequestResponseId = createHttpRequestResponseId;
        this.prettyPrintXml = prettyPrintXml;
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.createHttpHeaders = createHttpHeaders;

        this.dirMapping = new EnumMap<>(CommunicationLog.TransportType.class);

        for (CommunicationLog.TransportType transportType : CommunicationLog.TransportType.values()) {
            File subDirFile = new File(logDirectory, transportType.toString());

            if (!subDirFile.exists() && !subDirFile.mkdirs()) {
                this.dirMapping.put(transportType, null);

                instanceLogger.warn("Could not create the communication log directory '{}{}{}'",
                        logDirectory.getAbsolutePath(), File.separator, subDirFile.getName());
            } else {
                this.dirMapping.put(transportType, subDirFile);
            }
        }

    }

    @Override
    public OutputStream createTargetStream(CommunicationLog.TransportType transportType,
                                           CommunicationLog.Direction direction,
                                           CommunicationLog.MessageType messageType,
                                           CommunicationContext communicationContext) {
        var outputStream = OutputStream.nullOutputStream();

        var dir = dirMapping.get(transportType);
        if (dir == null) {
            instanceLogger.warn("The directory for the given transport type was not configured.");
            return outputStream;
        }

        var fileNamePrefix = CommunicationLogFileName.create(direction.toString(), communicationContext);

        // if message is http, we can store header info and request response relation info too
        if (communicationContext.getApplicationInfo() instanceof HttpApplicationInfo) {
            if (createHttpHeaders) {
                var headerPath = CommunicationLogFileName.appendHttpHeaderSuffix(dir.getAbsolutePath() +
                        File.separator + fileNamePrefix);
                var appInfo = (HttpApplicationInfo) communicationContext.getApplicationInfo();
                try (OutputStream headerFile = new FileOutputStream(headerPath)) {
                    for (Map.Entry<String, String> entry : appInfo.getHeaders().entries()) {
                        String targetString;
                        if (entry.getValue() == null) {
                            targetString = String.format("%s%n", entry.getKey());
                        } else {
                            targetString = String.format("%s = %s%n", entry.getKey(), entry.getValue());
                        }
                        headerFile.write(targetString.getBytes());
                    }
                } catch (IOException e) {
                    instanceLogger.error("Could not write headers to header file {}",
                            CommunicationLogFileName.appendHttpHeaderSuffix(headerPath));
                }
            }

            if (createHttpRequestResponseId) {
                var appInfo = (HttpApplicationInfo) communicationContext.getApplicationInfo();
                var requestResponseIdFilePath = dir.getAbsolutePath() +
                        File.separator + CommunicationLogFileName.append(fileNamePrefix,
                        validFilenameOfTransactionId(appInfo.getTransactionId()));

                try {
                    if (!new File(requestResponseIdFilePath).createNewFile()) {
                        instanceLogger.warn("File {} could not be created as it was existing already",
                                requestResponseIdFilePath);
                    }
                } catch (IOException e) {
                    instanceLogger.error("Could not write headers to header file {}",
                            requestResponseIdFilePath);
                }
            }
        }

        return new CommunicationLogFileOutputStream(dir, fileNamePrefix, prettyPrintXml);
    }

    private String validFilenameOfTransactionId(String transactionId) {
        return transactionId.replace(':', '_');
    }
}
