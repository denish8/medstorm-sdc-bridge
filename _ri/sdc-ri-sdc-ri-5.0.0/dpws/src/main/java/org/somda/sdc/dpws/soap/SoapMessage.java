package org.somda.sdc.dpws.soap;


import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.dpws.soap.factory.EnvelopeFactory;
import org.somda.sdc.dpws.soap.model.Envelope;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingHeader;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingMapper;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryHeader;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryMapper;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelling of a SOAP Message with convenient access to different headers.
 * <p>
 * {@linkplain SoapMessage} is a wrapper around an {@linkplain Envelope} which provides
 * mappers for certain header elements in Ws-Addressing and Ws-Discovery.
 * When additional headers are required, the original {@linkplain Envelope} can be accessed and
 * modified through {@linkplain #getOriginalEnvelope()}.
 * <em>There are no duplicate checks for any headers covered through the mappers, care is required.</em>
 * @see WsAddressingHeader
 * @see WsDiscoveryHeader
 */
public class SoapMessage {
    private final WsDiscoveryHeader wsdHeader;
    private final WsAddressingHeader wsaHeader;
    private final Envelope envelope;
    private final WsAddressingMapper wsaMapper;
    private final WsDiscoveryMapper wsdMapper;
    private final EnvelopeFactory envelopeFactory;

    @AssistedInject
    SoapMessage(@Assisted Envelope envelope,
                Provider<WsAddressingHeader> wsaHeaderProvider,
                WsAddressingMapper wsaMapper,
                Provider<WsDiscoveryHeader> wsdHeaderProvider,
                WsDiscoveryMapper wsdMapper,
                EnvelopeFactory envelopeFactory) {
        this.envelope = envelope;
        this.wsaMapper = wsaMapper;
        this.wsdMapper = wsdMapper;
        this.envelopeFactory = envelopeFactory;

        List<Object> tmpHeaderList = new ArrayList<>();
        if (envelope.getHeader() != null && envelope.getHeader().getAny() != null) {
            tmpHeaderList = envelope.getHeader().getAny();
        }

        wsaHeader = wsaHeaderProvider.get();
        wsaMapper.mapFromJaxbSoapHeader(tmpHeaderList, wsaHeader);

        wsdHeader = wsdHeaderProvider.get();
        wsdMapper.mapFromJaxbSoapHeader(tmpHeaderList, wsdHeader);
    }

    public WsAddressingHeader getWsAddressingHeader() {
        return wsaHeader;
    }

    public WsDiscoveryHeader getWsDiscoveryHeader() {
        return wsdHeader;
    }

    /**
     * Gets the original envelope.
     *
     * @return always returns the envelope as passed to the constructor.
     * Information that is stored in convenience headers are not synchronized with this envelope.
     */
    public Envelope getOriginalEnvelope() {
        return envelope;
    }

    /**
     * Gets the envelope that includes mapped headers.
     *
     * @return new envelope with mapped convenience headers as well as headers
     * and the body reference from {@link #getOriginalEnvelope()}.
     */
    public Envelope getEnvelopeWithMappedHeaders() {
        Envelope mappedEnv = envelopeFactory.createEnvelopeFromBody(envelope.getBody());
        List<Object> tmpHeaderList = mappedEnv.getHeader().getAny();

        // add all previous headers
        mappedEnv.getHeader().getAny().addAll(envelope.getHeader().getAny());

        wsaMapper.mapToJaxbSoapHeader(wsaHeader, tmpHeaderList);
        wsdMapper.mapToJaxbSoapHeader(wsdHeader, tmpHeaderList);

        return mappedEnv;
    }

    /**
     * Checks if a SOAP message is a fault or not.
     *
     * @return true if it is a fault, false otherwise.
     */
    public boolean isFault() {
        if (getOriginalEnvelope().getBody().getAny().size() == 1) {
            Object obj = getOriginalEnvelope().getBody().getAny().get(0);
            if (JAXBElement.class.isAssignableFrom(obj.getClass())) {
                var jaxbElem = (JAXBElement) obj;
                return jaxbElem.getName().equals(SoapConstants.FAULT);
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return SoapDebug.getBrief(this);
    }
}
