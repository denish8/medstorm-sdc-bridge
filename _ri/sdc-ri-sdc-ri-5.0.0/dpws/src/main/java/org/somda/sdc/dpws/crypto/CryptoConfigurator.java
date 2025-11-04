package org.somda.sdc.dpws.crypto;

import com.google.inject.Inject;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Supports generation of server and client SSL configurations.
 * <p>
 * Can either generate default configurations or derive configurations based on {@link CryptoSettings} objects.
 */
public class CryptoConfigurator {
    private static final Logger LOG = LogManager.getLogger(CryptoConfigurator.class);

    @Inject
    CryptoConfigurator() {
    }

    /**
     * Accepts a {@link CachingCryptoSettings} object and creates an {@linkplain SSLContext} object or retrieves
     * it from the cache.
     * <p>
     *
     * @param cryptoSettings the crypto settings.
     *
     * @return an SSlContext matching the given crypto settings.
     */
    public SSLContext createSslContextFromCryptoConfig(CachingCryptoSettings cryptoSettings)
        throws KeyStoreException, UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException,
        IOException, KeyManagementException {
        var contextOpt = cryptoSettings.getSslContext();
        if (contextOpt.isPresent()) {
            LOG.debug("Retrieved cached SSLContext");
            return contextOpt.orElseThrow();
        } else {
            LOG.debug("Creating new SSLContext");
            var context = createSslContextFromCryptoConfigInternal(cryptoSettings);
            cryptoSettings.setSslContext(context);
            return context;
        }
    }


    /**
     * Accepts a {@link CryptoSettings} object and creates an {@linkplain SSLContext} object.
     * <p>
     *
     * @param cryptoSettings the crypto settings.
     *
     * @return an SSlContext matching the given crypto settings.
     */
    public SSLContext createSslContextFromCryptoConfig(CryptoSettings cryptoSettings)
            throws KeyStoreException, UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException,
            IOException, KeyManagementException {

        if (cryptoSettings instanceof CachingCryptoSettings) {
            return createSslContextFromCryptoConfig((CachingCryptoSettings) cryptoSettings);
        }
        return createSslContextFromCryptoConfigInternal(cryptoSettings);

    }

    private SSLContext createSslContextFromCryptoConfigInternal(CryptoSettings cryptoSettings)
        throws KeyStoreException, UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException,
        IOException, KeyManagementException {
        final SSLContextBuilder sslContextBuilder = SSLContexts.custom();

        // key store
        final var keyStoreStreamOpt = cryptoSettings.getKeyStoreStream();
        if (keyStoreStreamOpt.isPresent()) {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(keyStoreStreamOpt.get(), cryptoSettings.getKeyStorePassword().toCharArray());
            sslContextBuilder.loadKeyMaterial(ks, cryptoSettings.getKeyStorePassword().toCharArray());
        } else {
            throw new IOException("Expected key store, but none found");
        }

        // trust store
        final var trustStoreStreamOpt = cryptoSettings.getTrustStoreStream();
        if (trustStoreStreamOpt.isPresent()) {
            KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
            ts.load(trustStoreStreamOpt.get(), cryptoSettings.getTrustStorePassword().toCharArray());
            sslContextBuilder.loadTrustMaterial(ts, null);
        } else {
            throw new IOException("Expected trust store, but none found");
        }

        return sslContextBuilder.build();

    }

    /**
     * Accepts a {@link CryptoSettings} object and extracts all certificates from the keystore.
     * <p>
     *
     * @param cryptoSettings the crypto settings.
     *                       Please note that key store files take precedence over key store streams.
     * @return a list of all X509 certificates from the keystore or an empty list.
     */
    public List<X509Certificate> getCertificates(@Nullable CryptoSettings cryptoSettings) {
        List<X509Certificate> certificates = new ArrayList<>();
        if (cryptoSettings == null) return certificates;
        try {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            final var keyStoreStreamOpt = cryptoSettings.getKeyStoreStream();
            if (keyStoreStreamOpt.isPresent()) {
                ks.load(keyStoreStreamOpt.get(), cryptoSettings.getKeyStorePassword().toCharArray());
            } else {
                return certificates;
            }
            var aliases = ks.aliases().asIterator();
            while (aliases.hasNext()) {
                var cert = ks.getCertificate(aliases.next());
                if (cert instanceof X509Certificate) {
                    certificates.add((X509Certificate) cert);
                }
            }
        } catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException e) {
            LOG.error("Error retrieving certificates from keystore", e);
        }
        return certificates;
    }

    /**
     * Creates a default {@linkplain SSLContext} object based on system properties.
     *
     * @return an SSLContext with default crypto settings.
     */
    public SSLContext createSslContextFromSystemProperties() {
        return SSLContexts.createSystemDefault();
    }

}
