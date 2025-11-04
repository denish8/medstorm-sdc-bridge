package org.somda.sdc.dpws.factory;

import org.somda.sdc.dpws.CommunicationLogContext;
import org.somda.sdc.dpws.TransportBinding;

import javax.annotation.Nullable;

/**
 * Factory to create {@linkplain TransportBinding} instances.
 * <p>
 * {@link TransportBinding} instances can be used to request any DPWS compatible endpoints.
 */
public interface TransportBindingFactory {
    /**
     * Generic factory method to create a binding on the basis of a URI.
     * <p>
     * Please note that - depending on the implementation - bindings are not supported. In this case an
     * {@link UnsupportedOperationException} is thrown.
     *
     * @param endpointUri      the URI to create a binding to.
     * @param communicationLogContext additional information made available in the communication log
     * @return a transport binding bound to endpointUri.
     * @throws UnsupportedOperationException if the URI scheme is not supported.
     */
    TransportBinding createTransportBinding(String endpointUri,
                                            @Nullable CommunicationLogContext communicationLogContext)
            throws UnsupportedOperationException;

    /**
     * Creates an HTTP or HTTPS binding.
     *
     * @param endpointUri      a valid HTTP/HTTPS URI to create a binding to.
     * @param communicationLogContext additional information made available in the communication log
     * @return a transport binding bound to endpointUri.
     * @throws UnsupportedOperationException if the URI scheme type is not supported.
     */
    TransportBinding createHttpBinding(String endpointUri,
                                       @Nullable CommunicationLogContext communicationLogContext)
            throws UnsupportedOperationException;
}
