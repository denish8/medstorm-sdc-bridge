package org.somda.sdc.dpws.soap;

import org.somda.sdc.dpws.CommunicationLogContext;

import javax.annotation.Nullable;

/**
 * Utility class to wrap application and transport layer information.
 */
public class CommunicationContext {

    private final ApplicationInfo applicationInfo;
    private final TransportInfo transportInfo;
    @Nullable
    private final CommunicationLogContext communicationLogContext;

    public CommunicationContext(
            ApplicationInfo applicationInfo,
            TransportInfo transportInfo,
            @Nullable CommunicationLogContext communicationLogContext
    ) {
        this.applicationInfo = applicationInfo;
        this.transportInfo = transportInfo;
        this.communicationLogContext = communicationLogContext;
    }

    public ApplicationInfo getApplicationInfo() {
        return applicationInfo;
    }

    public TransportInfo getTransportInfo() {
        return transportInfo;
    }

    @Nullable
    public CommunicationLogContext getCommunicationLogContext() {
        return communicationLogContext;
    }
}
