package org.somda.sdc.dpws.guice;

import org.somda.sdc.common.guice.AbstractConfigurationModule;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.client.ClientConfig;
import org.somda.sdc.dpws.crypto.CryptoConfig;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.sdc.dpws.device.DeviceConfig;
import org.somda.sdc.dpws.soap.SoapConfig;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingConfig;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConfig;
import org.somda.sdc.dpws.soap.wseventing.WsEventingConfig;
import org.somda.sdc.dpws.wsdl.WsdlProvisioningMode;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Default configuration module to configure {@link DefaultDpwsModule}.
 * <p>
 * Derive from this class to override default configuration values.
 * Use {@link #bind(String, Class, Object)} to set your default values.
 */
public class DefaultDpwsConfigModule extends AbstractConfigurationModule {
    @Override
    public void defaultConfigure() {
        configureWsAddressingConfig();
        configureWsDiscoveryConfig();
        configureWsEventingConfig();
        configureClientConfig();
        configureDeviceConfig();
        configureCryptoConfig();
        configureSoapConfig();
        configureDpws();
    }

    private void configureDeviceConfig() {
        bind(DeviceConfig.WSDL_PROVISIONING_MODE,
                WsdlProvisioningMode.class,
                WsdlProvisioningMode.RESOURCE);
    }

    private void configureDpws() {
        bind(DpwsConfig.MAX_WAIT_FOR_FUTURES,
                Duration.class,
                Duration.ofSeconds(10));

        bind(DpwsConfig.MAX_ENVELOPE_SIZE,
                Integer.class,
                DpwsConstants.MAX_ENVELOPE_SIZE);

        bind(DpwsConfig.COMMUNICATION_LOG_SINK_DIRECTORY,
                File.class,
                new File("commlog"));

        bind(DpwsConfig.COMMUNICATION_LOG_WITH_HTTP_HEADERS,
                Boolean.class,
                true);

        bind(DpwsConfig.COMMUNICATION_LOG_WITH_HTTP_REQUEST_RESPONSE_ID,
                Boolean.class,
                true);

        bind(DpwsConfig.COMMUNICATION_LOG_PRETTY_PRINT_XML,
                Boolean.class,
                false);

        bind(DpwsConfig.HTTP_GZIP_COMPRESSION,
                Boolean.class,
                true);

        bind(DpwsConfig.HTTP_RESPONSE_COMPRESSION_MIN_SIZE,
                Integer.class,
                32);

        bind(DpwsConfig.HTTPS_SUPPORT,
                Boolean.class,
                false);

        bind(DpwsConfig.HTTP_SUPPORT,
                Boolean.class,
                true);

        bind(DpwsConfig.ENFORCE_HTTP_CHUNKED_TRANSFER,
                Boolean.class,
                false);

        bind(DpwsConfig.HTTP_SERVER_CONNECTION_TIMEOUT,
                Duration.class,
                Duration.ofSeconds(30));

        bind(DpwsConfig.MULTICAST_TTL,
                Integer.class,
                DpwsConstants.DEFAULT_MULTICAST_TTL);

        bind(DpwsConfig.HTTP_CONTENT_TYPE_CHARSET,
                String.class,
                StandardCharsets.UTF_8.name());

        bind(DpwsConfig.HTTP_SERVER_COMMLOG_IN_HANDLER,
                Boolean.class,
                false);

        bind(DpwsConfig.HTTP_SERVER_THREAD_POOL_SIZE,
                Integer.class,
                200);

        bind(DpwsConfig.HTTP_CLIENT_POOL_SIZE,
                Integer.class,
                20);

        bind(DpwsConfig.HTTP_CLIENT_RETRY_POST,
                Boolean.class,
                false);
    }

    private void configureCryptoConfig() {
        bind(CryptoConfig.CRYPTO_SETTINGS,
                CryptoSettings.class,
                null);

        bind(CryptoConfig.CRYPTO_TLS_ENABLED_VERSIONS,
                String[].class,
                new String[]{"TLSv1.2", "TLSv1.3"});

        bind(CryptoConfig.CRYPTO_TLS_ENABLED_CIPHERS,
                String[].class,
                new String[]{
                        // 2020-03-03: mozilla modern tls 1.3 ciphers
                        "TLS_AES_128_GCM_SHA256",
                        "TLS_AES_256_GCM_SHA384",
                        // 2020-03-03: mozilla intermediate tls 1.2 ciphers (without ChaCha20)
                        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                });

        var defaultHostnameVerifier = new HostnameVerifier() {
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }

            @Override
            public String toString() {
                return "<accept every peer>";
            }
        };

        bind(CryptoConfig.CRYPTO_CLIENT_HOSTNAME_VERIFIER,
                HostnameVerifier.class,
                defaultHostnameVerifier);

        bind(CryptoConfig.CRYPTO_DEVICE_HOSTNAME_VERIFIER,
                HostnameVerifier.class,
                defaultHostnameVerifier);
    }

    private void configureClientConfig() {
        bind(ClientConfig.MAX_WAIT_FOR_RESOLVE_MATCHES,
                Duration.class,
                Duration.ofSeconds(10));

        bind(ClientConfig.AUTO_RESOLVE,
                Boolean.class,
                false);

        bind(DpwsConfig.HTTP_CLIENT_CONNECT_TIMEOUT,
                Duration.class,
                Duration.ofSeconds(5));

        bind(DpwsConfig.HTTP_CLIENT_READ_TIMEOUT,
                Duration.class,
                Duration.ofSeconds(5));

    }

    private void configureWsDiscoveryConfig() {
        bind(WsDiscoveryConfig.MAX_WAIT_FOR_PROBE_MATCHES,
                Duration.class,
                Duration.ofSeconds(10));
        bind(WsDiscoveryConfig.MAX_WAIT_FOR_RESOLVE_MATCHES,
                Duration.class,
                Duration.ofSeconds(10));
        bind(WsDiscoveryConfig.PROBE_MATCHES_BUFFER_SIZE,
                Integer.class,
                50);
        bind(WsDiscoveryConfig.RESOLVE_MATCHES_BUFFER_SIZE,
                Integer.class,
                50);
    }

    private void configureWsAddressingConfig() {
        bind(WsAddressingConfig.IGNORE_MESSAGE_IDS,
                Boolean.class,
                false);
        bind(WsAddressingConfig.MESSAGE_ID_CACHE_SIZE,
                Integer.class,
                50);
    }

    private void configureWsEventingConfig() {
        bind(WsEventingConfig.SOURCE_MAX_EXPIRES,
                Duration.class,
                Duration.ofHours(1));
        bind(WsEventingConfig.SOURCE_SUBSCRIPTION_MANAGER_PATH,
                String.class,
                "SubscriptionManager");
        bind(WsEventingConfig.NOTIFICATION_QUEUE_CAPACITY,
                Integer.class,
                500);
    }

    private void configureSoapConfig() {
        bind(SoapConfig.JAXB_CONTEXT_PATH,
                String.class,
                "");
        bind(SoapConfig.NAMESPACE_MAPPINGS,
                String.class,
                "");
        bind(SoapConfig.VALIDATE_SOAP_MESSAGES,
                Boolean.class,
                true);
        bind(SoapConfig.JAXB_SCHEMA_PATH,
                String.class,
                "");
        bind(SoapConfig.METADATA_COMMENT,
                Boolean.class,
                true);
    }
}
