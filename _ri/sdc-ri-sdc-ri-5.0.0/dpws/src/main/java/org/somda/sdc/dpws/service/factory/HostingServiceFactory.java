package org.somda.sdc.dpws.service.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.model.ThisDeviceType;
import org.somda.sdc.dpws.model.ThisModelType;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostingService;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryTargetService;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

/**
 * Factory to create {@link HostingService} instances.
 */
public interface HostingServiceFactory {
    /**
     * Creates a hosting service.
     *
     * @param targetService WS-Discovery target service information used by the hosting service.
     * @return a {@link HostingService} instance for targetService.
     */
    HostingService createHostingService(@Assisted WsDiscoveryTargetService targetService);

    /**
     * Creates a hosting service proxy.
     *
     * @param endpointReferenceAddress endpoint reference address of the hosting service (unique device identifier).
     * @param types list of types of the hosting service.
     * @param thisDevice ThisDevice information of the hosting service.
     * @param thisModel ThisModel information of the hosting service.
     * @param hostedServices map of service ids to hosted service proxies.
     * @param metadataVersion metadata version used for hosting service. todo DGr remove version, bc unreliable info
     * @param requestResponseClient request-response client to be used to access the hosting service.
     * @param activeXAddr physical address that was used to retrieve the hosting service proxy information.
     * @return a {@link HostingServiceProxy} instance.
     */
    // CHECKSTYLE.OFF: ParameterNumber
    HostingServiceProxy createHostingServiceProxy(@Assisted("eprAddress") String endpointReferenceAddress,
                                                  @Assisted List<QName> types,
                                                  @Assisted ThisDeviceType thisDevice,
                                                  @Assisted ThisModelType thisModel,
                                                  @Assisted Map<String, HostedServiceProxy> hostedServices,
                                                  @Assisted long metadataVersion,
                                                  @Assisted RequestResponseClient requestResponseClient,
                                                  @Assisted("activeXAddr") String activeXAddr);
    // CHECKSTYLE.ON: ParameterNumber
}
