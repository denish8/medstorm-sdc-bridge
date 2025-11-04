package it.org.somda.sdc.dpws.soap;

import it.org.somda.sdc.dpws.SslMetadata;
import org.somda.sdc.dpws.crypto.CryptoSettings;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Optional;

/**
 * Simple {@link CryptoSettings} generator for integration tests using HTTPS.
 */
public class Ssl {
    private static final SslMetadata SSL_METADATA = new SslMetadata();
    static {
        SSL_METADATA.startAsync().awaitRunning();
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        //System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
    }

    public static CryptoSettings setupServer() {
        return setup(SSL_METADATA.getServerKeySet());
    }

    public static CryptoSettings setupClient() {
        return setup(SSL_METADATA.getClientKeySet());
    }

    public static X509Certificate getServerCertificate() {
        return SSL_METADATA.getServerKeySet().getCertificate();
    }

    public static X509Certificate getClientCertificate() {
        return SSL_METADATA.getClientKeySet().getCertificate();
    }

    private static CryptoSettings setup(SslMetadata.KeySet keySet) {
        byte[] keyStoreBytes = null;
        byte[] trustStoreBytes = null;
        try {
            keyStoreBytes = convertToByteArray(keySet.getKeyStore(), keySet.getKeyStorePassword());
            trustStoreBytes = convertToByteArray(keySet.getTrustStore(), keySet.getTrustStorePassword());
        } catch (Exception e) {
            e.printStackTrace();
        }

        final byte[] finalKeyStoreBytes = keyStoreBytes;
        final byte[] finalTrustStoreBytes = trustStoreBytes;

        return new CryptoSettings() {
            @Override
            public Optional<InputStream> getKeyStoreStream() {
                if(finalKeyStoreBytes == null) {
                    return Optional.empty();
                }
                return Optional.of(new ByteArrayInputStream(finalKeyStoreBytes));
            }

            @Override
            public String getKeyStorePassword() {
                return keySet.getKeyStorePassword();
            }

            @Override
            public Optional<InputStream> getTrustStoreStream() {
                if(finalTrustStoreBytes == null) {
                    return Optional.empty();
                }
                return Optional.of(new ByteArrayInputStream(finalTrustStoreBytes));
            }

            @Override
            public String getTrustStorePassword() {
                return keySet.getTrustStorePassword();
            }
        };

        // Info: properties to set for global SSL configuration
        // Not recommended as client and device config may differ, but HTTP server and client access the same
        // system properties.
        // System.setProperty(SslConfigurator.TRUST_STORE_FILE, sslMetadata.getTrustStoreFile());
        // System.setProperty(SslConfigurator.KEY_STORE_FILE, sslMetadata.getKeyStoreFile());
        // System.setProperty(SslConfigurator.TRUST_STORE_PASSWORD, sslMetadata.getTrustStorePassword());
        // System.setProperty(SslConfigurator.KEY_STORE_PASSWORD, sslMetadata.getKeyStorePassword());
    }

    private static byte[] convertToByteArray(KeyStore keyStore, String password)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        keyStore.store(bos, password.toCharArray());
        return bos.toByteArray();
    }
}
