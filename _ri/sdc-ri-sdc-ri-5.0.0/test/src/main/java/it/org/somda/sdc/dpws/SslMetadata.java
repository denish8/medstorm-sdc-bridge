package it.org.somda.sdc.dpws;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;

import javax.annotation.Nullable;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * SSL metadata used for crypto related tests.
 * <p>
 * Provides key stores and trust stores for client and device side in-memory.
 * <p>
 * Code derived from http://www.bouncycastle.org/documentation.html and https://www.baeldung.com/java-keystore
 */
public class SslMetadata extends AbstractIdleService implements Service {
    @Nullable
    private KeySet serverKeySet;
    @Nullable
    private KeySet clientKeySet;

    public SslMetadata() {
        serverKeySet = null;
        clientKeySet = null;
    }

    @Override
    protected void startUp() throws NoSuchAlgorithmException, CertificateException, KeyStoreException,
            IOException, OperatorCreationException {

        Security.addProvider(new BouncyCastleProvider());

        final String commonPassword = "secret";

        final KeyPurposeId[] ekuId = {KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth};
        final ExtendedKeyUsage extendedKeyUsage = new ExtendedKeyUsage(ekuId);

        final String serverAlias = "test-device";
        final String clientAlias = "test-client";

        final KeyPair serverKeyPair = generateKeyPair();
        final KeyPair clientKeyPair = generateKeyPair();

        final X509Certificate serverCert = generateCertificate("sdc-lite-server.org", serverKeyPair, extendedKeyUsage);
        final X509Certificate clientCert = generateCertificate("sdc-lite-client.org", clientKeyPair, extendedKeyUsage);

        final KeyStore serverKeyStore = createKeyStore(serverAlias, serverKeyPair.getPrivate(),
                commonPassword, Collections.singletonList(serverCert));
        final KeyStore clientKeyStore = createKeyStore(clientAlias, clientKeyPair.getPrivate(),
                commonPassword, Collections.singletonList(clientCert));

        Map<String, X509Certificate> certsMap = Map.of(serverAlias, serverCert, clientAlias, clientCert);
        // use one trust store with trusted server & client certificates, otherwise HTTP connection self-test fails
        final KeyStore trustStore = createTrustStore(certsMap, commonPassword);

        serverKeySet = new KeySet(serverKeyStore, commonPassword, trustStore, commonPassword, serverCert);
        clientKeySet = new KeySet(clientKeyStore, commonPassword, trustStore, commonPassword, clientCert);
    }

    @Override
    protected void shutDown() {

    }

    public KeySet getServerKeySet() {
        return serverKeySet;
    }

    public KeySet getClientKeySet() {
        return clientKeySet;
    }

    private static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    private static KeyStore createKeyStore(String alias,
                                           PrivateKey privateKey,
                                           String password,
                                           List<X509Certificate> certificateChain)
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

        final KeyStore keyStore = KeyStore.getInstance("jks");
        keyStore.load(null, password.toCharArray());

        final X509Certificate[] certificates = certificateChain.toArray(new X509Certificate[0]);
        keyStore.setKeyEntry(alias, privateKey, password.toCharArray(), certificates);
        return keyStore;
    }

    private static KeyStore createTrustStore(Map<String, X509Certificate> certificateMap, String password)
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

        final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, password.toCharArray());

        for (Map.Entry<String, X509Certificate> entry : certificateMap.entrySet()) {
            String alias = entry.getKey();
            X509Certificate cert = entry.getValue();
            keyStore.setCertificateEntry(alias, cert);
        }

        return keyStore;
    }

    private static X509Certificate generateCertificate(
            String issuer, KeyPair keyPair, ExtendedKeyUsage extendedKeyUsage)
            throws IOException, OperatorCreationException, CertificateException {
        SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
        AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256WithRSAEncryption");
        AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
        AsymmetricKeyParameter privateKeyAsymKeyParam = PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded());
        ContentSigner sigGen = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(privateKeyAsymKeyParam);

        // generate the certificate
        X509v3CertificateBuilder certGen = new X509v3CertificateBuilder(
                new X500Name("CN=" + issuer),
                BigInteger.valueOf(System.currentTimeMillis()),
                new Date(System.currentTimeMillis() - 500000),
                new Date(System.currentTimeMillis() + 500000),
                new X500Name("CN=" + issuer),
                subPubKeyInfo
        );

        certGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        certGen.addExtension(
                Extension.keyUsage,
                true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment)
        );
        certGen.addExtension(
                Extension.subjectAlternativeName,
                false,
                new GeneralNames(
                        new GeneralName(GeneralName.rfc822Name, "david.gregorczyk@web.de")
                )
        );
        certGen.addExtension(Extension.extendedKeyUsage, true, extendedKeyUsage);

        var certificateHolder = certGen.build(sigGen);

        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certificateHolder);

    }

    public static class KeySet {
        private final KeyStore keyStore;
        private final String keyStorePassword;

        private final KeyStore trustStore;
        private final String trustStorePassword;

        private X509Certificate certificate;

        public KeySet(KeyStore keyStore, String keyStorePassword, KeyStore trustStore, String trustStorePassword, X509Certificate certificate) {
            this.keyStore = keyStore;
            this.keyStorePassword = keyStorePassword;
            this.trustStore = trustStore;
            this.trustStorePassword = trustStorePassword;
            this.certificate = certificate;
        }

        public KeyStore getKeyStore() {
            return keyStore;
        }

        public String getKeyStorePassword() {
            return keyStorePassword;
        }

        public KeyStore getTrustStore() {
            return trustStore;
        }

        public String getTrustStorePassword() {
            return trustStorePassword;
        }

        public X509Certificate getCertificate() { return certificate; }
    }
}

/*
    Snippets for file based key store and trust store loading
 */
//        final ClassLoader classLoader = getClass().getClassLoader();
//        trustStoreFile = new File("truststore.jks");
//        keyStoreFile = new File("keystore.jks");
//        String test = trustStoreFile.getAbsolutePath();
//        copy(classLoader.getResourceAsStream("it/org/somda/sdc/dpws/truststore.jks"), trustStoreFile);
//        copy(classLoader.getResourceAsStream("it/org/somda/sdc/dpws/keystore.jks"), keyStoreFile);
//
//        trustStorePassword = "whatever";
//        keyStorePassword = "whatever";

//    private static void copy(final InputStream is, final File f) throws IOException {
//        final byte[] buf = new byte[4096];
//        FileOutputStream os = new FileOutputStream(f);
//        int len = 0;
//        while ((len = is.read(buf)) > 0) {
//            os.write(buf, 0, len);
//        }
//        is.close();
//        os.close();
//    }