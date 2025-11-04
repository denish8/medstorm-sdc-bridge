package org.somda.sdc.dpws.soap.wseventing.event;

import org.somda.sdc.common.event.AbstractEventMessage;
import org.somda.sdc.dpws.soap.wseventing.SourceSubscriptionManager;
import org.somda.sdc.dpws.soap.wseventing.helper.SubscriptionRegistry;

/**
 * Event message to signalize that a {@link SourceSubscriptionManager} instance was added to a
 * {@link SubscriptionRegistry} instance.
 */
public class SubscriptionAddedMessage extends AbstractEventMessage<SourceSubscriptionManager> {
    /**
     * Constructor.
     *
     * @param payload the subscription manager that has been added.
     */
    public SubscriptionAddedMessage(SourceSubscriptionManager payload) {
        super(payload);
    }
}
