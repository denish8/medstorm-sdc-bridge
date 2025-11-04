package org.somda.sdc.dpws.soap.wseventing.model;

import org.somda.sdc.dpws.soap.SoapMessage;

import java.time.LocalDateTime;

/**
 * Notification queue item that provides a construction time to allow detection of stale notifications.
 *
 * @see #getConstructionTime()
 */
public class Notification {
    private final SoapMessage payload;
    private final LocalDateTime constructionTime;

    /**
     * Constructor that accepts a notification and records the time during construction.
     *
     * @param payload the notification message to convey.
     */
    public Notification(SoapMessage payload)
    {
        this.constructionTime = LocalDateTime.now();
        this.payload = payload;
    }

    public SoapMessage getPayload() {
        return payload;
    }

    public LocalDateTime getConstructionTime() {
        return constructionTime;
    }
}
