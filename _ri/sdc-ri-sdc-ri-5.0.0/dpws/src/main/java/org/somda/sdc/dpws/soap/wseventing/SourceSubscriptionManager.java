package org.somda.sdc.dpws.soap.wseventing;

import com.google.common.util.concurrent.Service;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.wseventing.model.Notification;
import org.somda.sdc.dpws.soap.wseventing.model.WsEventingStatus;

import java.time.Duration;
import java.util.Optional;

/**
 * Subscription manager interface that is used by event sources.
 */
public interface SourceSubscriptionManager extends SubscriptionManager, Service {

    /**
     * Inserts the notification into the subscription manager's queue.
     * <p>
     * The manager is shut down
     * <ul>
     * <li>on first delivery failure or
     * <li>in case there is queue overflow or a delivery failure.
     * </ul>
     *
     * @param notification the notification to add.
     */
    void offerNotification(Notification notification);

    /**
     * Tries to send a custom end-to message to the event sink.
     * <p>
     * This is a non-blocking call that silently ignores failed delivery.
     * This method ends the {@linkplain SourceSubscriptionManager} once the message has been delivered.
     *
     * @param endToMessage the message to send. This message is supposed to be a valid end-to message.
     */
    void offerEndTo(SoapMessage endToMessage);

    /**
     * Tries to send an end-to message to the event sink of the given status.
     * <p>
     * This is a non-blocking call that silently ignores failed delivery.
     * This method ends the {@linkplain SourceSubscriptionManager} once the status has been delivered.
     *
     * @param status the status for which a SubscriptionEnd message will be sent.
     */
    void offerEndTo(WsEventingStatus status);

    /**
     * Resets the expires duration.
     * <p>
     * This will also affect {@link #getExpiresTimeout()}.
     *
     * @param expires the duration to reset.
     */
    void renew(Duration expires);

    /**
     * Returns a string identifying the certificate of the consumer that started the subscription.
     *
     * @return an identifying string, or empty, if no such string exists.
     */
    Optional<String> getCallerId();
}
