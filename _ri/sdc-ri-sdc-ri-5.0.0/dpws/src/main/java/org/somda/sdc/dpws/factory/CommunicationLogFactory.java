package org.somda.sdc.dpws.factory;

import org.somda.sdc.dpws.CommunicationLog;

/**
 * Factory to create {@linkplain CommunicationLog} instances.
 */
public interface CommunicationLogFactory {
    /**
     * Creates a {@linkplain CommunicationLog} instance receiving with no additional context info.
     *
     * @return a new {@link CommunicationLog} instance.
     */
    CommunicationLog createCommunicationLog();
}
