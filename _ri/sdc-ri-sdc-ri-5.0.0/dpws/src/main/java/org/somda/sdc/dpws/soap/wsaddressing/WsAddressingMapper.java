package org.somda.sdc.dpws.soap.wsaddressing;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.JaxbUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.somda.sdc.dpws.soap.wsaddressing.model.ObjectFactory;
import org.somda.sdc.dpws.soap.wsaddressing.model.RelatesToType;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mapper to map between WS-Addressing JAXB elements and {@link WsAddressingHeader}.
 */
public class WsAddressingMapper {
    private static final Logger LOG = LogManager.getLogger(WsAddressingMapper.class);
    private final JaxbUtil jaxbUtil;
    private final ObjectFactory wsaFactory;
    private final WsAddressingUtil wsaUtil;
    private final Logger instanceLogger;

    @Inject
    WsAddressingMapper(JaxbUtil jaxbUtil,
                       ObjectFactory wsaFactory,
                       WsAddressingUtil wsaUtil,
                       @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.jaxbUtil = jaxbUtil;
        this.wsaFactory = wsaFactory;
        this.wsaUtil = wsaUtil;
    }

    /**
     * The mapper function that takes a convenience WS-Addressing header and adds it to a list of JAXB objects.
     *
     * @param src  The WS-Addressing source information.
     * @param dest The list of objects where to add the mapped JAXB element (typically the list of headers).
     */
    public void mapToJaxbSoapHeader(WsAddressingHeader src, List<Object> dest) {
        src.getAction().ifPresent(attributedURIType ->
                addWithDuplicateCheck(wsaFactory.createAction(attributedURIType), dest));
        src.getMessageId().ifPresent(attributedURIType ->
                addWithDuplicateCheck(wsaFactory.createMessageID(attributedURIType), dest));
        src.getTo().ifPresent(attributedURIType ->
                addWithDuplicateCheck(wsaFactory.createTo(attributedURIType), dest));
        src.getRelatesTo().ifPresent(attributedURIType -> {
            RelatesToType relatesToType = wsaFactory.createRelatesToType();
            relatesToType.setValue(attributedURIType.getValue());
            addWithDuplicateCheck(wsaFactory.createRelatesTo(relatesToType), dest);
        });
        src.getMappedReferenceParameters().ifPresent(referenceParameters -> {
            referenceParameters.forEach(param -> {
                param.setAttributeNS(
                        WsAddressingConstants.NAMESPACE,
                        WsAddressingConstants.NAMESPACE_PREFIX + ":"
                                + WsAddressingConstants.IS_REFERENCE_PARAMETER.getLocalPart(),
                        "true");
                addWithDuplicateCheck(param, dest);
            });
        });
    }

    /**
     * Duplicate check for adding new header elements.
     * <p>
     * Checks for duplicate {@linkplain JAXBElement} instances inside the dest list. When a duplicate is found, i.e.
     * the name, type and scope are equal, it is replaced with the new value.
     * <p>
     * {@linkplain Element} is not checked for duplicates, as they are only passed through for reference parameters.
     *
     * @param obj to add to the list
     * @param dest to add the new entry to
     */
    private void addWithDuplicateCheck(Object obj, List<Object> dest) {
        if (obj instanceof JAXBElement) {
            var jaxbObj = (JAXBElement<?>) obj;
            for (Object element : dest) {
                if (element instanceof JAXBElement) {

                    var jaxbElement = (JAXBElement<?>) element;

                    if (jaxbObj.getName().equals(jaxbElement.getName())
                            && jaxbObj.getDeclaredType().equals(jaxbElement.getDeclaredType())
                            && jaxbObj.getScope().equals(jaxbElement.getScope())) {
                        instanceLogger.warn("Envelope header already contains entry for JAXBElement {}."
                                        + "Removing previously set element with value {} and replacing it with {}",
                                obj, jaxbElement.getValue(), jaxbObj.getValue());
                        dest.remove(jaxbElement);
                        break;
                    }
                }
            }
        }
        dest.add(obj);
    }

    /**
     * The mapper function that takes a list of JAXB objects and populates the convenience WS-Addressing header.
     *
     * @param src  The list of objects where to get the WS-Addressing header information from.
     * @param dest The WS-Addressing mapper destination.
     */
    public void mapFromJaxbSoapHeader(List<Object> src, WsAddressingHeader dest) {
        List<Element> referenceParameters = new ArrayList<>();
        src.forEach(jaxbObject -> {
            Optional<AttributedURIType> uri;

            uri = jaxbUtil.extractElement(jaxbObject, WsAddressingConstants.ACTION);
            if (uri.isPresent() && dest.getAction().isEmpty()) {
                dest.setAction(uri.get());
            }

            uri = jaxbUtil.extractElement(jaxbObject, WsAddressingConstants.MESSAGE_ID);
            if (uri.isPresent() && dest.getMessageId().isEmpty()) {
                dest.setMessageId(uri.get());
            }

            uri = jaxbUtil.extractElement(jaxbObject, WsAddressingConstants.TO);
            if (uri.isPresent() && dest.getTo().isEmpty()) {
                dest.setTo(uri.get());
            }

            Optional<RelatesToType> rt = jaxbUtil.extractElement(jaxbObject, WsAddressingConstants.RELATES_TO);
            if (rt.isPresent() && dest.getRelatesTo().isEmpty()) {
                RelatesToType rtt = wsaUtil.createRelatesToType(rt.get().getValue());
                rtt.setRelationshipType(rt.get().getRelationshipType());
                dest.setRelatesTo(rtt);
            }

            if (jaxbObject instanceof Element) {
                // verify element is reference parameter
                var elem = (Element) jaxbObject;
                var isRefParm = elem.hasAttributeNS(
                        WsAddressingConstants.IS_REFERENCE_PARAMETER.getNamespaceURI(),
                        WsAddressingConstants.IS_REFERENCE_PARAMETER.getLocalPart()
                );
                if (isRefParm) {
                    instanceLogger.debug(
                            "Incoming message contained reference parameter element ({}:{})",
                            elem.getNamespaceURI(), elem.getTagName()
                    );
                    referenceParameters.add(elem);
                }
            }
        });
        if (!referenceParameters.isEmpty()) {
            dest.setMappedReferenceParameters(referenceParameters);
        }
    }
}
