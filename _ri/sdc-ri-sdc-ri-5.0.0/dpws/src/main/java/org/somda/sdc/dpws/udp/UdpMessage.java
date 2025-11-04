package org.somda.sdc.dpws.udp;

import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.soap.ApplicationInfo;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.TransportInfo;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Raw UDP message packed as a byte array plus a length attribute and receiver information.
 */
public class UdpMessage {
    private final int length;
    private final byte[] data;
    private final CommunicationContext communicationContext;

    /**
     * Constructor with transport information.
     *
     * @param data                 the payload of the UDP message.
     * @param length               the actual message length.
     * @param communicationContext message transport and application information.
     */
    public UdpMessage(byte[] data, int length, CommunicationContext communicationContext) {
        this.data = data;
        this.length = length;
        this.communicationContext = communicationContext;
    }

    /**
     * Constructor without transport information.
     * <p>
     * Transport information is not required in case this message is used with multicast.
     *
     * @param data   the payload of the UDP message.
     * @param length the actual message length.
     */
    public UdpMessage(byte[] data, int length) {
        this.data = data;
        this.length = length;
        this.communicationContext = new CommunicationContext(
                new ApplicationInfo(),
                new TransportInfo(
                        DpwsConstants.URI_SCHEME_SOAP_OVER_UDP,
                        null, null,
                        null, null,
                        Collections.emptyList()
                ),
                null
        );
    }

    /**
     * Checks if there is transport data attached to the message.
     *
     * @return true if there is a host and port, otherwise false.
     */
    public boolean hasTransportData() {
        return getHost() != null && getPort() != null;
    }

    public Integer getPort() {
        return communicationContext.getTransportInfo().getRemotePort().orElse(null);
    }

    public String getHost() {
        return communicationContext.getTransportInfo().getRemoteAddress().orElse(null);
    }

    public int getLength() {
        return length;
    }

    /**
     * Gets the data from this object as byte array.
     * <p>
     * <em>Do not rely on the byte array's length attribute, retrieve the length via {@link #getLength()} instead!</em>
     *
     * @return message byte array.
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Returns the communication context stored in this message.
     *
     * @return communication context containing transport information and more.
     */
    public CommunicationContext getCommunicationContext() {
        return communicationContext;
    }

    @Override
    public String toString() {
        return new String(data, 0, length, StandardCharsets.UTF_8);
    }
}
