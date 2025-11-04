package org.somda.sdc.dpws;

/**
 * Exception that comes up while a transport binding processes a network request.
 */
public class TransportBindingException extends RuntimeException {
    public TransportBindingException() {
    }

    public TransportBindingException(String message) {
        super(message);
    }

    public TransportBindingException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransportBindingException(Throwable cause) {
        super(cause);
    }

    public TransportBindingException(String message, Throwable cause,
                                     boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
