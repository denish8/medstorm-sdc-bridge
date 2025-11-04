package org.somda.sdc.dpws.soap.wseventing.exception;

/**
 * Indicates that a subscription was not found in, either on source or sink side.
 */
public class SubscriptionNotFoundException extends RuntimeException {
    public SubscriptionNotFoundException() {
    }

    public SubscriptionNotFoundException(String message) {
        super(message);
    }

    public SubscriptionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public SubscriptionNotFoundException(Throwable cause) {
        super(cause);
    }

    public SubscriptionNotFoundException(String message,
                                         Throwable cause,
                                         boolean enableSuppression,
                                         boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
