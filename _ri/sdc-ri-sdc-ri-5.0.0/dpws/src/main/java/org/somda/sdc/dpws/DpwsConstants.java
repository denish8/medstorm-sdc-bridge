package org.somda.sdc.dpws;

import javax.xml.namespace.QName;
import java.time.Duration;

/**
 * DPWS constants.
 *
 * @see <a href="http://docs.oasis-open.org/ws-dd/dpws/1.1/os/wsdd-dpws-1.1-spec-os.html#_Toc228672112"
 * >Appendix B. Constants</a>
 */
public class DpwsConstants {
    /**
     * JAXB context paths used to let JAXB recognize the DPWS model.
     */
    public static final String DPWS_JAXB_CONTEXT_PATH = "org.somda.sdc.dpws.soap.model:" +
            "org.somda.sdc.dpws.model:" +
            "org.somda.sdc.dpws.soap.wsaddressing.model:" +
            "org.somda.sdc.dpws.soap.wsdiscovery.model:" +
            "org.somda.sdc.dpws.soap.wseventing.model:" +
            "org.somda.sdc.dpws.soap.wstransfer.model:" +
            "org.somda.sdc.dpws.soap.wsmetadataexchange.model:" +
            "org.somda.sdc.dpws.wsdl.model";

    /**
     * Resource path to DPWS XML Schema.
     */
    public static final String SCHEMA_PATH = "wsdd-dpws-1.1-schema.xsd";

    /**
     * Defines the DPWS namespace.
     */
    public static final String NAMESPACE = "http://docs.oasis-open.org/ws-dd/ns/dpws/2009/01";

    /**
     * Defines the preferred prefix for the DPWS namespace.
     */
    public static final String NAMESPACE_PREFIX = "dpws";

    /**
     * Defines the multicast port used to transmit discovery messages.
     */
    public static final int DISCOVERY_PORT = 3_702;

    /**
     * Defines the maximum size for envelopes transmitted over TCP.
     * <p>
     * The maximum envelope size is currently not verified, i.e., SOAP messages of any size are neither
     * detected nor rejected.
     * CAVEAT: lower layer protocol implementations may have some restrictions that are out of scope for this constant.
     * <p>
     * Unit: octets.
     */
    public static final int MAX_ENVELOPE_SIZE = 32_767;

    /**
     * Defines the maximum size for envelopes transmitted over UDP.
     * <p>
     * Unit: octets
     */
    public static final int MAX_UDP_ENVELOPE_SIZE = 4_096;

    /**
     * Defines the maximum size for different attributes introduced by DPWS.
     * <p>
     * Unit: Unicode characters
     */
    public static final int MAX_FIELD_SIZE = 256;

    /**
     * Defines the maximum size for URIs.
     * <p>
     * Unit: octets
     */
    public static final int MAX_URI_SIZE = 2_048;

    /**
     * Defines the retry number for unreliable UDP multicast traffic.
     */
    public static final int MULTICAST_UDP_REPEAT = 1;

    /**
     * Defines the maximum delay for the SOAP-over-UDP retransmission algorithm.
     */
    public static final Duration UDP_MAX_DELAY = Duration.ofMillis(250L);

    /**
     * Defines the minimum delay for the SOAP-over-UDP retransmission algorithm.
     */
    public static final Duration UDP_MIN_DELAY = Duration.ofMillis(50L);

    /**
     * Defines the upper delay for the SOAP-over-UDP retransmission algorithm.
     */
    public static final Duration UDP_UPPER_DELAY = Duration.ofMillis(450L);

    /**
     * Defines the retry number for unreliable UDP unicast traffic.
     *
     * @see #MULTICAST_UDP_REPEAT
     */
    public static final int UNICAST_UDP_REPEAT = MULTICAST_UDP_REPEAT;

    /**
     * Default TTL for outgoing multicast messages.
     */
    public static final int DEFAULT_MULTICAST_TTL = 1;

    /**
     * Defines the namespace for the DPWS ThisModel data structure.
     */
    public static final String MEX_DIALECT_THIS_MODEL = NAMESPACE + "/ThisModel";

    /**
     * Defines the namespace for the DPWS ThisDevice data structure.
     */
    public static final String MEX_DIALECT_THIS_DEVICE = NAMESPACE + "/ThisDevice";

    /**
     * Defines the namespace for the DPWS Relationship data structure.
     */
    public static final String MEX_DIALECT_RELATIONSHIP = NAMESPACE + "/Relationship";

    /**
     * Defines the minimum supported WS-Eventing dialect URI.
     */
    public static final String WS_EVENTING_SUPPORTED_DIALECT = NAMESPACE + "/Action";

    /**
     * Defines the DPWS relationship type for hosts (devices).
     */
    public static final String RELATIONSHIP_TYPE_HOST = NAMESPACE + "/host";

    /**
     * Defines the DPWS device type that is required to identify a DPWS compliant device during discovery.
     */
    public static final QName DEVICE_TYPE = new QName(NAMESPACE, "Device");

    /**
     * URI scheme for SOAP-over-UDP.
     */
    public static final String URI_SCHEME_SOAP_OVER_UDP = "soap.udp";
}
