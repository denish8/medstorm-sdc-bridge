package org.somda.sdc.dpws.client.exception;

/**
 * Exception that is thrown if the EPR address from discovery differs from the EPR address from WS-TransferGet response.
 */
public class EprAddressMismatchException extends RuntimeException {
    public EprAddressMismatchException() {
    }

    public EprAddressMismatchException(String message) {
        super(message);
    }
}
