package org.somda.sdc.dpws.soap.wsdiscovery;

import javax.xml.namespace.QName;
import java.time.Duration;

/**
 * WS-Discovery constants.
 */
public class WsDiscoveryConstants {
    /**
     * Package that includes all JAXB generated WS-Discovery objects.
     */
    public static final String JAXB_CONTEXT_PACKAGE = "org.somda.sdc.dpws.soap.wsdiscovery.model";

    /**
     * Resource path to WS-Discovery XML Schema.
     */
    public static final String SCHEMA_PATH = "ws-discovery-1.1-schema.xsd";

    /**
     * WS-Discovery 1.1 namespace.
     *
     * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231804"
     * >XML Namespaces</a>
     */
    public static final String NAMESPACE = "http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01";

    /**
     * Defines the preferred prefix for the WS-Discovery 1.1 namespace.
     */
    public static final String NAMESPACE_PREFIX = "wsd";

    /**
     * WS-Addressing Probe action.
     *
     * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231831"
     * >Probe</a>
     */
    public static final String WSA_ACTION_PROBE = NAMESPACE + "/Probe";

    /**
     * WS-Addressing ProbeMatches action.
     *
     * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231835"
     * >Probe Match</a>
     */
    public static final String WSA_ACTION_PROBE_MATCHES = NAMESPACE + "/ProbeMatches";

    /**
     * WS-Addressing Resolve action.
     *
     * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231840"
     * >Resolve</a>
     */
    public static final String WSA_ACTION_RESOLVE = NAMESPACE + "/Resolve";

    /**
     * WS-Addressing ResolveMatches action.
     *
     * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231844"
     * >Resolve Match</a>
     */
    public static final String WSA_ACTION_RESOLVE_MATCHES = NAMESPACE + "/ResolveMatches";

    /**
     * WS-Addressing Hello action.
     *
     * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231821"
     * >Hello</a>
     */
    public static final String WSA_ACTION_HELLO = NAMESPACE + "/Hello";

    /**
     * WS-Addressing Bye action.
     *
     * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231825"
     * >Bye</a>
     */
    public static final String WSA_ACTION_BYE = NAMESPACE + "/Bye";

    /**
     * WS-Addressing To-field for UDP multicast sinks.
     *
     * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231821"
     * >Hello</a>
     */
    public static final String WSA_UDP_TO = "urn:docs-oasis-open-org:ws-dd:ns:discovery:2009:01";

    /**
     * QName for application sequence number element.
     *
     * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231821"
     * >Hello</a>
     */
    public static final QName APP_SEQUENCE = new QName(NAMESPACE, "AppSequence");

    /**
     * Max delay to wait for transmitting a UDP message.
     * <p>
     * The constant defines the Application Level Transmission Delay defined in WS-Discovery, section 3.1.3.
     * <em>The value is superseded by DPWS.</em>
     * <p>
     * Excerpt from WS-Discovery:
     * <blockquote>
     * As designated below, before sending some message types defined herein, a Target Service MUST wait for a timer
     * to elapse before sending the message using the bindings described above. This timer MUST be set to a random
     * value between 0 and APP_MAX_DELAY. Table 5 specifies the default value for this parameter.
     * </blockquote>
     *
     * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231819"
     * >Application Level Transmission Delay</a>
     * @see <a href="http://docs.oasis-open.org/ws-dd/dpws/1.1/os/wsdd-dpws-1.1-spec-os.html#_Toc228672112"
     * >Application Level Transmission Delay (DPWS)</a>
     */
    public static final Duration APP_MAX_DELAY = Duration.ofMillis(2_500);

    /**
     * QName of the fault subcode if given matching rule is not supported by a target service.
     *
     * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231831"
     * >Probe</a>
     */
    public static final QName MATCHING_RULE_NOT_SUPPORTED = new QName(NAMESPACE, "MatchingRuleNotSupported");

    /**
     * WS-Discovery fault action.
     *
     * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231831"
     * >Probe</a>
     */
    public static final String FAULT_ACTION = NAMESPACE + "/fault";

    /**
     * Multicast address for IPv4 socket binding.
     *
     * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231817"
     * >Ad hoc mode over IP multicast</a>
     */
    public static final String IPV4_MULTICAST_ADDRESS = "239.255.255.250";
}
