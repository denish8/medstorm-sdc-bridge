package org.somda.sdc.dpws.soap.wseventing.factory;

import com.google.inject.Inject;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.soap.SoapConstants;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.factory.SoapFaultFactory;
import org.somda.sdc.dpws.soap.model.Envelope;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingConstants;
import org.somda.sdc.dpws.soap.wseventing.WsEventingConstants;
import org.somda.sdc.dpws.soap.wseventing.model.ObjectFactory;

/**
 * Factory functions to create WS-Eventing related fault messages.
 */
public class WsEventingFaultFactory {
    private final SoapFaultFactory soapFaultFactory;
    private final ObjectFactory wseFactory;
    private final org.somda.sdc.dpws.soap.model.ObjectFactory soapFactory;

    @Inject
    WsEventingFaultFactory(SoapFaultFactory soapFaultFactory,
                           ObjectFactory wseFactory,
                           org.somda.sdc.dpws.soap.model.ObjectFactory soapFactory) {
        this.soapFaultFactory = soapFaultFactory;
        this.wseFactory = wseFactory;
        this.soapFactory = soapFactory;
    }

    /**
     * Creates DeliveryModeRequestedUnavailable fault messages.
     *
     * @return the fault message.
     * @see <a href="https://www.w3.org/Submission/WS-Eventing/#DeliveryModeRequestedUnavailable"
     * >WS-Eventing specification</a>
     */
    public SoapMessage createDeliveryModeRequestedUnavailable() {
        return soapFaultFactory.createFault(
                WsAddressingConstants.FAULT_ACTION,
                SoapConstants.SENDER,
                WsEventingConstants.DELIVERY_MODE_REQUESTED_UNAVAILABLE,
                "The requested delivery mode is not supported.",
                wseFactory.createSupportedDeliveryMode(WsEventingConstants.SUPPORTED_DELIVERY_MODE)
        );
    }

    /**
     * Creates InvalidExpirationTime fault messages.
     *
     * @return the fault message.
     * @see <a href="https://www.w3.org/Submission/WS-Eventing/#InvalidExpirationTime">WS-Eventing specification</a>
     */
    public SoapMessage createInvalidExpirationTime() {
        return soapFaultFactory.createFault(
                WsAddressingConstants.FAULT_ACTION,
                SoapConstants.SENDER,
                WsEventingConstants.INVALID_EXPIRATION_TIME,
                "The expiration time requested is invalid."
        );
    }

    /**
     * Creates UnsupportedExpirationType fault messages.
     *
     * @return the fault message.
     * @see <a href="https://www.w3.org/Submission/WS-Eventing/#UnsupportedExpirationType">WS-Eventing specification</a>
     */
    public SoapMessage createUnsupportedExpirationType() {
        return soapFaultFactory.createFault(
                WsAddressingConstants.FAULT_ACTION,
                SoapConstants.SENDER,
                WsEventingConstants.UNSUPPORTED_EXPIRATION_TYPE,
                "Only expiration durations are supported."
        );
    }

    /**
     * Creates FilteringNotSupported fault messages.
     *
     * @return the fault message.
     * @see <a href="https://www.w3.org/Submission/WS-Eventing/#FilteringNotSupported">WS-Eventing specification</a>
     */
    public SoapMessage createFilteringNotSupported() {
        return soapFaultFactory.createFault(
                WsAddressingConstants.FAULT_ACTION,
                SoapConstants.SENDER,
                WsEventingConstants.FILTERING_NOT_SUPPORTED,
                "Filtering is not supported."
        );
    }

    /**
     * Creates FilteringRequestedUnavailable fault messages.
     *
     * @return the fault message.
     * @see <a href="https://www.w3.org/Submission/WS-Eventing/#FilteringRequestedUnavailable"
     * >WS-Eventing specification</a>
     */
    public SoapMessage createFilteringRequestedUnavailable() {
        return soapFaultFactory.createFault(
                WsAddressingConstants.FAULT_ACTION,
                SoapConstants.SENDER,
                WsEventingConstants.FILTERING_REQUESTED_UNAVAILABLE,
                "The requested filter dialect is not supported",
                wseFactory.createSupportedDialect(DpwsConstants.WS_EVENTING_SUPPORTED_DIALECT)
        );
    }

    /**
     * Creates EventSourceUnableToProcess fault messages.
     *
     * @param reason a specific reason text.
     * @return the fault message.
     * @see <a href="https://www.w3.org/Submission/WS-Eventing/#EventSourceUnableToProcess"
     * >WS-Eventing specification</a>
     */
    public SoapMessage createEventSourceUnableToProcess(String reason) {
        return soapFaultFactory.createFault(
                WsAddressingConstants.FAULT_ACTION,
                SoapConstants.SENDER,
                WsEventingConstants.EVENT_SOURCE_UNABLE_TO_PROCESS,
                reason
        );
    }

    /**
     * Creates UnableToRenew fault messages.
     *
     * @param reason a specific reason text.
     * @return the fault message.
     * @see <a href="https://www.w3.org/Submission/WS-Eventing/#UnableToRenew">WS-Eventing specification</a>
     */
    public SoapMessage createUnableToRenew(String reason) {
        return soapFaultFactory.createFault(
                WsAddressingConstants.FAULT_ACTION,
                SoapConstants.RECEIVER,
                WsEventingConstants.UNABLE_TO_RENEW,
                reason
        );
    }

    /**
     * Creates InvalidMessage fault messages.
     *
     * @param reason a specific reason text.
     * @param invalidMessage the invalid message to put to the fault.
     * @return the fault message.
     * @see <a href="https://www.w3.org/Submission/WS-Eventing/#InvalidMessage">WS-Eventing specification</a>
     */
    public SoapMessage createInvalidMessage(String reason, Envelope invalidMessage) {
        return soapFaultFactory.createFault(
                WsAddressingConstants.FAULT_ACTION,
                SoapConstants.RECEIVER,
                WsEventingConstants.INVALID_MESSAGE,
                "The message is not valid and cannot be processed.",
                soapFactory.createEnvelope(invalidMessage)
        );
    }
}
