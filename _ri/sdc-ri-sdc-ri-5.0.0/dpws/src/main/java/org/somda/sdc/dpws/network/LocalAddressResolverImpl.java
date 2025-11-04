package org.somda.sdc.dpws.network;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;

import java.net.Socket;
import java.net.URI;
import java.util.Optional;

/**
 * Default implementation of {@linkplain LocalAddressResolver}.
 */
public class LocalAddressResolverImpl implements LocalAddressResolver {
    private static final Logger LOG = LogManager.getLogger(LocalAddressResolverImpl.class);
    private final Logger instanceLogger;

    @Inject
    LocalAddressResolverImpl(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
    }

    @Override
    public Optional<String> getLocalAddress(String remoteUri) {
        var parsedUri = URI.create(remoteUri);
        try (Socket socket = new Socket(parsedUri.getHost(), parsedUri.getPort())) {
            return Optional.of(socket.getLocalAddress().getHostAddress());
            // CHECKSTYLE.OFF: IllegalCatch
        } catch (Exception e) {
            // CHECKSTYLE.ON: IllegalCatch
            instanceLogger.info("Could not access remote URI {} and resolve local address. Reason: {}", remoteUri,
                    e.getMessage());
            return Optional.empty();
        }

    }
}
