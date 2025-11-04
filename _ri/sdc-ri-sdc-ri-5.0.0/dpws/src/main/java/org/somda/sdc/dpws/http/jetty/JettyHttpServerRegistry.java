package org.somda.sdc.dpws.http.jetty;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpCompliance;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.ssl.SslConnection;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.DetectorConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.CommunicationLogContext;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.crypto.CryptoConfig;
import org.somda.sdc.dpws.crypto.CryptoConfigurator;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.sdc.dpws.factory.CommunicationLogFactory;
import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.http.HttpServerRegistry;
import org.somda.sdc.dpws.http.HttpUriBuilder;
import org.somda.sdc.dpws.http.jetty.factory.JettyHttpServerHandlerFactory;
import org.somda.sdc.dpws.soap.SoapConstants;

import javax.annotation.Nullable;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class JettyServerData {
    private final Map<String, JettyHttpServerHandler> handlerRegistry;
    private final Map<String, ContextHandler> contextWrapperRegistry;
    private final ContextHandlerCollection contextHandlerCollection;

    JettyServerData(ContextHandlerCollection contextHandlerCollection) {
        handlerRegistry = new HashMap<>();
        contextWrapperRegistry = new HashMap<>();
        this.contextHandlerCollection = contextHandlerCollection;
    }

    public ContextHandlerCollection getContextHandlerCollection() {
        return contextHandlerCollection;
    }

    public Map<String, ContextHandler> getContextWrapperRegistry() {
        return contextWrapperRegistry;
    }

    public Map<String, JettyHttpServerHandler> getHandlerRegistry() {
        return handlerRegistry;
    }
}

/**
 * {@linkplain HttpServerRegistry} implementation based on Jetty HTTP servers.
 */
public class JettyHttpServerRegistry extends AbstractIdleService implements HttpServerRegistry {
    private static final Logger LOG = LogManager.getLogger(JettyHttpServerRegistry.class);

    private static final String URI_CONVERSION_ERROR_MSG = "Unexpected URI conversion error";

    private final JettyHttpServerHandlerFactory jettyHttpServerHandlerFactory;

    private final String frameworkIdentifier;
    private final Logger instanceLogger;
    private final CommunicationLog defaultCommunicationLog;
    private final Map<String, Server> serverRegistry;
    private final Map<Server, JettyServerData> serverData;
    private final Lock registryLock;
    private final HttpUriBuilder uriBuilder;
    private final boolean enableGzipCompression;
    private final int minCompressionSize;
    private final String[] tlsProtocols;
    private final String[] enabledCiphers;
    private final HostnameVerifier hostnameVerifier;
    private final boolean enableHttp;
    private final boolean enableHttps;
    private final Duration connectionTimeout;
    private SSLContext sslContext;
    private final boolean commlogInHandler;
    private final int threadPoolSize;

    @Inject
    JettyHttpServerRegistry(HttpUriBuilder uriBuilder,
                            CryptoConfigurator cryptoConfigurator,
                            @Nullable @Named(CryptoConfig.CRYPTO_SETTINGS) CryptoSettings cryptoSettings,
                            JettyHttpServerHandlerFactory jettyHttpServerHandlerFactory,
                            @Named(DpwsConfig.HTTP_GZIP_COMPRESSION) boolean enableGzipCompression,
                            @Named(DpwsConfig.HTTP_RESPONSE_COMPRESSION_MIN_SIZE) int minCompressionSize,
                            @Named(CryptoConfig.CRYPTO_TLS_ENABLED_VERSIONS) String[] tlsProtocols,
                            @Named(CryptoConfig.CRYPTO_TLS_ENABLED_CIPHERS) String[] enabledCiphers,
                            @Named(CryptoConfig.CRYPTO_DEVICE_HOSTNAME_VERIFIER) HostnameVerifier hostnameVerifier,
                            @Named(DpwsConfig.HTTPS_SUPPORT) boolean enableHttps,
                            @Named(DpwsConfig.HTTP_SUPPORT) boolean enableHttp,
                            @Named(DpwsConfig.HTTP_SERVER_CONNECTION_TIMEOUT) Duration connectionTimeout,
                            @Named(DpwsConfig.HTTP_SERVER_COMMLOG_IN_HANDLER) boolean commlogInHandler,
                            @Named(DpwsConfig.HTTP_SERVER_THREAD_POOL_SIZE) int threadPoolSize,
                            CommunicationLogFactory communicationLogFactory,
                            @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.frameworkIdentifier = frameworkIdentifier;
        this.uriBuilder = uriBuilder;
        this.jettyHttpServerHandlerFactory = jettyHttpServerHandlerFactory;
        this.enableGzipCompression = enableGzipCompression;
        this.minCompressionSize = minCompressionSize;
        this.tlsProtocols = tlsProtocols;
        this.enabledCiphers = enabledCiphers;
        this.hostnameVerifier = hostnameVerifier;
        this.defaultCommunicationLog = communicationLogFactory.createCommunicationLog();
        this.enableHttps = enableHttps;
        this.enableHttp = enableHttp;
        this.connectionTimeout = connectionTimeout;
        this.commlogInHandler = commlogInHandler;
        this.threadPoolSize = threadPoolSize;
        serverRegistry = new HashMap<>();
        serverData = new HashMap<>();
        registryLock = new ReentrantLock();
        configureSsl(cryptoConfigurator, cryptoSettings);

        if (!this.enableHttp && !this.enableHttps) {
            throw new RuntimeException("Http and https are disabled, cannot continue");
        }

    }

    @Override
    protected void startUp() throws Exception {
        // nothing to do here - servers will be started on demand
        instanceLogger.info("{} is running", getClass().getSimpleName());
    }

    @Override
    protected void shutDown() throws Exception {
        instanceLogger.info("Shut down running HTTP servers");
        registryLock.lock();
        try {

            serverRegistry.forEach((uri, server) -> {
                try {

                    server.stop();
                    instanceLogger.info("Shut down HTTP server at {}", uri);

                    final var data = serverData.remove(server);

                    if (data != null) {
                        data.getContextHandlerCollection().stop();
                        instanceLogger.info("Shut down HTTP context handler collection at {}", uri);

                        data.getHandlerRegistry().forEach((handlerUri, handler) -> {
                                    try {
                                        handler.stop();
                                        // CHECKSTYLE.OFF: IllegalCatch
                                    } catch (Exception e) {
                                        // CHECKSTYLE.ON: IllegalCatch
                                        instanceLogger.warn("HTTP handler could not be stopped properly", e);
                                    }
                                }
                        );

                        data.getContextWrapperRegistry().forEach((contextPath, wrapper) -> {
                            try {
                                wrapper.stop();
                                // CHECKSTYLE.OFF: IllegalCatch
                            } catch (Exception e) {
                                // CHECKSTYLE.ON: IllegalCatch
                                instanceLogger.warn("HTTP handler wrapper could not be stopped properly", e);
                            }
                        });
                    }
                    // CHECKSTYLE.OFF: IllegalCatch
                } catch (Exception e) {
                    // CHECKSTYLE.ON: IllegalCatch
                    instanceLogger.warn("HTTP server could not be stopped properly", e);
                }
            });

            serverRegistry.clear();
        } finally {
            registryLock.unlock();
        }
    }

    // TODO: 2.0.0 - return all created URIs, i.e. http and https
    @Override
    public String initHttpServer(String schemeAndAuthority, boolean allowReuse) {
        registryLock.lock();
        try {
            var commlogToUse = commlogInHandler ? null : defaultCommunicationLog;
            var server = makeHttpServer(schemeAndAuthority, commlogToUse, allowReuse);
            var uriString = server.getURI().toString();
            if (uriString.endsWith("/")) {
                uriString = uriString.substring(0, uriString.length() - 1);
            }
            var serverUri = URI.create(uriString);
            var requestedUri = URI.create(schemeAndAuthority);
            if (!serverUri.getScheme().equals(requestedUri.getScheme())) {
                try {
                    serverUri = replaceScheme(serverUri, requestedUri.getScheme());
                } catch (URISyntaxException e) {
                    instanceLogger.error(
                            "Unexpected error while creating server uri value with uri {} and new scheme {} value: {}",
                            serverUri, requestedUri.getScheme(),
                            e.getMessage()
                    );
                    instanceLogger.trace(
                            "Unexpected error while creating server uri value with uri {} and new scheme {} value",
                            serverUri, requestedUri.getScheme(),
                            e
                    );
                }
            }
            return serverUri.toString();
        } finally {
            registryLock.unlock();
        }
    }

    // TODO: 6.0.0 - return all created URIs, i.e. http and https
    @Override
    public String registerContext(String schemeAndAuthority,
                                  boolean allowReuse,
                                  String contextPath,
                                  @Nullable String mediaType,
                                  @Nullable CommunicationLogContext communicationLogContext,
                                  HttpHandler handler) {
        final var commLogToUse = defaultCommunicationLog;
        final String mediaTypeToUse = mediaType != null ? mediaType : SoapConstants.MEDIA_TYPE_SOAP;

        if (!contextPath.startsWith("/")) {
            throw new RuntimeException(String.format("Context path needs to start with a slash, but is %s",
                    contextPath));
        }

        registryLock.lock();
        try {
            // only use if commlogInHandler is false
            final var serverCommlog = commlogInHandler ? null : commLogToUse;
            Server server = makeHttpServer(schemeAndAuthority, serverCommlog, allowReuse);
            ImmutablePair<String, Integer> mapKey;
            try {
                mapKey = makeMapKey(server.getURI().toString(), contextPath);
            } catch (UnknownHostException e) {
                instanceLogger.error(URI_CONVERSION_ERROR_MSG, e);
                throw new RuntimeException(URI_CONVERSION_ERROR_MSG, e);
            }
            URI mapKeyUri = URI.create(mapKey.left);

            final var endpointCommlog = commlogInHandler ? commLogToUse : null;
            JettyHttpServerHandler endpointHandler = this.jettyHttpServerHandlerFactory
                    .create(mediaTypeToUse, handler, endpointCommlog, communicationLogContext);

            ContextHandler context = new ContextHandler(contextPath);
            context.setHandler(endpointHandler);
            context.setAllowNullPathInfo(true);

            final var jettyServerData = this.serverData.get(server);

            jettyServerData.getHandlerRegistry()
                    .put(mapKeyUri.toString(), endpointHandler);
            jettyServerData.getContextWrapperRegistry()
                    .put(contextPath, context);
            jettyServerData.getContextHandlerCollection().addHandler(context);

            context.start();

            // use requested scheme for response
            var contextUri = replaceScheme(mapKeyUri, URI.create(schemeAndAuthority).getScheme());
            return contextUri.toString();
            // CHECKSTYLE.OFF: IllegalCatch
        } catch (Exception e) {
            // CHECKSTYLE.ON: IllegalCatch
            instanceLogger.error("Registering context {} failed.", contextPath, e);
            throw new RuntimeException(e);
        } finally {
            registryLock.unlock();
        }
    }

    @Override
    public void unregisterContext(String schemeAndAuthority, String contextPath) {
        registryLock.lock();
        try {
            ImmutablePair<String, Integer> serverRegistryPair;
            ImmutablePair<String, Integer> httpHandlerRegistryPair;

            try {
                serverRegistryPair = makeMapKey(schemeAndAuthority);
                httpHandlerRegistryPair = makeMapKey(schemeAndAuthority, contextPath);
            } catch (UnknownHostException e) {
                instanceLogger.error(URI_CONVERSION_ERROR_MSG, e);
                throw new RuntimeException(URI_CONVERSION_ERROR_MSG, e);
            }

            Optional.ofNullable(serverRegistry.get(serverRegistryPair.left)).ifPresent(httpServer ->
            {
                final var jettyServerData = this.serverData.get(httpServer);

                if (jettyServerData != null) {
                    final var handler = jettyServerData
                            .getHandlerRegistry()
                            .remove(httpHandlerRegistryPair.left);
                    Optional.ofNullable(handler)
                        .ifPresent(
                            handlerWrapper -> {
                                instanceLogger.info("Unregister context path '{}'", contextPath);
                                ContextHandler removedHandler = jettyServerData
                                        .getContextWrapperRegistry()
                                        .remove(contextPath);
                                jettyServerData.getContextHandlerCollection().removeHandler(removedHandler);
                            }
                        );
                }

                if (jettyServerData == null || jettyServerData.getHandlerRegistry().isEmpty()) {
                    instanceLogger.info("No further HTTP handlers active. Shutdown HTTP server at '{}'",
                            schemeAndAuthority);
                    stopServer(serverRegistry.remove(serverRegistryPair.left));
                }
            });
        } finally {
            registryLock.unlock();
        }

    }

    private void stopServer(Server server) {
        // stop all handlers
        try {
            server.stop();
            // CHECKSTYLE.OFF: IllegalCatch
        } catch (Exception e) {
            // CHECKSTYLE.ON: IllegalCatch
            instanceLogger.error("Could not stop HTTP server", e);
        }

        final var jettyServerData = this.serverData.remove(server);

        if (jettyServerData != null) {
            if (jettyServerData.getHandlerRegistry().size() != 0) {
                LOG.warn("Lingering handlers in shut down server");
            }

            try {
                jettyServerData.getContextHandlerCollection().stop();
                // CHECKSTYLE.OFF: IllegalCatch
            } catch (Exception e) {
                // CHECKSTYLE.ON: IllegalCatch
                instanceLogger.error("Could not stop HTTP ContextHandlerCollection", e);
            }

            for (var x : jettyServerData.getContextWrapperRegistry().values()) {
                try {
                    x.stop();
                    // CHECKSTYLE.OFF: IllegalCatch
                } catch (Exception e) {
                    // CHECKSTYLE.ON: IllegalCatch
                    instanceLogger.error("Could not stop HTTP ContextHandler", e);
                }
            }
        }
    }


    private void configureSsl(CryptoConfigurator cryptoConfigurator,
                              @Nullable CryptoSettings cryptoSettings) {
        if (cryptoSettings == null) {
            sslContext = null;
            return;
        }

        try {
            sslContext = cryptoConfigurator.createSslContextFromCryptoConfig(cryptoSettings);
        } catch (IllegalArgumentException |
                KeyStoreException |
                UnrecoverableKeyException |
                CertificateException |
                NoSuchAlgorithmException |
                IOException |
                KeyManagementException e) {
            instanceLogger.warn("Could not read server crypto config, fallback to system properties");
            sslContext = cryptoConfigurator.createSslContextFromSystemProperties();
        }
    }

    private Server makeHttpServer(String uri, @Nullable CommunicationLog communicationLog, boolean allowReuse) {
        Pair<String, Integer> mapKeyPair;
        try {
            mapKeyPair = makeMapKey(uri);
        } catch (UnknownHostException e) {
            instanceLogger.error(URI_CONVERSION_ERROR_MSG, e);
            throw new RuntimeException(URI_CONVERSION_ERROR_MSG, e);
        }

        Optional<Server> oldServer;
        if (allowReuse && mapKeyPair.getRight() == 0) {
            oldServer = serverRegistry.values().stream().findFirst();
        } else {
            oldServer = Optional.ofNullable(serverRegistry.get(mapKeyPair.getLeft()));
        }
        if (oldServer.isPresent()) {
            instanceLogger.debug("Re-use running HTTP server from URI: {}", oldServer.get().getURI().getHost());
            return oldServer.get();
        }

        instanceLogger.debug("Init new HTTP server from URI: {}", uri);
        Server httpServer = createHttpServer(URI.create(uri), communicationLog);
        try {
            httpServer.start();
            // CHECKSTYLE.OFF: IllegalCatch
        } catch (Exception e) {
            // CHECKSTYLE.ON: IllegalCatch
            throw new RuntimeException(e);
        }

        var serverUri = httpServer.getURI().toString();
        try {
            serverRegistry.put(makeMapKey(serverUri).left, httpServer);
        } catch (UnknownHostException e) {
            instanceLogger.error(URI_CONVERSION_ERROR_MSG, e);
            throw new RuntimeException(URI_CONVERSION_ERROR_MSG, e);
        }
        instanceLogger.debug("New HTTP server initialized: {}", uri);
        return httpServer;
    }

    private Server createHttpServer(URI uri, @Nullable CommunicationLog communicationLog) {
        instanceLogger.info("Setup HTTP server for address '{}'", uri);
        if (!isSupportedScheme(uri)) {
            throw new RuntimeException(String.format("HTTP server setup failed. Unsupported scheme: %s",
                    uri.getScheme()));
        }

        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSecureScheme(HttpScheme.HTTPS.asString());
        httpConfig.setHttpCompliance(HttpCompliance.RFC2616);

        var server = new Server(new QueuedThreadPool(threadPoolSize));

        ContextHandlerCollection context = new ContextHandlerCollection();
        server.setHandler(context);

        final var jettyServerData = new JettyServerData(context);

        this.serverData.put(server, jettyServerData);

        CommunicationLogHandlerWrapper commlogHandler = new CommunicationLogHandlerWrapper(
                communicationLog, frameworkIdentifier
        );
        commlogHandler.setHandler(server.getHandler());
        server.setHandler(commlogHandler);

        // wrap the context handler in a gzip handler
        if (this.enableGzipCompression) {
            GzipHandler gzipHandler = new GzipHandler();
            gzipHandler.setIncludedMethods(
                    HttpMethod.PUT.asString(),
                    HttpMethod.POST.asString(),
                    HttpMethod.GET.asString()
            );
            gzipHandler.setInflateBufferSize(2048);
            gzipHandler.setHandler(server.getHandler());
            gzipHandler.setMinGzipSize(minCompressionSize);
            gzipHandler.setIncludedMimeTypes(
                    "text/plain", "text/html",
                    SoapConstants.MEDIA_TYPE_SOAP, SoapConstants.MEDIA_TYPE_WSDL
            );
            server.setHandler(gzipHandler);
        }

        if (sslContext != null && enableHttps) {
            SslContextFactory.Server contextFactory = new SslContextFactory.Server();
            contextFactory.setSslContext(sslContext);
            contextFactory.setNeedClientAuth(true);

            instanceLogger.debug("Enabled protocols: {}", () -> List.of(tlsProtocols));

            // reset excluded protocols to force only included protocols
            contextFactory.setExcludeProtocols();
            contextFactory.setIncludeProtocols(tlsProtocols);

            // reset excluded ciphers to force only included protocols
            contextFactory.setExcludeCipherSuites();
            contextFactory.setIncludeCipherSuites(enabledCiphers);

            SecureRequestCustomizer src = new SecureRequestCustomizer();
            // disable hostname validation, does not match sdc behavior
            src.setSniHostCheck(false);

            HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
            HttpConfiguration.Customizer clientVerifier = (Connector connector,
                                                           HttpConfiguration channelConfig,
                                                           Request request) -> {
                var numRequest = request.getHttpChannel().getRequests();
                if (numRequest != 1) {
                    instanceLogger.debug("Connection already verified");
                    return;
                }
                EndPoint endp = request.getHttpChannel().getEndPoint();
                if (endp instanceof SslConnection.DecryptedEndPoint) {
                    SslConnection.DecryptedEndPoint sslEndp = (SslConnection.DecryptedEndPoint) endp;
                    SslConnection sslConnection = sslEndp.getSslConnection();
                    SSLEngine sslEngine = sslConnection.getSSLEngine();

                    var session = sslEngine.getSession();

                    if (!hostnameVerifier.verify(sslEndp.getLocalAddress().getHostName(), session)) {
                        instanceLogger.debug("HostnameVerifier has filtered request, marking request as " +
                                "handled and aborting request");
                        request.setHandled(true);
                        request.getHttpChannel().abort(new Exception("HostnameVerifier has rejected request"));
                    }
                }
            };
            httpsConfig.addCustomizer(clientVerifier);
            httpsConfig.addCustomizer(src);

            var connectionFactory = new SslConnectionFactory(contextFactory, HttpVersion.HTTP_1_1.asString());
            ServerConnector httpsConnector;

            HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpsConfig);

            if (enableHttp) {
                httpsConnector = new ServerConnector(server,
                        new DetectorConnectionFactory(connectionFactory),
                        connectionFactory,
                        httpConnectionFactory);
            } else {
                httpsConnector = new ServerConnector(server,
                        connectionFactory,
                        httpConnectionFactory);
            }
            httpsConnector.setIdleTimeout(connectionTimeout.toMillis());
            httpsConnector.setHost(uri.getHost());
            httpsConnector.setPort(uri.getPort());

            server.setConnectors(new Connector[]{httpsConnector});
        } else {
            HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory();
            ServerConnector httpConnector;
            httpConnector = new ServerConnector(server, httpConnectionFactory);
            httpConnector.setIdleTimeout(connectionTimeout.toMillis());
            httpConnector.setHost(uri.getHost());
            httpConnector.setPort(uri.getPort());

            server.setConnectors(new Connector[]{httpConnector});
        }

        return server;
    }


    /*
     * Calculate http server map key:
     * - scheme is replaced by httpx to compare entries independent of used scheme
     * - host address is used instead of DNS name.
     *
     * Returns the port as second parameter of the pair.
     *
     * throws UnknownHostException if host address cannot be resolved.
     */
    private ImmutablePair<String, Integer> makeMapKey(String uri) throws UnknownHostException {
        URI parsedUri = URI.create(uri);
        InetAddress address = InetAddress.getByName(parsedUri.getHost());
        return new ImmutablePair<>(
                uriBuilder.buildUri("httpx", address.getHostAddress(), parsedUri.getPort()),
                parsedUri.getPort()
        );
    }

    private ImmutablePair<String, Integer> makeMapKey(String uri, String contextPath) throws UnknownHostException {
        final var pair = makeMapKey(uri);
        return new ImmutablePair<>(pair.getLeft() + contextPath, pair.getRight());
    }

    private URI replaceScheme(URI baseUri, String scheme) throws URISyntaxException {
        return new URI(scheme, baseUri.getUserInfo(),
                baseUri.getHost(), baseUri.getPort(),
                baseUri.getPath(), baseUri.getQuery(),
                baseUri.getFragment());
    }

    private boolean isSupportedScheme(URI address) {
        return (enableHttp && HttpScheme.HTTP.asString().equalsIgnoreCase(address.getScheme()))
                || (enableHttps && HttpScheme.HTTPS.asString().equalsIgnoreCase(address.getScheme()));
    }
}
