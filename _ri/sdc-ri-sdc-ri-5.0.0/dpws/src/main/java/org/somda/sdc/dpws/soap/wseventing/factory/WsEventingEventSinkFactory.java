package org.somda.sdc.dpws.soap.wseventing.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.CommunicationLogContext;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.dpws.soap.wseventing.EventSink;

import javax.annotation.Nullable;

/**
 * Creates {@link EventSink} instances.
 */
public interface WsEventingEventSinkFactory {
    /**
     * Creates a new WS-Eventing event sink.
     *
     * @param requestResponseClient request response client where to send requests to (subscribe, renew, ...).
     * @param hostAddress           address where to bind a notification sink server.
     * @param communicationLogContext      TODO
     * @return a new {@link EventSink} instance.
     */
    EventSink createWsEventingEventSink(@Assisted RequestResponseClient requestResponseClient,
                                        @Assisted String hostAddress,
                                        @Assisted @Nullable CommunicationLogContext communicationLogContext);
}
