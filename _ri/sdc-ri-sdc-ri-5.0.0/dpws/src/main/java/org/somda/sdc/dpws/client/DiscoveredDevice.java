package org.somda.sdc.dpws.client;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * Device information that is resolved through WS-Discovery.
 * <p>
 * <em>Note that all information provided by instances of {@linkplain DiscoveredDevice} are potentially unsecure!
 * Potentially unsecure because there are cases in which a consumer may connect to a discovery proxy that uses
 * secured transmission to exchange discovery information.</em>
 *
 * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/wsdd-discovery-1.1-spec.html">WS-Discovery</a>
 */
public class DiscoveredDevice {
    private final String eprAddress;
    private final List<QName> types;
    private final List<String> scopes;
    private final List<String> xAddrs;
    private final long metadataVersion;

    public DiscoveredDevice(String eprAddress,
                            List<QName> types,
                            List<String> scopes,
                            List<String> xAddrs,
                            long metadataVersion) {
        this.eprAddress = eprAddress;
        this.types = types;
        this.scopes = scopes;
        this.xAddrs = xAddrs;
        this.metadataVersion = metadataVersion;
    }

    /**
     * Returns the EPR address of the discovered device.
     * <p>
     * <em>Note that all information provided by instances of {@linkplain DiscoveredDevice} are potentially unsecure!
     * Potentially unsecure because there are cases in which a consumer may connect to a discovery proxy that uses
     * secured transmission to exchange discovery information.</em>
     *
     * @return the EPR address of the discovered device.
     */
    public String getEprAddress() {
        return eprAddress;
    }

    /**
     * Returns the types implemented by the discovered device.
     * <p>
     * <em>Note that all information provided by instances of {@linkplain DiscoveredDevice} are potentially unsecure!
     * Potentially unsecure because there are cases in which a consumer may connect to a discovery proxy that uses
     * secured transmission to exchange discovery information.</em>
     *
     * @return the types of the discovered device.
     */
    public List<QName> getTypes() {
        return types;
    }

    /**
     * Returns the scopes set on the discovered device.
     * <p>
     * <em>Note that all information provided by instances of {@linkplain DiscoveredDevice} are potentially unsecure!
     * Potentially unsecure because there are cases in which a consumer may connect to a discovery proxy that uses
     * secured transmission to exchange discovery information.</em>
     *
     * @return the scopes of the discovered device.
     */
    public List<String> getScopes() {
        return scopes;
    }

    /**
     * Returns the physical addresses at which the device can be accessed.
     * <p>
     * <em>Note that all information provided by instances of {@linkplain DiscoveredDevice} are potentially unsecure!
     * Potentially unsecure because there are cases in which a consumer may connect to a discovery proxy that uses
     * secured transmission to exchange discovery information.</em>
     *
     * @return the physical addresses of the discovered device.
     */
    public List<String> getXAddrs() {
        return xAddrs;
    }

    /**
     * Returns the metadata version as specified by WS-Discovery.
     * <p>
     * <em>Note that all information provided by instances of {@linkplain DiscoveredDevice} are potentially unsecure!
     * Potentially unsecure because there are cases in which a consumer may connect to a discovery proxy that uses
     * secured transmission to exchange discovery information.</em>
     *
     * @return the version of the device's meta information.
     */
    public long getMetadataVersion() {
        return metadataVersion;
    }
}
