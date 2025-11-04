package org.somda.sdc.dpws.soap.wsdiscovery;

import com.google.inject.Inject;
import org.somda.sdc.dpws.soap.wsdiscovery.model.AppSequenceType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ObjectFactory;
import org.somda.sdc.common.util.JaxbUtil;

import java.util.List;
import java.util.Optional;

/**
 * Mapper to map between WS-Discovery JAXB elements and {@link WsDiscoveryHeader}.
 */
public class WsDiscoveryMapper {
    private final JaxbUtil jaxbUtil;
    private final ObjectFactory wsdFactory;

    @Inject
    WsDiscoveryMapper(JaxbUtil jaxbUtil, ObjectFactory wsdFactory) {
        this.jaxbUtil = jaxbUtil;
        this.wsdFactory = wsdFactory;
    }

    /**
     * The mapper function that takes a convenience WS-Discovery header and adds it to a list of JAXB objects.
     *
     * @param src  the WS-Discovery source information.
     * @param dest the list of objects where to add the mapped JAXB element (typically the list of headers).
     */
    public void mapToJaxbSoapHeader(WsDiscoveryHeader src, List<Object> dest) {
        src.getAppSequence().ifPresent(appSequenceType -> dest.add(wsdFactory.createAppSequence(appSequenceType)));
    }

    /**
     * The mapper function that takes a list of JAXB objects and populates the convenience WS-Discovery header.
     *
     * @param src  the list of objects where to get the WS-Discovery header information from.
     * @param dest the WS-Discovery mapper destination.
     */
    public void mapFromJaxbSoapHeader(List<Object> src, WsDiscoveryHeader dest) {
        src.forEach(o -> {
            Optional<AppSequenceType> appSeq;
            appSeq = jaxbUtil.extractElement(o, WsDiscoveryConstants.APP_SEQUENCE);
            if (appSeq.isPresent() && dest.getAppSequence().isEmpty()) {
                dest.setAppSequence(appSeq.get());
            }
        });
    }
}
