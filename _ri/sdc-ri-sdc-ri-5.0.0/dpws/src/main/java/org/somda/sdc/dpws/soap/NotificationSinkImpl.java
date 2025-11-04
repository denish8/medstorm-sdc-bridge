package org.somda.sdc.dpws.soap;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.interception.Direction;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.interception.InterceptorRegistry;
import org.somda.sdc.dpws.soap.interception.NotificationObject;
import org.somda.sdc.dpws.soap.interception.ServerDispatcher;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingServerInterceptor;

/**
 * Default implementation of {@linkplain NotificationSink}.
 */
public class NotificationSinkImpl implements NotificationSink {
    private static final Logger LOG = LogManager.getLogger(NotificationSinkImpl.class);

    private final InterceptorRegistry interceptorRegistry;
    private final ServerDispatcher serverDispatcher;
    private final Logger instanceLogger;

    @Inject
    NotificationSinkImpl(@Assisted WsAddressingServerInterceptor wsaServerInterceptor,
                         ServerDispatcher serverDispatcher,
                         InterceptorRegistry interceptorRegistry,
                         @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.serverDispatcher = serverDispatcher;
        this.interceptorRegistry = interceptorRegistry;
        register(wsaServerInterceptor);
    }

    @Override
    public void register(Interceptor interceptor) {
        interceptorRegistry.addInterceptor(interceptor);
    }

    @Override
    public void receiveNotification(SoapMessage notification, CommunicationContext communicationContext)
        throws SoapFaultException {
        NotificationObject nObj = new NotificationObject(notification, communicationContext);
        serverDispatcher.invokeDispatcher(Direction.NOTIFICATION, interceptorRegistry, notification, nObj);
    }
}
