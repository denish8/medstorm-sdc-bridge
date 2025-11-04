package org.somda.sdc.dpws.http;

/**
 * Exception to convey HTTP status codes.
 */
public class HttpException extends Exception {
    private final int statusCode;

    /**
     * Creates an exception with an HTTP status code and empty message.
     * <p>
     * <em>Attention: the status code is not verified.</em>
     *
     * @param statusCode the status code to set.
     */
    public HttpException(int statusCode) {
        super("");
        this.statusCode = statusCode;
    }

    /**
     * Creates an exception with an HTTP status code and a message.
     * <p>
     * <em>The status code is not verified!</em>
     *
     * @param statusCode the status code to set.
     * @param message    the message that can be passed to an HTTP content body.
     */
    public HttpException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
