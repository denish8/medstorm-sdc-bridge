package org.somda.sdc.dpws.service;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.guice.ClientSpecific;
import org.somda.sdc.dpws.model.HostedServiceType;
import org.somda.sdc.dpws.soap.NotificationSink;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.factory.NotificationSinkFactory;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingServerInterceptor;
import org.somda.sdc.dpws.soap.wseventing.EventSink;
import org.somda.sdc.dpws.soap.wseventing.SubscribeResult;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Default implementation of {@linkplain HostedServiceProxy}.
 */
public class HostedServiceProxyImpl implements HostedServiceProxy, EventSinkAccess {

    private final EventSink eventSink;
    private final NotificationSinkFactory notificationSinkFactory;
    private final WsAddressingServerInterceptor wsAddressingServerInterceptor;

    private final HostedServiceType hostedServiceType;
    private final RequestResponseClient requestResponseClient;
    private final String activeEprAddress;

    @AssistedInject
    HostedServiceProxyImpl(@Assisted HostedServiceType hostedServiceType,
                           @Assisted RequestResponseClient requestResponseClient,
                           @Assisted String activeEprAddress,
                           @Assisted EventSink eventSink,
                           NotificationSinkFactory notificationSinkFactory,
                           @ClientSpecific WsAddressingServerInterceptor wsAddressingServerInterceptor) {
        this.eventSink = eventSink;
        this.notificationSinkFactory = notificationSinkFactory;
        this.wsAddressingServerInterceptor = wsAddressingServerInterceptor;
        this.hostedServiceType = hostedServiceType.createCopy();
        this.requestResponseClient = requestResponseClient;
        this.activeEprAddress = activeEprAddress;
    }

    @Override
    public HostedServiceType getType() {
        return hostedServiceType.createCopy();
    }

    @Override
    public RequestResponseClient getRequestResponseClient() {
        return requestResponseClient;
    }

    @Override
    public EventSinkAccess getEventSinkAccess() {
        return this;
    }

    @Override
    public String getActiveEprAddress() {
        return activeEprAddress;
    }

    @Override
    public synchronized void register(Interceptor interceptor) {
        requestResponseClient.register(interceptor);
    }

    @Override
    public synchronized SoapMessage sendRequestResponse(SoapMessage request)
            throws SoapFaultException, MarshallingException, TransportException, InterceptorException {
        return requestResponseClient.sendRequestResponse(request);
    }

    @Override
    public ListenableFuture<SubscribeResult> subscribe(List<String> actions,
                                                       @Nullable Duration expires,
                                                       Interceptor notificationSink) {
        final NotificationSink notifications = notificationSinkFactory
                .createNotificationSink(wsAddressingServerInterceptor);
        notifications.register(notificationSink);
        return eventSink.subscribe(
                DpwsConstants.WS_EVENTING_SUPPORTED_DIALECT,
                Collections.singletonList(implodeUriList(actions)),
                expires,
                notifications
        );
    }

    private String implodeUriList(List<String> actionUris) {
        StringBuilder sb = new StringBuilder();
        actionUris.forEach(s -> {
            sb.append(s);
            sb.append(" ");
        });
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    @Override
    public ListenableFuture<SubscribeResult> subscribe(String filterDialect,
                                                       List<Object> filters,
                                                       @Nullable Duration expires,
                                                       Interceptor notificationSink) {
        final NotificationSink notifications = notificationSinkFactory
                .createNotificationSink(wsAddressingServerInterceptor);
        notifications.register(notificationSink);
        return eventSink.subscribe(filterDialect, filters, expires, notifications);
    }

    @Override
    public ListenableFuture<Duration> renew(String subscriptionId, Duration expires) {
        return eventSink.renew(subscriptionId, expires);
    }

    @Override
    public ListenableFuture<Duration> getStatus(String subscriptionId) {
        return eventSink.getStatus(subscriptionId);
    }

    @Override
    public ListenableFuture<?> unsubscribe(String subscriptionId) {
        return eventSink.unsubscribe(subscriptionId);
    }

    @Override
    public void unsubscribeAll() {
        eventSink.unsubscribeAll();
    }
}
