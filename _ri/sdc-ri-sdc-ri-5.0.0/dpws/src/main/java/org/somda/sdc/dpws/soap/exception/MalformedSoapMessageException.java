package org.somda.sdc.dpws.soap.exception;

/**
 * Runtime exception to express that a SOAP message is malformed in some way.
 */
public class MalformedSoapMessageException extends RuntimeException {
    public MalformedSoapMessageException() {
    }

    public MalformedSoapMessageException(String message) {
        super(message);
    }

    public MalformedSoapMessageException(String message, Throwable cause) {
        super(message, cause);
    }

    public MalformedSoapMessageException(Throwable cause) {
        super(cause);
    }

    public MalformedSoapMessageException(String message, Throwable cause, boolean enableSuppression,
                                         boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
