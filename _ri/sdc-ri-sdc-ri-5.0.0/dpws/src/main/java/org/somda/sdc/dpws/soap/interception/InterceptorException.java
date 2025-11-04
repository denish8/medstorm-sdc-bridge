package org.somda.sdc.dpws.soap.interception;

/**
 * Exception that comes up during SOAP message interceptor processing.
 */
public class InterceptorException extends Exception {
    private final Interceptor interceptor;

    public InterceptorException(String message, Interceptor interceptor) {
        super(message);
        this.interceptor = interceptor;
    }

    public InterceptorException(String message, Interceptor interceptor, Throwable cause) {
        super(message, cause);
        this.interceptor = interceptor;
    }

    /**
     * Gets the interceptor where the error was raised.
     *
     * @return the exception source.
     */
    public Interceptor getInterceptor() {
        return interceptor;
    }
}
