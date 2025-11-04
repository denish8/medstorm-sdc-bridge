package org.somda.sdc.glue.consumer.sco;

/**
 * Error that can occur during SCO processing.
 */
public class InvocationException extends Exception {
    public InvocationException() {
    }

    public InvocationException(String message) {
        super(message);
    }

    public InvocationException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvocationException(Throwable cause) {
        super(cause);
    }

    public InvocationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
