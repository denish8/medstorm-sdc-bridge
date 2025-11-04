package org.somda.sdc.biceps.provider.preprocessing;

/**
 * Exception that is thrown if the cardinality rules of BICEPS are violated during preprocessing.
 */
public class CardinalityException extends Exception {
    public CardinalityException() {
    }

    public CardinalityException(String message) {
        super(message);
    }

    public CardinalityException(String message, Throwable cause) {
        super(message, cause);
    }

    public CardinalityException(Throwable cause) {
        super(cause);
    }

    public CardinalityException(
            String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace
    ) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
