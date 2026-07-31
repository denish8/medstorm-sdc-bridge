package org.somda.sdc.dpws.soap.wseventing;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNullableByDefault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.factory.NotificationSourceFactory;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wseventing.model.ObjectFactory;

@ParametersAreNullableByDefault
public final class PatchedSourceSubscriptionManagerImpl extends SourceSubscriptionManagerImpl {

    private static final Logger log = LoggerFactory.getLogger(PatchedSourceSubscriptionManagerImpl.class);

    @AssistedInject
    public PatchedSourceSubscriptionManagerImpl(
            @Assisted("SubscriptionManager") EndpointReferenceType subscriptionManager,
            @Assisted Duration expires,
            @Assisted("NotifyTo") EndpointReferenceType notifyTo,
            @Assisted("EndTo") @Nullable EndpointReferenceType endTo,
            @Assisted("SubscriptionId") String subscriptionId,
            @Assisted("Filters") List<Object> filters,
            @Assisted("FilterDialect") String filterDialect,
            @Assisted("callerId") @Nullable String callerId,
            @Named("SoapConfig.NotificationQueueCapacity") Integer notificationQueueCapacity,
            NotificationSourceFactory notificationSourceFactory,
            TransportBindingFactory transportBindingFactory,
            WsAddressingUtil wsaUtil,
            ObjectFactory wseFactory,
            SoapUtil soapUtil,
            @Named("Common.InstanceIdentifier") String instanceIdentifier
    ) {
        super(
                subscriptionManager,
                expires,
                notifyTo,
                endTo,
                subscriptionId,
                filters,
                filterDialect,
                normalizeCallerId(callerId),
                notificationQueueCapacity,
                notificationSourceFactory,
                transportBindingFactory,
                wsaUtil,
                wseFactory,
                soapUtil,
                instanceIdentifier
        );

        log.warn("[PATCH] Created PatchedSourceSubscriptionManagerImpl callerId='{}' endToNull={}",
                normalizeCallerId(callerId), endTo == null);
    }

    private static String normalizeCallerId(@Nullable String callerId) {
        if (callerId == null) return "anonymous";
        String t = callerId.trim();
        return t.isEmpty() ? "anonymous" : t;
    }
}
