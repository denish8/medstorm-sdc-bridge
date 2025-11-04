package org.somda.sdc.biceps.provider.preprocessing;

/**
 * Exception that is thrown during preprocessing of {@linkplain TypeConsistencyChecker}.
 */
public class TypeConsistencyException extends Exception{
    public TypeConsistencyException() {
    }

    public TypeConsistencyException(String message) {
        super(message);
    }

    public TypeConsistencyException(String message, Throwable cause) {
        super(message, cause);
    }

    public TypeConsistencyException(Throwable cause) {
        super(cause);
    }

    public TypeConsistencyException(
            String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace
    ) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
