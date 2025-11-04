package org.somda.sdc.dpws.crypto;

import java.io.InputStream;
import java.util.Optional;

/**
 * Common interface to retrieve key and trust store information.
 * <p>
 * Key and trust store information can be referenced using streams ({@link #getKeyStoreStream()},
 * {@link #getKeyStoreStream()}).
 *
 * @see <a href="https://www.cloudera.com/documentation/enterprise/5-10-x/topics/cm_sg_create_key_trust.html"
 * >More information on key and trust stores</a>
 */
public interface CryptoSettings {
    Optional<InputStream> getKeyStoreStream();

    String getKeyStorePassword();

    Optional<InputStream> getTrustStoreStream();

    String getTrustStorePassword();
}
