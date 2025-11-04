package org.somda.sdc.dpws.soap.wsaddressing;

import org.somda.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.somda.sdc.dpws.soap.wsaddressing.model.RelatesToType;
import org.w3c.dom.Element;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;

/**
 * Convenience class to access WS-Addressing header information.
 *
 * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-core-20060509/#msgaddrprops">Message Addressing Properties</a>
 */
public class WsAddressingHeader {
    private AttributedURIType action;
    private AttributedURIType messageId;
    private AttributedURIType to;
    private RelatesToType relatesTo;
    private Collection<Element> referenceParameters;

    public Optional<AttributedURIType> getAction() {
        return Optional.ofNullable(action);
    }

    /**
     * Sets the wsa:Action element.
     * @param action to set
     */
    public void setAction(@Nullable AttributedURIType action) {
        this.action = action;
    }

    public Optional<AttributedURIType> getMessageId() {
        return Optional.ofNullable(messageId);
    }

    /**
     * Sets the wsa:MessageID element.
     * @param messageId to set
     */
    public void setMessageId(@Nullable AttributedURIType messageId) {
        this.messageId = messageId;
    }

    public Optional<AttributedURIType> getTo() {
        return Optional.ofNullable(to);
    }

    /**
     * Sets the wsa:To element.
     * @param to to set
     */
    public void setTo(@Nullable AttributedURIType to) {
        this.to = to;
    }

    public Optional<RelatesToType> getRelatesTo() {
        return Optional.ofNullable(relatesTo);
    }

    /**
     * Sets the wsa:RelatesTo element.
     * @param relatesTo to set
     */
    public void setRelatesTo(@Nullable RelatesToType relatesTo) {
        this.relatesTo = relatesTo;
    }

    public Optional<Collection<Element>> getMappedReferenceParameters() {
        return Optional.ofNullable(referenceParameters);
    }

    /**
     * Attaches reference parameters to the SOAP Header.
     * <p>
     * These will be attached to the header directly, and will have the attribute
     * "IsReferenceParameter='true'" added. <b>Make sure you only add elements which aren't simple types
     * and allow adding attributes!</b>
     *
     * @param referenceParameters Elements which will be attached to the SOAP header
     */
    public void setMappedReferenceParameters(Collection<Element> referenceParameters) {
        this.referenceParameters = referenceParameters;
    }
}
