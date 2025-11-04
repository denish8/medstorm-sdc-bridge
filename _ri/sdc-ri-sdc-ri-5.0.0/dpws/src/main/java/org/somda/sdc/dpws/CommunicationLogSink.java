package org.somda.sdc.dpws;

import org.somda.sdc.dpws.soap.CommunicationContext;

import java.io.OutputStream;

/**
 * Communication log sink interface.
 */
public interface CommunicationLogSink {
    /**
     * Creates a branch based on the given key and path.
     *
     * @param path                 path to save the key to branch mapping in.
     * @param direction            direction of message to store.
     * @param messageType          the type of the message i.e. request, response.
     * @param communicationContext context which can be used to derive storage information.
     * @return an {@linkplain OutputStream}, that represents the branch to write to.
     */
    OutputStream createTargetStream(CommunicationLog.TransportType path,
                                    CommunicationLog.Direction direction,
                                    CommunicationLog.MessageType messageType,
                                    CommunicationContext communicationContext);
}
