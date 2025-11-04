package org.somda.sdc.dpws.device;

import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;

import java.net.NetworkInterface;

/**
 * Settings used in the setup process of a device.
 */
public interface DeviceSettings {
    /**
     * Gets the unique and persisted endpoint reference (EPR) of the device.
     * 
     * @return the endpoint reference of the device.
     */
    EndpointReferenceType getEndpointReference();

    /**
     * Gets the network interface the device shall bind to.
     *
     * @return the network interface to bind to.
     */
    NetworkInterface getNetworkInterface();
}
