package org.somda.sdc.dpws.crypto;

import javax.net.ssl.SSLContext;
import java.util.Optional;

/**
 * Extends the default {@link CryptoSettings} with a cache.
 * <p>
 * Note: Remember invalidating the cache if the content of the {@link CryptoSettings} changes!
 */
public interface CachingCryptoSettings extends CryptoSettings {

    /**
     * Retrieves an already created SSL Context, or an empty optional if none exists.
     * @return an SSL Context or none if not cached
     */
    Optional<SSLContext> getSslContext();


    /**
     * Sets an {@link SSLContext} as cached result for the crypto settings.
     * @param sslContext to cache
     */
    void setSslContext(SSLContext sslContext);
}
