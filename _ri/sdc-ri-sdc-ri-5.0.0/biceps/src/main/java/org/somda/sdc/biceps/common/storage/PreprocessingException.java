package org.somda.sdc.biceps.common.storage;

/**
 * An exception that is thrown if a preprocessing error occurs.
 */
public class PreprocessingException extends Exception {
    private final String handle;
    private final String segment;

    public PreprocessingException(String message, Throwable cause, String handle, String segment) {
        super(message, cause);
        this.handle = handle;
        this.segment = segment;
    }

    public PreprocessingException(String message, String handle, String segment) {
        super(message);
        this.handle = handle;
        this.segment = segment;
    }

    public String getHandle() {
        return handle;
    }

    public String getSegment() {
        return segment;
    }
}
