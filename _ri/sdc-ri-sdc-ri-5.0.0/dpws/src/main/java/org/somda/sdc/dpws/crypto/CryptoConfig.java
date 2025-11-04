package org.somda.sdc.dpws.crypto;

/**
 * Configuration keys for cryptographic components.
 *
 * @see org.somda.sdc.dpws.guice.DefaultDpwsConfigModule
 */
public class CryptoConfig {
    /**
     * Used to retrieve SSL configuration.
     * <ul>
     * <li>Data type: {@link CryptoSettings}
     * <li>Use: optional
     * </ul>
     */
    public static final String CRYPTO_SETTINGS = "Dpws.Crypto.Settings";

    /**
     * Protocols enabled for secure communication, support may depend on used libraries.
     *
     * <ul>
     * <li>Data type: array of {@link String}
     * <li>Use: optional
     * </ul>
     */
    public static final String CRYPTO_TLS_ENABLED_VERSIONS = "Dpws.Crypto.TlsEnabledVersions";

    /**
     * Ciphers enabled for secure communication, support may depend on used libraries.
     * <p>
     * <em>Uses the IANA notation for ciphers!</em>
     *
     * <ul>
     * <li>Data type: array of {@link String}
     * <li>Use: optional
     * </ul>
     */
    public static final String CRYPTO_TLS_ENABLED_CIPHERS = "Dpws.Crypto.TlsEnabledCiphers";

    /**
     * Hostname verifier called on new connections in the http client.
     *
     * <ul>
     * <li>Data type: {@link javax.net.ssl.HostnameVerifier}
     * <li>Use: optional
     * </ul>
     */
    public static final String CRYPTO_CLIENT_HOSTNAME_VERIFIER = "Dpws.Crypto.ClientHostnameVerifier";

    /**
     * Hostname verifier called on new connections in the http server.
     *
     * <ul>
     * <li>Data type: {@link javax.net.ssl.HostnameVerifier}
     * <li>Use: optional
     * </ul>
     */
    public static final String CRYPTO_DEVICE_HOSTNAME_VERIFIER = "Dpws.Crypto.DeviceHostnameVerifier";

}
