package org.somda.sdc.dpws.http.helper;

/**
 * Indicates that the self test between HTTP server and client failed.
 */
public class ServerClientSelfTestException extends RuntimeException {
    public ServerClientSelfTestException(String message) {
        super(message);
    }

    public ServerClientSelfTestException(String message, Throwable cause) {
        super(message, cause);
    }
}
