package org.somda.sdc.dpws.service.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.device.WebService;
import org.somda.sdc.dpws.model.HostedServiceType;
import org.somda.sdc.dpws.service.HostedService;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.dpws.soap.wseventing.EventSink;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * Factory to create {@link HostedService} related} and {@link HostedServiceProxy} instances.
 */
public interface HostedServiceFactory {
    /**
     * Creates a hosted service instance.
     *
     * @param serviceId the service id. It is good practice to use a relative URL part (e.g., "SampleService").
     * @param types  list of QNames that matches the QNames of the port types of the WSDL
     *                that comes with the hosted service.
     * @param eprAddresses list of URLs where the hosted service can be requested from.
     * @param webService   interceptor to process incoming network requests.
     * @param wsdlDocument byte array to expose the hosted service's WSDL document.
     * @return hosted service instance used on the device side.
     */
    HostedService createHostedService(@Assisted String serviceId,
                                      @Assisted List<QName> types,
                                      @Assisted List<String> eprAddresses,
                                      @Assisted WebService webService,
                                      @Assisted byte[] wsdlDocument);

    /**
     * Creates a hosted service metadata instance without available EPR addresses.
     * <p>
     * This factory method can be used if EPR addresses shall be assigned automatically from a hosting service.
     *
     * @param serviceId the service id. It is good practice to use a relative URL part (e.g., "SampleService").
     * @param types  list of QNames that matches the QNames of the port types of the WSDL
     *                that comes with the hosted service.
     * @param webService interceptor to process incoming network requests.
     * @param wsdlDocument byte array to expose the hosted service's WSDL document.
     * @return hosted service instance used on the device side.
     */
    HostedService createHostedService(@Assisted String serviceId,
                                      @Assisted List<QName> types,
                                      @Assisted WebService webService,
                                      @Assisted byte[] wsdlDocument);

    /**
     * Creates a hosted service proxy instance.
     *
     * @param hostedServiceType the hosted service WS-MetadataExchange response information.
     * @param rrClient the request-response client to invoke service operations on.
     * @param activeEprAddress the physical address that is actively being used to send network requests.
     * @param eventSink the event sink client API to subscribe to notifications of a remote device.
     * @return hosted service proxy instance used by on the client side.
     */
    HostedServiceProxy createHostedServiceProxy(@Assisted HostedServiceType hostedServiceType,
                                                @Assisted RequestResponseClient rrClient,
                                                @Assisted String activeEprAddress,
                                                @Assisted EventSink eventSink);
}
