package org.somda.sdc.dpws.soap.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.soap.NotificationSource;
import org.somda.sdc.dpws.soap.interception.NotificationCallback;

/**
 * Factory to create notification sources.
 */
public interface NotificationSourceFactory {
    /**
     * Creates a notification source.
     *
     * @param callback a callback that is supposed to be invoked after all interceptors are visited and
     *                 the notification is ready to be transmitted to an event sink.
     * @return a new {@link NotificationSource}.
     */
    NotificationSource createNotificationSource(@Assisted NotificationCallback callback);
}
