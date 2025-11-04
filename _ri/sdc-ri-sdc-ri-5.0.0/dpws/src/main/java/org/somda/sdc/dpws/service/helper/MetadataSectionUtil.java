package org.somda.sdc.dpws.service.helper;

import com.google.inject.Inject;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.model.HostServiceType;
import org.somda.sdc.dpws.model.Relationship;
import org.somda.sdc.dpws.service.HostedService;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wsmetadataexchange.model.MetadataSection;
import org.somda.sdc.dpws.soap.wsmetadataexchange.model.ObjectFactory;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * Utility class to create metadata sections for WS-MetadataExchange.
 */
public class MetadataSectionUtil {

    private final ObjectFactory mexFactory;
    private final org.somda.sdc.dpws.model.ObjectFactory dpwsFactory;

    @Inject
    MetadataSectionUtil(ObjectFactory mexFactory,
                        org.somda.sdc.dpws.model.ObjectFactory dpwsFactory) {
        this.mexFactory = mexFactory;
        this.dpwsFactory = dpwsFactory;
    }

    /**
     * Creates a metadata section for a DPWS Host.
     *
     * @param hostingServiceEpr of the host
     * @param hostingServiceTypes of the services
     * @param hostedServices of the host
     * @return a new metadata section containing host and services.
     */
    public MetadataSection createRelationship(EndpointReferenceType hostingServiceEpr,
                                              List<QName> hostingServiceTypes,
                                              List<HostedService> hostedServices) {
        MetadataSection metadataSection = mexFactory.createMetadataSection();
        metadataSection.setDialect(DpwsConstants.MEX_DIALECT_RELATIONSHIP);
        Relationship relationship = dpwsFactory.createRelationship();
        relationship.setType(DpwsConstants.RELATIONSHIP_TYPE_HOST);

        HostServiceType hostServiceType = dpwsFactory.createHostServiceType();
        hostServiceType.setEndpointReference(hostingServiceEpr);
        hostServiceType.setTypes(hostingServiceTypes);
        relationship.getAny().add(dpwsFactory.createHost(hostServiceType));
        hostedServices.forEach(hostedService ->
                relationship.getAny().add(dpwsFactory.createHosted(hostedService.getType())));

        metadataSection.setAny(relationship);
        return metadataSection;
    }
}
