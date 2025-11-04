package org.somda.sdc.dpws.soap.wsaddressing.factory;

import com.google.inject.Inject;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapConstants;
import org.somda.sdc.dpws.soap.factory.SoapFaultFactory;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingConstants;

import javax.xml.namespace.QName;

/**
 * Factory to create WS-Addressing fault messages.
 *
 * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-soap-20060509/#faults">Faults</a>
 */
public class WsAddressingFaultFactory {
    private final SoapFaultFactory soapFaultFactory;

    @Inject
    WsAddressingFaultFactory(SoapFaultFactory soapFaultFactory) {
        this.soapFaultFactory = soapFaultFactory;
    }

    /**
     * Creates an action-not-supported fault.
     *
     * @param action the action URI that is not supported.
     * @return a {@link SoapMessage} that encapsulates the fault.
     */
    public SoapMessage createActionNotSupported(String action) {
        return soapFaultFactory.createFault(
                WsAddressingConstants.FAULT_ACTION,
                SoapConstants.RECEIVER,
                WsAddressingConstants.ACTION_NOT_SUPPORTED,
                String.format("The %s cannot be processed at the receiver", action),
                action);
    }

    /**
     * Creates a message-information-header-required fault.
     *
     * @param missingHeaderQName qualified name of the missing header element.
     * @return a {@link SoapMessage} that encapsulates the fault.
     */
    public SoapMessage createMessageInformationHeaderRequired(QName missingHeaderQName) {
        return soapFaultFactory.createFault(
                WsAddressingConstants.FAULT_ACTION,
                SoapConstants.RECEIVER,
                WsAddressingConstants.MESSAGE_ADDRESSING_HEADER_REQUIRED,
                "A required header representing a Message Addressing Property is not present",
                missingHeaderQName.toString()
        );
    }
}
