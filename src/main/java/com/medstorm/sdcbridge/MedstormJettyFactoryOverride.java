package com.medstorm.sdcbridge;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import java.lang.reflect.Constructor;

import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.CommunicationLogContext;
import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.http.jetty.JettyHttpServerHandler;
import org.somda.sdc.dpws.http.jetty.factory.JettyHttpServerHandlerFactory;

/**
 * Overrides the default Jetty handler factory so we never pass null
 * CommunicationLog/CommunicationLogContext to JettyHttpServerHandler's ctor.
 */
public final class MedstormJettyFactoryOverride extends AbstractModule {

    @Provides
    @Singleton
    JettyHttpServerHandlerFactory provideJettyHttpServerHandlerFactory(
            // fallbacks coming from MedstormSdcriConfigModule providers (non-null no-ops)
            CommunicationLog fallbackLog,
            CommunicationLogContext fallbackCtx,
            @Named("Common.InstanceIdentifier") String frameworkId,
            @Named("Dpws.EnforceHttpChunked") Boolean chunkedTransfer,
            @Named("Dpws.HttpCharset") String charset
    ) {
        final boolean chunked = Boolean.TRUE.equals(chunkedTransfer);
        final String cs = (charset == null ? "UTF-8" : charset);

        // Return a factory that replaces nulls with our fallbacks and calls the ctor reflectively.
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
            // package-private ctor signature in SDCri 6.x:
            // (String, HttpHandler, CommunicationLog, CommunicationLogContext, String, boolean, String)
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
