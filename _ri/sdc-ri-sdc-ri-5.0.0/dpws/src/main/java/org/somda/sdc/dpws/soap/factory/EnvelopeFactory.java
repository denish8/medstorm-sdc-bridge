package org.somda.sdc.dpws.soap.factory;

import com.google.inject.Inject;
import org.somda.sdc.dpws.soap.model.Body;
import org.somda.sdc.dpws.soap.model.Envelope;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.somda.sdc.dpws.soap.wsaddressing.model.ObjectFactory;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Convenience factory to create SOAP envelopes in accordance with the JAXB SOAP model.
 */
public class EnvelopeFactory {
    private final ObjectFactory wsaFactory;
    private final org.somda.sdc.dpws.soap.model.ObjectFactory soapFactory;
    private final WsAddressingUtil wsaUtil;

    @Inject
    EnvelopeFactory(ObjectFactory wsaFactory,
                    org.somda.sdc.dpws.soap.model.ObjectFactory soapFactory,
                    WsAddressingUtil wsaUtil) {
        this.wsaFactory = wsaFactory;
        this.soapFactory = soapFactory;
        this.wsaUtil = wsaUtil;
    }

    /**
     * Creates an envelope with given wsa:Action attribute and body.
     *
     * @param wsaAction the action to be used for the envelope.
     * @param firstBodyChild the message body.
     * @return an {@link Envelope} instance.
     */
    public Envelope createEnvelope(String wsaAction, @Nullable Object firstBodyChild) {
        Envelope envelope = createEnvelope(firstBodyChild);
        envelope.getHeader().getAny().add(wsaFactory.createAction(createUri(wsaAction)));
        return envelope;
    }

    /**
     * Creates an envelope with given wsa:Action attribute, wsa:To attribute, and body.
     *
     * @param wsaAction the action to be used for the envelope.
     * @param firstBodyChild the message body.
     * @param wsaTo the wsa:To element content.
     * @return an {@link Envelope} instance.
     */
    public Envelope createEnvelope(String wsaAction, String wsaTo, @Nullable Object firstBodyChild) {
        Envelope envelope = createEnvelope(firstBodyChild);
        envelope.getHeader().getAny().add(wsaFactory.createAction(createUri(wsaAction)));
        envelope.getHeader().getAny().add(wsaFactory.createTo(createUri(wsaTo)));
        return envelope;
    }

    /**
     * Creates an envelope with given JAXB body element.
     * <p>
     * Any header fields will be left empty.
     *
     * @param firstBodyChild the message body.
     * @return an {@link Envelope} instance.
     */
    public Envelope createEnvelope(@Nullable Object firstBodyChild) {
        Envelope envelope = soapFactory.createEnvelope();
        envelope.setHeader(soapFactory.createHeader());
        envelope.setBody(soapFactory.createBody());
        Optional.ofNullable(firstBodyChild).ifPresent(jaxbElement -> envelope.getBody().getAny().add(jaxbElement));
        return envelope;
    }

    /**
     * Creates an empty SOAP envelope with existing empty body and header.
     * @return an empty {@link Envelope} instance.
     */
    public Envelope createEnvelope() {
        return createEnvelope(null);
    }

    /**
     * Creates a SOAP envelope from an existing body.
     * @param body the message body.
     * @return an {@link Envelope} instance.
     */
    public Envelope createEnvelopeFromBody(Body body) {
        Envelope envelope = createEnvelope(null);
        envelope.setBody(body);
        return envelope;
    }

    private AttributedURIType createUri(String uri) {
        return wsaUtil.createAttributedURIType(uri);
    }
}
