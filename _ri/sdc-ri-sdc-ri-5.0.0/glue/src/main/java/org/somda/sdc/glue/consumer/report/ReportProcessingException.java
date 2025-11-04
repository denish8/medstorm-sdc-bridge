package org.somda.sdc.glue.consumer.report;

/**
 * An exception that is thrown if something during report processing fails.
 */
public class ReportProcessingException extends Exception {
    public ReportProcessingException() {
    }

    public ReportProcessingException(String message) {
        super(message);
    }

    public ReportProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReportProcessingException(Throwable cause) {
        super(cause);
    }

    public ReportProcessingException(String message, Throwable cause,
                                     boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
