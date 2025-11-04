package org.somda.sdc.dpws.soap.factory;

import org.somda.sdc.dpws.soap.NotificationSink;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingServerInterceptor;

/**
 * Factory to create notification sinks.
 */
public interface NotificationSinkFactory {
    /**
     * Creates a notification sink.
     *
     * @param wsAddressingServerInterceptor the server interceptor to use for action processing and message duplication
     *                                      detection.
     * @return a new {@link NotificationSink}.
     */
    NotificationSink createNotificationSink(WsAddressingServerInterceptor wsAddressingServerInterceptor);
}
