package org.somda.sdc.biceps.provider.preprocessing;

/**
 * Exception that can be thrown if a handle is duplicated during preprocessing.
 */
public class HandleDuplicatedException extends Exception {

    public HandleDuplicatedException() {
    }

    public HandleDuplicatedException(String message) {
        super(message);
    }

    public HandleDuplicatedException(String message, Throwable cause) {
        super(message, cause);
    }

    public HandleDuplicatedException(Throwable cause) {
        super(cause);
    }

    public HandleDuplicatedException(
            String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace
    ) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
