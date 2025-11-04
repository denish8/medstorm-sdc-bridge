package org.somda.sdc.dpws.service;

import org.somda.sdc.dpws.model.HostedServiceType;
import org.somda.sdc.dpws.soap.RequestResponseClient;

/**
 * Hosted service proxy of a client.
 */
public interface HostedServiceProxy extends RequestResponseClient {
    /**
     * Gets the hosted service metadata requestable via WS-TransferGet.
     *
     * @return a copy of the hosted service metadata received from the network.
     */
    HostedServiceType getType();

    /**
     * Gets the request-response client used to send request messages and receive their response.
     * <p>
     * <em>Hint: the {@linkplain HostingServiceProxy} itself implements {@link RequestResponseClient}, which is the same
     * as using the return value of this function.</em>
     *
     * @return the {@link RequestResponseClient} instance capable of requesting this hosted service.
     */
    RequestResponseClient getRequestResponseClient();

    /**
     * Gets the event sink that can be used to subscribe and manage subscriptions of the hosted service.
     * <p>
     * <em>Attention: the event sink only works if the underlying hosted service acts as an event source.
     * It is up to the user check the availability in advance, otherwise calls  will end up in SOAP faults.</em>

     * @return the {@link EventSinkAccess} instance.
     */
    EventSinkAccess getEventSinkAccess();

    /**
     * Gets the physical address that is actively being used to send requests.
     * <p>
     * A hosted service can have different physical addresses in order to be accessible.
     * The one that is returned with this function is the one that was used to initially resolve metadata
     * (GetMetadata request).
     *
     * @return the currently active EPR address.
     */
    String getActiveEprAddress();
}
