package org.somda.sdc.dpws.http.jetty.factory;

import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.CommunicationLogContext;
import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.http.jetty.JettyHttpServerHandler;

import javax.annotation.Nullable;

/**
 * Creates {@linkplain JettyHttpServerHandler} instances.
 */
public interface JettyHttpServerHandlerFactory {

    /**
     * Instantiates {@linkplain JettyHttpServerHandler} with the given objects and injected objects.
     *
     * @param mediaType media type of transmitted content
     * @param handler   to handle incoming requests
     * @param communicationLog a communication log, if null no logging happens
     * @param communicationLogContext additional information made available in the communication log
     * @return a new {@linkplain JettyHttpServerHandler}
     */
    JettyHttpServerHandler create(
            String mediaType,
            HttpHandler handler,
            @Nullable CommunicationLog communicationLog,
            @Nullable CommunicationLogContext communicationLogContext
    );

}
