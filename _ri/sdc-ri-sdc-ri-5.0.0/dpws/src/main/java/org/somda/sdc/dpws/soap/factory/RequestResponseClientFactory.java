package org.somda.sdc.dpws.soap.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.dpws.soap.interception.RequestResponseCallback;

/**
 * Factory to create request-response clients.
 */
public interface RequestResponseClientFactory {
    /**
     * @param callback callback that is supposed to be invoked after all interceptors on a request-direction are visited
     *                 and the request is ready to be conveyed over the network.
     * @return a new {@link RequestResponseClient}.
     */
    RequestResponseClient createRequestResponseClient(@Assisted RequestResponseCallback callback);
}
