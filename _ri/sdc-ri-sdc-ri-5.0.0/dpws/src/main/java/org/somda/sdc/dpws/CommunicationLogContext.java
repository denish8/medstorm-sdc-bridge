package org.somda.sdc.dpws;

/**
 * Additional information for {@linkplain CommunicationLog} implementations.
 */
public class CommunicationLogContext {
    private final String eprAddress;

    public CommunicationLogContext(String eprAddress) {
        this.eprAddress = eprAddress;
    }

    /**
     * Gets the EPR address (identification) of a device.
     *
     * @return the EPR address.
     */
    public String getEprAddress() {
        return eprAddress;
    }
}
