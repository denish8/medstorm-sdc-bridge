package org.somda.sdc.dpws.soap;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.interception.ClientDispatcher;
import org.somda.sdc.dpws.soap.interception.Direction;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.dpws.soap.interception.InterceptorRegistry;
import org.somda.sdc.dpws.soap.interception.NotificationCallback;
import org.somda.sdc.dpws.soap.interception.NotificationObject;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingClientInterceptor;

/**
 * Default implementation of {@linkplain NotificationSource}.
 */
public class NotificationSourceImpl implements NotificationSource {

    private final InterceptorRegistry interceptorRegistry;
    private final NotificationCallback networkCallback;
    private final ClientDispatcher clientDispatcher;

    @Inject
    NotificationSourceImpl(@Assisted NotificationCallback networkCallback,
                           ClientDispatcher clientDispatcher,
                           InterceptorRegistry interceptorRegistry,
                           WsAddressingClientInterceptor wsaClientInterceptor) {
        this.networkCallback = networkCallback;
        this.clientDispatcher = clientDispatcher;
        this.interceptorRegistry = interceptorRegistry;

        register(wsaClientInterceptor);
    }

    @Override
    public void register(Interceptor interceptor) {
        interceptorRegistry.addInterceptor(interceptor);
    }

    @Override
    public void sendNotification(SoapMessage notification) throws MarshallingException, TransportException,
            InterceptorException {
        NotificationObject nObj = new NotificationObject(notification);
        clientDispatcher.invokeDispatcher(Direction.NOTIFICATION, interceptorRegistry, notification, nObj);
        networkCallback.onNotification(notification);
    }
}
