package org.somda.sdc.dpws.soap.wseventing.exception;

/**
 * Indicates that the Http Client for a certain subscription was not found.
 */
public class SubscriptionRequestResponseClientNotFoundException extends RuntimeException {
    public SubscriptionRequestResponseClientNotFoundException() {
    }

    public SubscriptionRequestResponseClientNotFoundException(String message) {
        super(message);
    }

    public SubscriptionRequestResponseClientNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public SubscriptionRequestResponseClientNotFoundException(Throwable cause) {
        super(cause);
    }

    public SubscriptionRequestResponseClientNotFoundException(String message,
                                                              Throwable cause,
                                                              boolean enableSuppression,
                                                              boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
