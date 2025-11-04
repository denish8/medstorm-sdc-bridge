package org.somda.sdc.dpws.soap.wseventing.model;

import org.somda.sdc.dpws.soap.wseventing.WsEventingConstants;

/**
 * WS-Eventing SubscriptionEnd status enumeration.
 *
 * @see <a href="https://www.w3.org/Submission/2006/SUBM-WS-Eventing-20060315/#Subscription_End">Subscription End</a>
 */
public enum WsEventingStatus {
    STATUS_SOURCE_DELIVERY_FAILURE(WsEventingConstants.STATUS_SOURCE_DELIVERY_FAILURE),
    STATUS_SOURCE_SHUTTING_DOWN(WsEventingConstants.STATUS_SOURCE_SHUTTING_DOWN),
    STATUS_SOURCE_CANCELLING(WsEventingConstants.STATUS_SOURCE_CANCELLING),;

    private final String status;

    WsEventingStatus(String status) {
        this.status = status;
    }

    public String getUri() {
       return status;
    }
}
