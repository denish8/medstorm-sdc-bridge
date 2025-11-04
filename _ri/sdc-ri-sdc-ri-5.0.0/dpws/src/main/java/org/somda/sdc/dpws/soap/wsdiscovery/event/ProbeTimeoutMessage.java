package org.somda.sdc.dpws.soap.wsdiscovery.event;

import org.somda.sdc.common.event.EventMessage;

/**
 * Message that signalizes the end of a probe phase.
 */
public class ProbeTimeoutMessage implements EventMessage {
    private final Integer probeMatchesCount;
    private final String probeRequestId;

    /**
     * Constructor.
     *
     * @param probeMatchesCount the amount of matches found for a probe request.
     * @param probeRequestId    the probe request identifier.
     */
    public ProbeTimeoutMessage(Integer probeMatchesCount, String probeRequestId) {
        this.probeMatchesCount = probeMatchesCount;
        this.probeRequestId = probeRequestId;
    }

    /**
     * Gets the identifier that relates a probe request and response to each other.
     *
     * @return the probe request identifier.
     */
    public String getProbeRequestId() {
        return probeRequestId;
    }

    /**
     * Gets the number of matched items.
     * <p>
     * Note that the maximum amount of matched items can be limited.
     *
     * @return the number of matches.
     */
    public Integer getProbeMatchesCount() {
        return probeMatchesCount;
    }
}

