package com.medstorm.sdcbridge;

import com.google.common.util.concurrent.Service;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.CommunicationLogContext;
import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.http.HttpServerRegistry;
import org.somda.sdc.dpws.http.jetty.JettyHttpServerRegistry;

import java.net.URI;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Singleton
public final class MedstormHttpServerRegistryOverride implements HttpServerRegistry {
    private static final Logger log = LoggerFactory.getLogger(MedstormHttpServerRegistryOverride.class);

    private final JettyHttpServerRegistry delegate;
    private final String httpHost;
    private final int httpPort;

    @Inject
    public MedstormHttpServerRegistryOverride(
            JettyHttpServerRegistry delegate,
            @Named("Dpws.HttpHost") String httpHost,
            @Named("Dpws.HttpServerPort") Integer httpPort
    ) {
        this.delegate = delegate;

        this.httpHost = (httpHost == null || httpHost.isBlank())
                ? System.getProperty("Dpws.HttpHost", "127.0.0.1")
                : httpHost.trim();

        int p = (httpPort != null ? httpPort : 0);
        if (p <= 0) p = Integer.getInteger("Dpws.HttpServerPort", 53200);
        if (p <= 0) p = 53200;
        this.httpPort = p;

        log.info("[HttpServerRegistryOverride] configured httpHost={} httpPort={}", this.httpHost, this.httpPort);
    }

    private String normalizeServerUri(String serverUri) {
        if (serverUri == null || serverUri.isBlank()) {
            String u = "http://" + httpHost + ":" + httpPort;
            log.info("[HttpServerRegistryOverride] serverUri blank -> {}", u);
            return u;
        }

        try {
            URI u = URI.create(serverUri.trim());

            String scheme = (u.getScheme() == null || u.getScheme().isBlank()) ? "http" : u.getScheme();
            String host = (u.getHost() == null || u.getHost().isBlank()) ? httpHost : u.getHost();
            int port = u.getPort();

            // normalize :0 (or missing) -> configured port
            if (port <= 0) port = httpPort;

            URI fixed = new URI(
                    scheme,
                    u.getUserInfo(),
                    host,
                    port,
                    u.getPath(),
                    u.getQuery(),
                    u.getFragment()
            );

            String out = fixed.toString();
            if (!out.equals(serverUri)) {
                log.info("[HttpServerRegistryOverride] normalize {} -> {}", serverUri, out);
            }
            return out;
        } catch (Exception e) {
            String u = "http://" + httpHost + ":" + httpPort;
            log.warn("[HttpServerRegistryOverride] parse failed for '{}', forcing {}", serverUri, u);
            return u;
        }
    }

    // ---- HttpServerRegistry ----

    @Override
    public String initHttpServer(String serverUri, boolean https) {
        return delegate.initHttpServer(normalizeServerUri(serverUri), https);
    }

    @Override
    public String registerContext(
            String serverUri,
            boolean https,
            String mediaType,
            String contextPath,
            CommunicationLogContext commLogContext,
            HttpHandler handler
    ) {
        return delegate.registerContext(
                normalizeServerUri(serverUri),
                https,
                mediaType,
                contextPath,
                commLogContext,
                handler
        );
    }

    @Override
    public void unregisterContext(String serverUri, String contextId) {
        delegate.unregisterContext(normalizeServerUri(serverUri), contextId);
    }

    // ---- Guava Service delegation (HttpServerRegistry extends Service) ----

    @Override
    public Service startAsync() {
        delegate.startAsync();
        return this;
    }

    @Override
    public boolean isRunning() {
        return delegate.isRunning();
    }

    @Override
    public State state() {
        return delegate.state();
    }

    @Override
    public Service stopAsync() {
        delegate.stopAsync();
        return this;
    }

    @Override
    public void awaitRunning() {
        delegate.awaitRunning();
    }

    @Override
    public void awaitRunning(long timeout, TimeUnit unit) throws TimeoutException {
        delegate.awaitRunning(timeout, unit);
    }

    @Override
    public void awaitTerminated() {
        delegate.awaitTerminated();
    }

    @Override
    public void awaitTerminated(long timeout, TimeUnit unit) throws TimeoutException {
        delegate.awaitTerminated(timeout, unit);
    }

    @Override
    public Throwable failureCause() {
        return delegate.failureCause();
    }

    @Override
    public void addListener(Listener listener, Executor executor) {
        delegate.addListener(listener, executor);
    }
}
