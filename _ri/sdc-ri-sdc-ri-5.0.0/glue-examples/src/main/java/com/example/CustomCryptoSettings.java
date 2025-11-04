package com.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.somda.sdc.dpws.crypto.CachingCryptoSettings;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Objects;
import java.util.Optional;

public class CustomCryptoSettings implements CachingCryptoSettings {
    private static final Logger LOG = LogManager.getLogger(CustomCryptoSettings.class);

    private static final String DEFAULT_KEYSTORE = "crypto/sdcparticipant.jks";
    private static final String DEFAULT_TRUSTSTORE = "crypto/root.jks";
    private static final String DEFAULT_KEYSTORE_PASSWORD = "whatever";
    private static final String DEFAULT_TRUSTSTORE_PASSWORD = "whatever";

    private byte[] keyStore = null;
    private byte[] trustStore = null;
    private String keyStorePassword = null;
    private String trustStorePassword = null;

    private @Nullable SSLContext cachedContext = null;

    public CustomCryptoSettings(
            byte[] keyStore,
            byte[] trustStore,
            String keyStorePassword,
            String trustStorePassword
    ) {
        this.keyStore = keyStore;
        this.trustStore = trustStore;
        this.keyStorePassword = keyStorePassword;
        this.trustStorePassword = trustStorePassword;
    }

    public CustomCryptoSettings() {
    }

    public static CustomCryptoSettings fromKeyStore(
            String keyStorePath,
            String trustStorePath,
            String keyStorePassword,
            String trustStorePassword
    ) {
        byte[] keyStoreFile;
        byte[] trustStoreFile;
        try {
            keyStoreFile = Files.readAllBytes(Path.of(keyStorePath));
            trustStoreFile = Files.readAllBytes(Path.of(trustStorePath));
        } catch (IOException e) {
            LOG.error("Specified store file could not be found", e);
            throw new RuntimeException("Specified store file could not be found", e);
        }

        return new CustomCryptoSettings(keyStoreFile, trustStoreFile, keyStorePassword, trustStorePassword);
    }

    public static CustomCryptoSettings fromKeyFile(
            String userKeyFilePath,
            String userCertFilePath,
            String caCertFilePath,
            String userKeyPassword
    ) {
        Security.addProvider(new BouncyCastleProvider());

        byte[] userKeyFile;
        byte[] userCertFile;
        byte[] caCertFile;
        try {
            userKeyFile = Files.readAllBytes(Path.of(userKeyFilePath));
            userCertFile = Files.readAllBytes(Path.of(userCertFilePath));
            caCertFile = Files.readAllBytes(Path.of(caCertFilePath));
        } catch (IOException e) {
            LOG.error("Specified certificate file could not be found", e);
            throw new RuntimeException("Specified certificate file could not be found", e);
        }

        PrivateKey userKey;
        Certificate userCert;
        Certificate caCert;

        try {
            var cf = CertificateFactory.getInstance("X.509");

            // private key
            userKey = getPrivateKey(userKeyFile, userKeyPassword);

            // public key
            userCert = cf.generateCertificate(new ByteArrayInputStream(userCertFile));

            // ca cert
            caCert = cf.generateCertificate(new ByteArrayInputStream(caCertFile));
        } catch (CertificateException | IOException e) {
            LOG.error("Specified certificate file could not be loaded", e);
            throw new RuntimeException("Specified certificate file could not be loaded", e);
        }

        KeyStore keyStore;
        KeyStore trustStore;
        try {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);
            trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null);
        } catch (CertificateException | NoSuchAlgorithmException | IOException | KeyStoreException e) {
            LOG.error("Error creating keystore instance", e);
            throw new RuntimeException("Error creating keystore instance", e);
        }

        try {
            keyStore.setKeyEntry("key", userKey, userKeyPassword.toCharArray(), new Certificate[]{userCert});
            trustStore.setCertificateEntry("ca", caCert);
        } catch (KeyStoreException e) {
            LOG.error("Error loading certificate into keystore instance", e);
            throw new RuntimeException("Error loading certificate into keystore instance", e);
        }

        var keyStoreOutputStream = new ByteArrayOutputStream();
        var trustStoreOutputStream = new ByteArrayOutputStream();

        try {
            keyStore.store(keyStoreOutputStream, userKeyPassword.toCharArray());
            trustStore.store(trustStoreOutputStream, userKeyPassword.toCharArray());
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            LOG.error("Error converting keystore to stream", e);
            throw new RuntimeException("Error converting keystore to stream", e);
        }

        return new CustomCryptoSettings(keyStoreOutputStream.toByteArray(), trustStoreOutputStream.toByteArray(), userKeyPassword, userKeyPassword);

    }

    private static PrivateKey getPrivateKey(byte[] key, String password) throws IOException {

        PEMParser pp = new PEMParser(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(key))));
        var pemKey = (PKCS8EncryptedPrivateKeyInfo) pp.readObject();
        pp.close();

        InputDecryptorProvider pkcs8Prov;
        try {
            pkcs8Prov = new JceOpenSSLPKCS8DecryptorProviderBuilder().build(password.toCharArray());
        } catch (OperatorCreationException e) {
            throw new IOException(e);
        }
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(new BouncyCastleProvider());

        PrivateKeyInfo decrypted;
        try {
            decrypted = pemKey.decryptPrivateKeyInfo(pkcs8Prov);
        } catch (PKCSException e) {
            throw new IOException(e);
        }
        return converter.getPrivateKey(decrypted);
    }

    @Override
    public Optional<InputStream> getKeyStoreStream() {
        if (keyStore != null) {
            return Optional.of(new ByteArrayInputStream(keyStore));
        }
        return Optional.ofNullable(getClass().getClassLoader().getResourceAsStream(DEFAULT_KEYSTORE));
    }

    @Override
    public String getKeyStorePassword() {
        return Objects.requireNonNullElse(this.keyStorePassword, DEFAULT_KEYSTORE_PASSWORD);
    }

    @Override
    public Optional<InputStream> getTrustStoreStream() {
        if (trustStore != null) {
            return Optional.of(new ByteArrayInputStream(trustStore));
        }
        return Optional.ofNullable(getClass().getClassLoader().getResourceAsStream(DEFAULT_TRUSTSTORE));
    }

    @Override
    public String getTrustStorePassword() {
        return Objects.requireNonNullElse(trustStorePassword, DEFAULT_TRUSTSTORE_PASSWORD);
    }

    @Override
    public synchronized Optional<SSLContext> getSslContext() {
        return Optional.ofNullable(cachedContext);
    }

    @Override
    public synchronized void setSslContext(final SSLContext sslContext) {
        cachedContext = sslContext;
    }
}
