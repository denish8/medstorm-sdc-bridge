package com.medstorm.sdcbridge;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.CommunicationLogContext;
import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.http.HttpServerRegistry;
import org.somda.sdc.dpws.http.jetty.JettyHttpServerHandler;
import org.somda.sdc.dpws.http.jetty.factory.JettyHttpServerHandlerFactory;

import java.lang.reflect.Constructor;

/**
 * 1) Forces DPWS to use our HttpServerRegistry that normalizes ":0" -> configured port.
 * 2) Overrides Jetty handler factory so we never pass null CommunicationLog/Context.
 */
public final class MedstormJettyFactoryOverride extends AbstractModule {

    @Override
    protected void configure() {
        bind(HttpServerRegistry.class)
                .to(MedstormHttpServerRegistryOverride.class)
                .in(Singleton.class);
    }

    @Provides
    @Singleton
    JettyHttpServerHandlerFactory provideJettyHttpServerHandlerFactory(
            CommunicationLog fallbackLog,
            CommunicationLogContext fallbackCtx,
            @Named("Common.InstanceIdentifier") String frameworkId,
            @Named("Dpws.EnforceHttpChunked") Boolean chunkedTransfer,
            @Named("Dpws.HttpCharset") String charset
    ) {
        final boolean chunked = Boolean.TRUE.equals(chunkedTransfer);
        final String cs = (charset == null ? "UTF-8" : charset);

        return (String mediaType,
                HttpHandler httpHandler,
                CommunicationLog commLog,
                CommunicationLogContext commCtx) -> constructJettyHandler(
                mediaType,
                httpHandler,
                (commLog != null ? commLog : fallbackLog),
                (commCtx != null ? commCtx : fallbackCtx),
                frameworkId,
                chunked,
                cs
        );
    }

    private static JettyHttpServerHandler constructJettyHandler(
            String mediaType,
            HttpHandler httpHandler,
            CommunicationLog commLog,
            CommunicationLogContext commCtx,
            String frameworkId,
            boolean chunkedTransfer,
            String charset
    ) {
        try {
            Constructor<JettyHttpServerHandler> ctor =
                    JettyHttpServerHandler.class.getDeclaredConstructor(
                            String.class,
                            HttpHandler.class,
                            CommunicationLog.class,
                            CommunicationLogContext.class,
                            String.class,
                            boolean.class,
                            String.class
                    );
            ctor.setAccessible(true);
            return ctor.newInstance(mediaType, httpHandler, commLog, commCtx, frameworkId, chunkedTransfer, charset);
        } catch (Throwable t) {
            throw new IllegalStateException("Failed to construct JettyHttpServerHandler reflectively", t);
        }
    }
}
