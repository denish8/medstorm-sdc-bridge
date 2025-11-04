package org.somda.sdc.dpws.soap.wsaddressing;

import com.google.inject.Inject;
import org.somda.sdc.dpws.soap.wsaddressing.model.AttributedQNameType;
import org.somda.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wsaddressing.model.ObjectFactory;
import org.somda.sdc.dpws.soap.wsaddressing.model.RelatesToType;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import java.net.URI;
import java.util.Optional;

/**
 * Utility class for WS-Addressing related interaction.
 */
public class WsAddressingUtil {
    private final ObjectFactory wsaFactory;

    @Inject
    WsAddressingUtil(ObjectFactory wsaFactory) {
        this.wsaFactory = wsaFactory;
    }

    /**
     * Shorthand method to create an {@link AttributedURIType} from a simple string.
     *
     * @param uri the URI as string.
     * @return an {@link AttributedURIType} instance.
     */
    public AttributedURIType createAttributedURIType(String uri) {
        AttributedURIType attributedURIType = wsaFactory.createAttributedURIType();
        attributedURIType.setValue(uri);
        return attributedURIType;
    }

    /**
     * Shorthand method to create an {@linkplain AttributedQNameType} from a QName.
     *
     * @param qName the {@link QName} to add.
     * @return an {@link AttributedQNameType} instance.
     */
    public AttributedQNameType createAttributedQNameType(QName qName) {
        var attributedQNameType = wsaFactory.createAttributedQNameType();
        attributedQNameType.setValue(qName);
        return attributedQNameType;
    }

    /**
     * Shorthand method to create an {@link AttributedURIType} from an URI.
     *
     * @param uri the URI.
     * @return an {@link AttributedURIType} instance.
     */
    public AttributedURIType createAttributedURIType(URI uri) {
        return createAttributedURIType(uri.toString());
    }

    /**
     * Shorthand method to create an {@link RelatesToType} from a simple String.
     *
     * @param uri the URI as string.
     * @return an {@link RelatesToType} instance.
     */
    public RelatesToType createRelatesToType(String uri) {
        RelatesToType relatesToType = wsaFactory.createRelatesToType();
        relatesToType.setValue(uri);
        return relatesToType;
    }

    /**
     * Shorthand method to create an {@link RelatesToType} from an AttributedURIType.
     *
     * @param attributedURIType the URI as AttributedURIType.
     * @return an {@link RelatesToType} instance.
     */
    public RelatesToType createRelatesToType(@Nullable AttributedURIType attributedURIType) {
        String msgId = attributedURIType != null ? attributedURIType.getValue() :
        WsAddressingConstants.UNSPECIFIED_MESSAGE;
        return createRelatesToType(msgId);
    }

    /**
     * Gets the address URI of an endpoint reference.
     *
     * @param epr the endpoint reference.
     * @return the endpoint reference or {@linkplain Optional#empty()} if there was no URI available.
     */
    public Optional<String> getAddressUri(EndpointReferenceType epr) {
        return Optional.ofNullable(epr.getAddress()).map(AttributedURIType::getValue);
    }

    /**
     * Gets the address URI string of an {@link AttributedURIType}.
     *
     * @param attributedURIType the attributed URI type where to extract the URI string.
     * @return the URI or an empty string if {@link AttributedURIType#getValue()} returned null.
     */
    public String getAddressUriString(AttributedURIType attributedURIType) {
        return attributedURIType.getValue() == null ? "" : attributedURIType.getValue();
    }

    /**
     * Creates an endpoint reference given an address string.
     *
     * @param addressUri the address string of the endpoint reference.
     * @return a new endpoint reference object.
     */
    public EndpointReferenceType createEprWithAddress(String addressUri) {
        EndpointReferenceType eprType = wsaFactory.createEndpointReferenceType();
        eprType.setAddress(createAttributedURIType(addressUri));
        return eprType;
    }

    /**
     * Creates an endpoint reference given an address URI.
     *
     * @param addressUri the address URI of the endpoint reference.
     * @return a new endpoint reference object.
     */
    public EndpointReferenceType createEprWithAddress(URI addressUri) {
        return createEprWithAddress(addressUri.toString());
    }
}
