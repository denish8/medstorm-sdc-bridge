package org.somda.sdc.dpws.soap.exception;

import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.model.Fault;
import org.somda.sdc.dpws.soap.model.Reasontext;
import org.somda.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.somda.sdc.dpws.soap.wsaddressing.model.RelatesToType;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBElement;
import java.util.List;

import static org.somda.sdc.dpws.soap.wsaddressing.WsAddressingConstants.UNSPECIFIED_MESSAGE;

/**
 * Defines an exception that is supposed to be used to convey SOAP fault information.
 *
 * @see <a href="https://www.w3.org/TR/2007/REC-soap12-part1-20070427/#soapfault">SOAP Fault</a>
 */
public class SoapFaultException extends Exception {
    private final SoapMessage faultMessage;
    private final Fault fault;


    /**
     * Constructor that requires an wrapped SOAP fault message and a messageId.
     *
     * @param faultMessage SOAP message that shall include a {@linkplain JAXBElement} with {@link Fault} body.
     *                     Otherwise, a {@linkplain ClassCastException} is thrown.
     * @param messageId    of the request, to properly set the relatesTo field.
     */
    @SuppressWarnings("unchecked")
    public SoapFaultException(SoapMessage faultMessage, @Nullable AttributedURIType messageId) {
        this.faultMessage = faultMessage;
        this.fault = ((JAXBElement<Fault>) faultMessage.getOriginalEnvelope().getBody().getAny().get(0)).getValue();
        setRelatesTo(messageId);
    }

    /**
     * Constructor that requires an wrapped SOAP fault message.
     *
     * @param faultMessage SOAP message that shall include a {@linkplain JAXBElement} with {@link Fault} body.
     *                     Otherwise, a {@linkplain ClassCastException} is thrown.
     */
    public SoapFaultException(SoapMessage faultMessage) {
        this(faultMessage, (AttributedURIType) null);
    }

    /**
     * Constructor that requires a wrapped SOAP fault message plus a nested cause and a messageId.
     *
     * @param faultMessage SOAP message that shall include a {@linkplain JAXBElement} with {@link Fault} body.
     *                     Otherwise, a {@linkplain ClassCastException} is thrown.
     * @param throwable    extended information, e.g. transport layer info.
     * @param messageId    of the request, to properly set the relatesTo field.
     */
    @SuppressWarnings("unchecked")
    public SoapFaultException(SoapMessage faultMessage, Throwable throwable, @Nullable AttributedURIType messageId) {
        super(throwable);
        this.faultMessage = faultMessage;
        this.fault = ((JAXBElement<Fault>) faultMessage.getOriginalEnvelope().getBody().getAny().get(0)).getValue();
        setRelatesTo(messageId);
    }

    /**
     * Constructor that requires a wrapped SOAP fault message plus a nested cause.
     *
     * @param faultMessage SOAP message that shall include a {@linkplain JAXBElement} with {@link Fault} body.
     *                     Otherwise, a {@linkplain ClassCastException} is thrown.
     * @param throwable    extended information, e.g. transport layer info.
     */
    public SoapFaultException(SoapMessage faultMessage, Throwable throwable) {
        this(faultMessage, throwable, null);
    }

    private void setRelatesTo(@Nullable AttributedURIType messageId) {
        if (faultMessage.getWsAddressingHeader().getRelatesTo().isEmpty()) {
            final RelatesToType unspecifiedMessageUri = new RelatesToType();
            if (messageId != null) {
                unspecifiedMessageUri.setValue(messageId.getValue());
            } else {
                unspecifiedMessageUri.setValue(UNSPECIFIED_MESSAGE);
            }
            faultMessage.getWsAddressingHeader().setRelatesTo(unspecifiedMessageUri);
        }
    }

    public SoapMessage getFaultMessage() {
        return faultMessage;
    }

    /**
     * Gets the SOAP fault information.
     *
     * @return {@link Fault} that is encapsulated in the {@link SoapMessage} wrapped by this exception.
     */
    public Fault getFault() {
        return fault;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(getFault().getCode().getValue().toString());
        List<Reasontext> text = getFault().getReason().getText();
        if (!text.isEmpty()) {
            sb.append(": ");
            sb.append(text.get(0).getValue());
        }
        return sb.toString();
    }
}
