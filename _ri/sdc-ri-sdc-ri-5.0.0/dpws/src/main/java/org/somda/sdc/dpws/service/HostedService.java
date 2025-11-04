package org.somda.sdc.dpws.service;

import org.somda.sdc.dpws.device.WebService;
import org.somda.sdc.dpws.model.HostedServiceType;

/**
 * Hosted service information of a device.
 */
public interface HostedService {
    /**
     * Gets the hosted service metadata requestable via WS-TransferGet.
     *
     * @return a copy of the hosted service metadata to be sent over the network.
     */
    HostedServiceType getType();

    /**
     * Gets the interceptor that is responsible to process incoming network requests.
     *
     * @return the Web Service interceptor where service logic is stored.
     */
    WebService getWebService();

    /**
     * Provision of an byte array with WSDL document data.
     *
     * @return byte array with WSDL data used to respond to WS-MetadataExchange requests.
     */
    byte[] getWsdlDocument();
}