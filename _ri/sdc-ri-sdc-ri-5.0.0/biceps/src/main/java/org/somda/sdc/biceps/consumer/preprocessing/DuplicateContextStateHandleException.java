package org.somda.sdc.biceps.consumer.preprocessing;

/**
 * Exception that is thrown during preprocessing, if duplicate context state handles exists.
 */
public class DuplicateContextStateHandleException extends Exception {

    public DuplicateContextStateHandleException() {
    }

    public DuplicateContextStateHandleException(String message) {
        super(message);
    }

    public DuplicateContextStateHandleException(String message, Throwable cause) {
        super(message, cause);
    }

    public DuplicateContextStateHandleException(Throwable cause) {
        super(cause);
    }

    public DuplicateContextStateHandleException(
            String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace
    ) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
