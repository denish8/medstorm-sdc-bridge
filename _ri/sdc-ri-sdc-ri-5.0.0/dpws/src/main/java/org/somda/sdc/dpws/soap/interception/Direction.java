package org.somda.sdc.dpws.soap.interception;

/**
 * Defines a communication direction.
 */
public enum Direction {
    /**
     * The message exchange was requested and the message flow is directed to the request sink.
     */
    REQUEST,

    /**
     * The request sink has been processed and the message flow is directed to the request source.
     */
    RESPONSE,

    /**
     * Notification that is on the way to its sink.
     */
    NOTIFICATION,

    /**
     * Any type of message and direction.
     */
    ANY
}
