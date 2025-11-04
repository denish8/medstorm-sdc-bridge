package org.somda.sdc.dpws.soap.wseventing.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wseventing.SinkSubscriptionManager;
import org.somda.sdc.dpws.soap.wseventing.SourceSubscriptionManager;
import org.somda.sdc.dpws.soap.wseventing.SubscriptionManager;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.List;

/**
 * Creates {@link SubscriptionManager} instances for event source and sink side.
 */
public interface SubscriptionManagerFactory {

    // CHECKSTYLE.OFF: ParameterNumber
    /**
     * Creates a {@link SourceSubscriptionManager} instance.
     *
     * @param subscriptionManagerEpr    endpoint reference where to request subscription modification requests
     *                                  (GetStatus, Renew, Unsubscribe).
     * @param expires                   expiration duration.
     * @param notifyTo                  endpoint reference where to send notifications to.
     * @param endTo                     endpoint reference where to send end-to request to or null if none is available.
     * @param subscriptionId            the subscription id for the subscription manager.
     * @param filters                   filter objects handled by the subscription manager (depending on the filter
     *                                  dialect).
     * @param filterDialect             the filter dialect as a URI.
     * @param callerId                  the caller id or null if none is available.
     * @return a new {@link SourceSubscriptionManager} instance.
     */
    SourceSubscriptionManager createSourceSubscriptionManager(
            @Assisted("SubscriptionManager")
            EndpointReferenceType subscriptionManagerEpr,
            @Assisted Duration expires,
            @Assisted("NotifyTo") EndpointReferenceType notifyTo,
            @Assisted("EndTo") @Nullable EndpointReferenceType endTo,
            @Assisted("SubscriptionId") String subscriptionId,
            @Assisted("Filters") List<Object> filters,
            @Assisted("FilterDialect") String filterDialect,
            @Assisted("callerId") @Nullable String callerId
    );
    // CHECKSTYLE.ON: ParameterNumber

    /**
     * Creates a {@link SinkSubscriptionManager} instance.
     * <p>
     * A subscription id is assigned automatically.
     *
     * @param subscriptionManagerEpr endpoint reference where to receive subscription modification requests (GetStatus,
     *                               Renew, Unsubscribe).
     * @param expires                expiration duration.
     * @param notifyTo               endpoint reference where to receive notifications at.
     * @param endTo                  endpoint reference where to receive end-to request at.
     * @param filters                filter objects handled by the subscription manager (depending on the filter
     *                               dialect).
     * @param filterDialect          the filter dialect as a URI.
     * @return a new {@link SinkSubscriptionManager} instance.
     */
    SinkSubscriptionManager createSinkSubscriptionManager(@Assisted("SubscriptionManager")
                                                          EndpointReferenceType subscriptionManagerEpr,
                                                          @Assisted Duration expires,
                                                          @Assisted("NotifyTo") EndpointReferenceType notifyTo,
                                                          @Assisted("EndTo") EndpointReferenceType endTo,
                                                          @Assisted("Filters") List<Object> filters,
                                                          @Assisted("FilterDialect") String filterDialect);
}
