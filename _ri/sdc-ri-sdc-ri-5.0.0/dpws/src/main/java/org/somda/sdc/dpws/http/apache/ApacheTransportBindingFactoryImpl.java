package org.somda.sdc.dpws.http.apache;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.CommunicationLogContext;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.TransportBinding;
import org.somda.sdc.dpws.crypto.CryptoConfig;
import org.somda.sdc.dpws.crypto.CryptoConfigurator;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.sdc.dpws.factory.CommunicationLogFactory;
import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.http.factory.HttpClientFactory;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.dpws.soap.SoapUtil;

import javax.annotation.Nullable;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Factory for creating apache http client backed transport bindings and clients.
 */
public class ApacheTransportBindingFactoryImpl implements TransportBindingFactory, HttpClientFactory {

    private static final Logger LOG = LogManager.getLogger(ApacheTransportBindingFactoryImpl.class);

    private static final String SCHEME_SOAP_OVER_UDP = DpwsConstants.URI_SCHEME_SOAP_OVER_UDP;
    private static final String SCHEME_HTTP = "http";
    private static final String SCHEME_HTTPS = "https";

    private final SoapMarshalling marshalling;
    private final SoapUtil soapUtil;
    private final boolean enableGzipCompression;
    private final Duration clientConnectTimeout;
    private final Duration clientReadTimeout;
    private final HttpClient client;
    private final String[] tlsProtocols;
    private final HostnameVerifier hostnameVerifier;
    private final String[] enabledCiphers;
    private final boolean enableHttps;
    private final boolean enableHttp;
    private final CryptoConfigurator cryptoConfigurator;
    @Nullable
    private final CryptoSettings cryptoSettings;
    private final Logger instanceLogger;
    private final String frameworkIdentifier;
    private final int clientPoolSize;
    private final boolean retryNonIdempotent;

    private ClientTransportBindingFactory clientTransportBindingFactory;

    @Inject
    ApacheTransportBindingFactoryImpl(SoapMarshalling marshalling,
                                      SoapUtil soapUtil,
                                      CryptoConfigurator cryptoConfigurator,
                                      @Nullable @Named(CryptoConfig.CRYPTO_SETTINGS) CryptoSettings cryptoSettings,
                                      @Named(DpwsConfig.HTTP_CLIENT_CONNECT_TIMEOUT) Duration clientConnectTimeout,
                                      @Named(DpwsConfig.HTTP_CLIENT_READ_TIMEOUT) Duration clientReadTimeout,
                                      @Named(DpwsConfig.HTTP_CLIENT_POOL_SIZE) int clientPoolSize,
                                      @Named(DpwsConfig.HTTP_CLIENT_RETRY_POST) boolean retryNonIdempotent,
                                      @Named(DpwsConfig.HTTP_GZIP_COMPRESSION) boolean enableGzipCompression,
                                      ClientTransportBindingFactory clientTransportBindingFactory,
                                      CommunicationLogFactory communicationLogFactory,
                                      @Named(CryptoConfig.CRYPTO_TLS_ENABLED_VERSIONS) String[] tlsProtocols,
                                      @Named(CryptoConfig.CRYPTO_TLS_ENABLED_CIPHERS) String[] enabledCiphers,
                                      @Named(CryptoConfig.CRYPTO_CLIENT_HOSTNAME_VERIFIER)
                                              HostnameVerifier hostnameVerifier,
                                      @Named(DpwsConfig.HTTPS_SUPPORT) boolean enableHttps,
                                      @Named(DpwsConfig.HTTP_SUPPORT) boolean enableHttp,
                                      @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.cryptoConfigurator = cryptoConfigurator;
        this.cryptoSettings = cryptoSettings;
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.frameworkIdentifier = frameworkIdentifier;
        this.marshalling = marshalling;
        this.soapUtil = soapUtil;
        this.clientConnectTimeout = clientConnectTimeout;
        this.clientReadTimeout = clientReadTimeout;
        this.enableGzipCompression = enableGzipCompression;
        this.clientTransportBindingFactory = clientTransportBindingFactory;
        this.tlsProtocols = tlsProtocols;
        this.enabledCiphers = enabledCiphers;
        this.hostnameVerifier = hostnameVerifier;
        this.enableHttps = enableHttps;
        this.enableHttp = enableHttp;
        this.clientPoolSize = clientPoolSize;
        this.retryNonIdempotent = retryNonIdempotent;

        if (!this.enableHttp && !this.enableHttps) {
            throw new RuntimeException("Http and https are disabled, cannot continue");
        }

        this.client = buildClient(cryptoConfigurator, cryptoSettings, communicationLogFactory.createCommunicationLog());
    }

    private HttpClient buildClient(CryptoConfigurator cryptoConfigurator,
                                   @Nullable CryptoSettings cryptoSettings,
                                   CommunicationLog communicationLog) {
        var socketConfig = SocketConfig.custom().setTcpNoDelay(true).build();

        // set the timeout for all requests
        var requestConfig = RequestConfig.custom().setConnectionRequestTimeout((int) clientReadTimeout.toMillis())
                .setConnectTimeout((int) clientConnectTimeout.toMillis())
                .setSocketTimeout((int) clientConnectTimeout.toMillis()).build();

        var clientBuilder = HttpClients.custom().setDefaultSocketConfig(socketConfig)
                // attach interceptors to enable communication log capabilities including message headers
                .addInterceptorLast(new CommunicationLogHttpRequestInterceptor(communicationLog, frameworkIdentifier,
                        cryptoConfigurator.getCertificates(cryptoSettings)))
                .addInterceptorLast(new CommunicationLogHttpResponseInterceptor(communicationLog, frameworkIdentifier))
                .setDefaultRequestConfig(requestConfig)
                // allow reusing ssl connections in the pool
                .disableConnectionState()
                // retry every request just once in case the socket has died
                .setRetryHandler(new DefaultHttpRequestRetryHandler(1, retryNonIdempotent));
        if (!enableGzipCompression) {
            // disable gzip compression
            clientBuilder.disableContentCompression();

        }

        var registryBuilder = RegistryBuilder.<ConnectionSocketFactory>create();

        if (enableHttps) {
            SSLContext sslContext;
            try {
                sslContext = cryptoConfigurator.createSslContextFromCryptoConfig(cryptoSettings);
                // CHECKSTYLE.OFF: IllegalCatch
            } catch (Exception e) {
                // CHECKSTYLE.ON: IllegalCatch
                instanceLogger.error("Could not read client crypto config, fallback to system properties", e);
                sslContext = cryptoConfigurator.createSslContextFromSystemProperties();
            }

            instanceLogger.debug("Enabled protocols: {}", () -> List.of(tlsProtocols));
            SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                    sslContext,
                    tlsProtocols,
                    enabledCiphers,
                    hostnameVerifier
            );

            registryBuilder.register("https", socketFactory);
        }
        if (enableHttp) {
            registryBuilder.register("http", PlainConnectionSocketFactory.getSocketFactory());
        }

        final PoolingHttpClientConnectionManager poolingmgr = new PoolingHttpClientConnectionManager(
                registryBuilder.build(),
                null,
                null,
                null,
                -1,
                TimeUnit.MILLISECONDS
        );

        // only allow one connection per host
        poolingmgr.setDefaultMaxPerRoute(1);
        poolingmgr.setMaxTotal(clientPoolSize);

        return clientBuilder
                .setConnectionManager(poolingmgr)
                .build();
    }

    @Override
    public TransportBinding createTransportBinding(
            String endpointUri,
            @Nullable CommunicationLogContext communicationLogContext
    ) throws UnsupportedOperationException {
        // To keep things simple, this method directly checks if there is a SOAP-UDP or
        // HTTP(S) binding
        // No plug-and-play feature is implemented that dispatches, based on the URI
        // scheme, to endpoint processor
        // factories

        String scheme = URI.create(endpointUri).getScheme();
        if (scheme.equalsIgnoreCase(SCHEME_SOAP_OVER_UDP)) {
            throw new UnsupportedOperationException(
                    "SOAP-over-UDP is currently not supported by the TransportBindingFactory");
        } else if (scheme.equalsIgnoreCase(SCHEME_HTTP)) {
            return createHttpBinding(endpointUri, communicationLogContext);
        } else if (scheme.equalsIgnoreCase(SCHEME_HTTPS)) {
            return createHttpBinding(endpointUri, communicationLogContext);
        } else {
            throw new UnsupportedOperationException(
                    String.format("Unsupported transport binding requested: %s", scheme));
        }
    }

    @Override
    public TransportBinding createHttpBinding(
            String endpointUri,
            @Nullable CommunicationLogContext communicationLogContext
    ) throws UnsupportedOperationException {
        var scheme = URI.create(endpointUri).getScheme();
        if (scheme.toLowerCase().startsWith("http")) {
            return this.clientTransportBindingFactory.create(
                    this.client,
                    endpointUri,
                    marshalling,
                    soapUtil,
                    communicationLogContext
            );
        }

        throw new UnsupportedOperationException(
                String.format("Binding with scheme %s is currently not supported", scheme));
    }

    @Override
    public org.somda.sdc.dpws.http.HttpClient createHttpClient() {
        return this.clientTransportBindingFactory.createHttpClient(client);
    }

    /**
     * Access the configured apache http client.
     * <p>
     * Note: <em>Do not</em> use this client for productive purposes, always use the {@linkplain TransportBinding}
     * instead. This is only useful if you want to send intentionally bad messages to a server, which you most likely
     * do not want.
     *
     * @return the configured apache http client
     */
    public HttpClient getClient() {
        return client;
    }
}
