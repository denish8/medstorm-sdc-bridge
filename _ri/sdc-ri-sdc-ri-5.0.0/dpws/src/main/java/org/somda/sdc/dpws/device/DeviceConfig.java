package org.somda.sdc.dpws.device;

/**
 * Configuration tags of the DPWS device side.
 *
 * @see org.somda.sdc.dpws.guice.DefaultDpwsConfigModule
 */
public class DeviceConfig {
    /**
     * Defines the mode that describes how to provide WSDL data.
     * <p>
     * <ul>
     * <li>Data type: {@linkplain org.somda.sdc.dpws.wsdl.WsdlProvisioningMode}
     * <li>Use: optional
     * </ul>
     */
    public static final String WSDL_PROVISIONING_MODE = "Dpws.WsdlProvisioningMode";
}
