package org.somda.sdc.dpws.soap.interception;

/**
 * Interface to register or update interceptor objects for message interception chains.
 *
 * @see MessageInterceptor
 */
public interface InterceptorHandler {
    /**
     * Registers a callback object.
     *
     * @param interceptor an object that possesses {@link MessageInterceptor} annotated methods.
     * @see MessageInterceptor
     */
    void register(Interceptor interceptor);
}
