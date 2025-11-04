package org.somda.sdc.dpws.service;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.dpws.device.DeviceConfig;
import org.somda.sdc.dpws.device.helper.ByteResourceHandler;
import org.somda.sdc.dpws.http.HttpServerRegistry;
import org.somda.sdc.dpws.service.helper.MetadataSectionUtil;
import org.somda.sdc.dpws.soap.SoapConstants;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.interception.Direction;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryTargetService;
import org.somda.sdc.dpws.soap.wsmetadataexchange.WsMetadataExchangeConstants;
import org.somda.sdc.dpws.soap.wsmetadataexchange.model.GetMetadata;
import org.somda.sdc.dpws.soap.wsmetadataexchange.model.Metadata;
import org.somda.sdc.dpws.soap.wsmetadataexchange.model.MetadataSection;
import org.somda.sdc.dpws.soap.wsmetadataexchange.model.ObjectFactory;
import org.somda.sdc.dpws.wsdl.WsdlMarshalling;
import org.somda.sdc.dpws.wsdl.WsdlProvisioningMode;
import org.somda.sdc.dpws.wsdl.model.TDefinitions;

import javax.inject.Named;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

/**
 * Server interceptor to serve GetMetadata requests on hosted services.
 */
public class HostedServiceInterceptor implements Interceptor {
    private static final Logger LOG = LogManager.getLogger(HostedServiceInterceptor.class);

    private final HostedService hostedService;
    private final WsDiscoveryTargetService targetService;
    private final SoapUtil soapUtil;
    private final ObjectFactory mexFactory;
    private final org.somda.sdc.dpws.wsdl.model.ObjectFactory wsdlFactory;
    private final WsAddressingUtil wsaUtil;
    private final MetadataSectionUtil metadataSectionUtil;
    private final WsdlMarshalling wsdlMarshalling;
    private final HttpServerRegistry httpServerRegistry;

    private String wsdlUri;
    private JAXBElement<TDefinitions> wsdlDefinition;
    private WsdlProvisioningMode provisioningMode;


    @AssistedInject
    HostedServiceInterceptor(@Assisted HostedService hostedService,
                             @Assisted WsDiscoveryTargetService targetService,
                             @Named(DeviceConfig.WSDL_PROVISIONING_MODE) WsdlProvisioningMode provisioningMode,
                             SoapUtil soapUtil,
                             ObjectFactory mexFactory,
                             org.somda.sdc.dpws.wsdl.model.ObjectFactory wsdlFactory,
                             WsAddressingUtil wsaUtil,
                             MetadataSectionUtil metadataSectionUtil,
                             WsdlMarshalling wsdlMarshalling,
                             HttpServerRegistry httpServerRegistry) {
        this.hostedService = hostedService;
        this.targetService = targetService;
        this.soapUtil = soapUtil;
        this.mexFactory = mexFactory;
        this.wsdlFactory = wsdlFactory;
        this.wsaUtil = wsaUtil;
        this.metadataSectionUtil = metadataSectionUtil;
        this.provisioningMode = provisioningMode;
        this.wsdlMarshalling = wsdlMarshalling;
        this.httpServerRegistry = httpServerRegistry;

        this.wsdlUri = null;
        this.wsdlDefinition = null;
        setupInlineDefinition();
        setupWsdlResource();
    }

    @MessageInterceptor(value = WsMetadataExchangeConstants.WSA_ACTION_GET_METADATA_REQUEST,
            direction = Direction.REQUEST)
    void processGetMetadata(RequestResponseObject rrObj) {
        GetMetadata body = soapUtil.getBody(rrObj.getRequest(), GetMetadata.class).orElse(null);

        Metadata metadata = mexFactory.createMetadata();
        List<MetadataSection> metadataSection = metadata.getMetadataSection();

        // \todo DGr is host relationship required here?
        metadataSection.add(metadataSectionUtil.createRelationship(targetService.getEndpointReference(),
                targetService.getTypes(), Collections.singletonList(hostedService)));

        if (body == null || body.getDialect() == null || body.getDialect().isEmpty() ||
                body.getDialect().equals(WsMetadataExchangeConstants.DIALECT_WSDL)) {
            switch (provisioningMode) {
                case INLINE:
                    metadataSection.add(createWsdlMetadataSection());
                    break;
                case RESOURCE:
                    metadataSection.add(createWsdlMetadataSectionAsResource());
                    break;
                default:
                    LOG.warn("Unsupported WSDL provisioning mode detected: {}", provisioningMode);
            }
        }

        rrObj.getResponse().getWsAddressingHeader().setAction(
                wsaUtil.createAttributedURIType(WsMetadataExchangeConstants.WSA_ACTION_GET_METADATA_RESPONSE));

        metadata.setMetadataSection(metadataSection);
        soapUtil.setBody(metadata, rrObj.getResponse());
    }

    private MetadataSection createWsdlMetadataSection() {
        MetadataSection metadataSection = mexFactory.createMetadataSection();
        metadataSection.setDialect(WsMetadataExchangeConstants.DIALECT_WSDL);
        metadataSection.setAny(wsdlDefinition);
        return metadataSection;
    }

    private MetadataSection createWsdlMetadataSectionAsResource() {
        MetadataSection metadataSection = mexFactory.createMetadataSection();
        metadataSection.setDialect(WsMetadataExchangeConstants.DIALECT_WSDL);
        if (wsdlUri == null) {
            LOG.warn("No resource link for WSDL provisioning found. Metadata section will be left empty.");
        } else {
            metadataSection.setAny(mexFactory.createLocation(wsdlUri));
        }
        return metadataSection;
    }

    private void setupInlineDefinition() {
        if (!WsdlProvisioningMode.INLINE.equals(provisioningMode)) {
            return;
        }

        try {
            this.wsdlDefinition = wsdlFactory.createDefinitions(
                    wsdlMarshalling.unmarshal(new ByteArrayInputStream(hostedService.getWsdlDocument())));
        } catch (JAXBException e) {
            LOG.warn("Unmarshalling of WSDL failed. Fallback to resource provisioning. Error message: {}",
                    e.getMessage());
            LOG.trace("Unmarshalling of WSDL failed", e);
            this.provisioningMode = WsdlProvisioningMode.RESOURCE;
        }
    }

    private void setupWsdlResource() {
        if (!WsdlProvisioningMode.RESOURCE.equals(provisioningMode)) {
            return;
        }

        // Make WSDL document bytes available as HTTP resource
        var wsdlDocBytes = hostedService.getWsdlDocument();
        for (EndpointReferenceType epr : hostedService.getType().getEndpointReference()) {
            if (wsdlDocBytes.length == 0) {
                throw new RuntimeException(String.format("Could not register WSDL resource for %s. WSDL is empty.",
                        epr));
            }

            var uriFromEpr = wsaUtil.getAddressUri(epr);
            if (uriFromEpr.isEmpty()) {
                throw new RuntimeException("Invalid EPR detected while trying to create WSDL resource. " +
                                                   String.format("Skip EPR %s.", epr));
            }

            URI uri;
            try {
                uri = new URI(uriFromEpr.get());
            } catch (URISyntaxException e) {
                LOG.warn("Invalid URI detected while trying to create WSDL resource. Skip EPR {}.", epr);
                continue;
            }

            var wsdlContextPath = uri.getPath() + "/wsdl";
            wsdlUri = httpServerRegistry.registerContext(uriFromEpr.get(), true, wsdlContextPath,
                    SoapConstants.MEDIA_TYPE_WSDL, null, new ByteResourceHandler(wsdlDocBytes));
            // There is only space for one location per metadata section, and we don't want to provide more than
            // one section at the moment, hence exit loop once a valid entry was found
            break;
        }
    }
}
