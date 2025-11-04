package org.somda.sdc.dpws.http.helper;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpCompliance;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.crypto.CryptoConfig;
import org.somda.sdc.dpws.crypto.CryptoConfigurator;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.http.jetty.JettyHttpServerHandler;
import org.somda.sdc.dpws.http.jetty.factory.JettyHttpServerHandlerFactory;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.SoapConstants;

import javax.annotation.Nullable;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.time.Duration;

/**
 * Helper class to perform HTTP server - client connection self-test.
 */
public class HttpServerClientSelfTest {
    private static final Logger LOG = LogManager.getLogger(HttpServerClientSelfTest.class);

    private final Logger instanceLogger;
    private final boolean enableHttps;
    private final String[] tlsProtocols;
    private final String[] enabledCiphers;
    private final HostnameVerifier hostnameVerifier;
    private final Duration connectionTimeout;
    private final JettyHttpServerHandlerFactory jettyHttpServerHandlerFactory;
    private final CryptoConfigurator cryptoConfigurator;
    private final CryptoSettings cryptoSettings;
    private SSLContext sslContext;

    @Inject
    public HttpServerClientSelfTest(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                                    @Named(DpwsConfig.HTTPS_SUPPORT) boolean enableHttps,
                                    CryptoConfigurator cryptoConfigurator,
                                    @Nullable @Named(CryptoConfig.CRYPTO_SETTINGS) CryptoSettings cryptoSettings,
                                    @Named(CryptoConfig.CRYPTO_TLS_ENABLED_VERSIONS) String[] tlsProtocols,
                                    @Named(CryptoConfig.CRYPTO_TLS_ENABLED_CIPHERS) String[] enabledCiphers,
                                    @Named(CryptoConfig.CRYPTO_DEVICE_HOSTNAME_VERIFIER)
                                                HostnameVerifier hostnameVerifier,
                                    @Named(DpwsConfig.HTTP_SERVER_CONNECTION_TIMEOUT) Duration connectionTimeout,
                                    JettyHttpServerHandlerFactory jettyHttpServerHandlerFactory) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.enableHttps = enableHttps;
        this.cryptoConfigurator = cryptoConfigurator;
        this.cryptoSettings = cryptoSettings;
        this.tlsProtocols = tlsProtocols;
        this.hostnameVerifier = hostnameVerifier;
        this.enabledCiphers = enabledCiphers;
        this.connectionTimeout = connectionTimeout;
        this.jettyHttpServerHandlerFactory = jettyHttpServerHandlerFactory;
    }

    /**
     * Performs HTTP connection self-test during framework startup.
     */
    public void testConnection() {
        if (!this.enableHttps) {
            instanceLogger.info("HTTPS protocol disabled, skipping self-test");
            return;
        }
        if (cryptoSettings == null) {
            logAndThrowException("Crypto settings are required for HTTPS protocol");
        }

        instanceLogger.info("Starting self test between server and client");

        sslContext = getSslContext();
        var server = buildServer();

        String uri = server.getURI().toString();
        instanceLogger.info("Using URI {} for the self test", uri);

        var clientBuilder = buildHttpClient();
        try (CloseableHttpClient client = clientBuilder.build()) {
            try (CloseableHttpResponse response = client.execute(new HttpGet(uri))) {
                var responseCode = response.getStatusLine().getStatusCode();
                if (responseCode != 200) {
                    logAndThrowException("Connection self test failed with status: " + responseCode);
                }
                instanceLogger.info("Connection self test was successful");
            }
        } catch (IOException e) {
            logAndThrowException("Connection self test failed", e);
        } finally {
            try {
                server.stop();
                // CHECKSTYLE.OFF: IllegalCatch
            } catch (Exception e) {
                // CHECKSTYLE.ON: IllegalCatch
                instanceLogger.error("Could not stop HTTP server", e);
            }
        }
    }

    private SSLContext getSslContext() {
        try {
            var sslContextBuilder = SSLContexts.custom();
            if (cryptoSettings.getKeyStoreStream().isEmpty() || cryptoSettings.getTrustStoreStream().isEmpty()) {
                throw new IOException("Expected key and trust store, but none found");
            }

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(
                    cryptoSettings.getKeyStoreStream().get(),
                    cryptoSettings.getKeyStorePassword().toCharArray());
            sslContextBuilder.loadKeyMaterial(keyStore, cryptoSettings.getKeyStorePassword().toCharArray());

            printCertificateDetails(keyStore);

            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(
                    cryptoSettings.getTrustStoreStream().get(),
                    cryptoSettings.getTrustStorePassword().toCharArray());
            sslContextBuilder.loadTrustMaterial(trustStore, null);

            return sslContextBuilder.build();
        } catch (IllegalArgumentException |
                KeyStoreException |
                UnrecoverableKeyException |
                CertificateException |
                NoSuchAlgorithmException |
                IOException |
                KeyManagementException e) {
            instanceLogger.warn("Could not read server crypto config, fallback to system properties");
            return cryptoConfigurator.createSslContextFromSystemProperties();
        }
    }

    private HttpClientBuilder buildHttpClient() {
        return HttpClients.custom()
                .setDefaultSocketConfig(SocketConfig.custom().setTcpNoDelay(true).build())
                .setSSLSocketFactory(new SSLConnectionSocketFactory(
                        sslContext,
                        tlsProtocols,
                        enabledCiphers,
                        hostnameVerifier
                ));
    }

    private Server buildServer() {

        String schemeAndAuthority = "https://127.0.0.1:0";
        String contextPath = "/ctxt/self-test";
        instanceLogger.debug("Init new HTTP server from URI: {}", schemeAndAuthority);
        var uri = URI.create(schemeAndAuthority);

        var server = new Server(new InetSocketAddress(
                uri.getHost(),
                uri.getPort()));

        server.setHandler(new ContextHandlerCollection(getHttpHandler(contextPath)));
        server.setConnectors(new Connector[]{getServerConnector(uri, server)});
        try {
            server.start();
            // CHECKSTYLE.OFF: IllegalCatch
        } catch (Exception e) {
            // CHECKSTYLE.ON: IllegalCatch
            logAndThrowException("Could not start HTTP server for self-test", e);
        }
        return server;
    }

    private ServerConnector getServerConnector(URI uri, Server server) {
        var httpsConfig = new HttpConfiguration();
        httpsConfig.setHttpCompliance(HttpCompliance.RFC2616);

        SecureRequestCustomizer src = new SecureRequestCustomizer();
        // disable hostname validation, does not match sdc behavior
        src.setSniHostCheck(false);
        httpsConfig.addCustomizer(src);

        ServerConnector httpsConnector = new ServerConnector(server, getContextFactory(),
                new HttpConnectionFactory(httpsConfig));
        httpsConnector.setIdleTimeout(connectionTimeout.toMillis());
        httpsConnector.setHost(uri.getHost());
        httpsConnector.setPort(uri.getPort());
        return httpsConnector;
    }

    private SslConnectionFactory getContextFactory() {
        SslContextFactory.Server contextFactory = new SslContextFactory.Server();

        contextFactory.setSslContext(sslContext);
        contextFactory.setNeedClientAuth(true);
        contextFactory.setExcludeProtocols();
        contextFactory.setIncludeProtocols(tlsProtocols);
        contextFactory.setExcludeCipherSuites();
        contextFactory.setIncludeCipherSuites(enabledCiphers);
        return new SslConnectionFactory(contextFactory, HttpVersion.HTTP_1_1.asString());
    }

    private ContextHandler getHttpHandler(String contextPath) {
        var handler = new HttpHandler() {
            @Override
            public void handle(InputStream inStream,
                               OutputStream outStream,
                               CommunicationContext communicationContext) {
                try {
                    outStream.write("OK".getBytes());
                } catch (IOException e) {
                    logAndThrowException("HTTP handler failed", e);
                }
            }
        };
        JettyHttpServerHandler endpointHandler = this.jettyHttpServerHandlerFactory.create(
                SoapConstants.MEDIA_TYPE_SOAP, handler, null, null);

        ContextHandler contextHandler = new ContextHandler(contextPath);
        contextHandler.setHandler(endpointHandler);
        contextHandler.setAllowNullPathInfo(true);
        return contextHandler;
    }

    private void printCertificateDetails(KeyStore keyStore) {
        try {
            var aliases = keyStore.aliases();

            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
                instanceLogger.info("Certificate Alias: {}", alias);
                instanceLogger.info("Certificate Issuer DName: {}", certificate.getIssuerDN().getName());
                instanceLogger.info("Certificate Subject DName: {}", certificate.getSubjectDN().getName());
                instanceLogger.info("Certificate Extended Key Usage: {}", certificate.getExtendedKeyUsage());
                instanceLogger.info("Certificate Expiration: {}", certificate.getNotAfter());
                instanceLogger.trace("Certificate: {}", certificate);
            }
        } catch (CertificateParsingException | KeyStoreException e) {
            instanceLogger.warn("Failed to parse certificate details", e);
        }

    }

    private void logAndThrowException(String msg) {
        instanceLogger.error(msg);
        throw new ServerClientSelfTestException(msg);
    }

    private void logAndThrowException(String msg, Exception e) {
        instanceLogger.error(msg, e);
        throw new ServerClientSelfTestException(msg, e);
    }
}
