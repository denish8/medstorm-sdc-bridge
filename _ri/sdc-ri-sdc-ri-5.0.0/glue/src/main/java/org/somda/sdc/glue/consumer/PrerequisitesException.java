package org.somda.sdc.glue.consumer;

/**
 * Any exception that comes up before the connection process to a remote device could be finished.
 */
public class PrerequisitesException extends Exception {
    public PrerequisitesException() {
    }

    public PrerequisitesException(String message) {
        super(message);
    }

    public PrerequisitesException(String message, Throwable cause) {
        super(message, cause);
    }

    public PrerequisitesException(Throwable cause) {
        super(cause);
    }

    public PrerequisitesException(String message, Throwable cause,
                                  boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
