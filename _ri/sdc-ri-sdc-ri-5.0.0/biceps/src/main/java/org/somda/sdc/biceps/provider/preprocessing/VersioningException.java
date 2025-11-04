package org.somda.sdc.biceps.provider.preprocessing;

/**
 * Exception that is thrown if the versioning rules of BICEPS could not be applied during preprocessing.
 */
public class VersioningException extends Exception {
    public VersioningException() {
    }

    public VersioningException(String message) {
        super(message);
    }

    public VersioningException(String message, Throwable cause) {
        super(message, cause);
    }

    public VersioningException(Throwable cause) {
        super(cause);
    }

    public VersioningException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
