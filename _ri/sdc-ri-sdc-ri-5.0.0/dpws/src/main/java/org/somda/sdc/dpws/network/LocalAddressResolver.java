package org.somda.sdc.dpws.network;

import java.util.Optional;

/**
 * Tool to resolve a local address from a remote address.
 */
public interface LocalAddressResolver {

    /**
     * Resolves a local address from a remote URI.
     *
     * @param remoteUri the remote URI to test against.
     * @return the resolved address or {@linkplain Optional#empty()} if resolving failed.
     */
    Optional<String> getLocalAddress(String remoteUri);
}
