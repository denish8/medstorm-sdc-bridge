package org.somda.sdc.dpws.service;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.dpws.device.WebService;
import org.somda.sdc.dpws.model.HostedServiceType;
import org.somda.sdc.dpws.model.ObjectFactory;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation of {@linkplain HostedService}.
 */
public class HostedServiceImpl implements HostedService {
    private final String serviceId;
    private final List<QName> types;
    private final List<EndpointReferenceType> eprs;
    private final WebService webService;
    private final byte[] wsdlDocument;
    private final ObjectFactory dpwsFactory;

    @AssistedInject
    HostedServiceImpl(@Assisted String serviceId,
                      @Assisted List<QName> types,
                      @Assisted List<String> eprAddresses,
                      @Assisted WebService webService,
                      @Assisted byte[] wsdlDocument,
                      ObjectFactory dpwsFactory,
                      WsAddressingUtil wsaUtil) {
        this.serviceId = serviceId;
        this.types = types;
        this.eprs = eprAddresses.stream()
                .map(wsaUtil::createEprWithAddress)
                .collect(Collectors.toList());
        this.webService = webService;
        this.wsdlDocument = wsdlDocument;
        this.dpwsFactory = dpwsFactory;
    }

    @AssistedInject
    HostedServiceImpl(@Assisted String serviceId,
                      @Assisted List<QName> types,
                      @Assisted WebService webService,
                      @Assisted byte[] wsdlDocument,
                      ObjectFactory dpwsFactory,
                      WsAddressingUtil wsaUtil) {
        this(serviceId, types, new ArrayList<>(), webService, wsdlDocument, dpwsFactory, wsaUtil);
    }

    @Override
    public HostedServiceType getType() {
        HostedServiceType hst = dpwsFactory.createHostedServiceType();
        hst.setServiceId(serviceId);
        hst.setEndpointReference(eprs.stream().map(EndpointReferenceType::createCopy).collect(Collectors.toList()));
        hst.setTypes(new ArrayList<>(types));
        return hst;
    }

    @Override
    public WebService getWebService() {
        return webService;
    }

    @Override
    public byte[] getWsdlDocument() {
        return wsdlDocument;
    }
}
