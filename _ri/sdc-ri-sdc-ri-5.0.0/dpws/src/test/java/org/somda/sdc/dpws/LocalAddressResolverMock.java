package org.somda.sdc.dpws;

import org.somda.sdc.dpws.network.LocalAddressResolver;

import java.net.URI;
import java.util.Optional;

public class LocalAddressResolverMock implements LocalAddressResolver {
    @Override
    public Optional<String> getLocalAddress(String remoteUri) {
        return Optional.of(URI.create(remoteUri).getHost() + "-local");
    }
}
