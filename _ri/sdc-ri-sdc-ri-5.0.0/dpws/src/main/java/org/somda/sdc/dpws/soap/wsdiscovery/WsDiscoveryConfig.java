package org.somda.sdc.dpws.soap.wsdiscovery;

/**
 * Configuration of the WS-Discovery package.
 *
 * @see org.somda.sdc.dpws.guice.DefaultDpwsConfigModule
 */
public class WsDiscoveryConfig {
    /**
     * Controls the maximum waiting time for ProbeMatches messages.
     * <ul>
     * <li>Data type: {@linkplain java.time.Duration}
     * <li>Use: optional
     * </ul>
     */
    public static final String MAX_WAIT_FOR_PROBE_MATCHES = "WsDiscovery.MaxWaitForProbeMatches";

    /**
     * Controls the maximum waiting time for ResolveMatches messages.
     * <ul>
     * <li>Data type: {@linkplain java.time.Duration}
     * <li>Use: optional
     * </ul>
     */
    public static final String MAX_WAIT_FOR_RESOLVE_MATCHES = "WsDiscovery.MaxWaitForResolveMatches";

    /**
     * Controls the maximum buffer size for incoming ProbeMatches messages.
     * <ul>
     * <li>Data type: {@linkplain Integer}
     * <li>Use: optional
     * </ul>
     */
    public static final String PROBE_MATCHES_BUFFER_SIZE = "WsDiscovery.MaxProbeMatchesBufferSize";

    /**
     * Controls the maximum buffer size for incoming ResolveMatches messages.
     * <ul>
     * <li>Data type: {@linkplain Integer}
     * <li>Use: optional
     * </ul>
     */
    public static final String RESOLVE_MATCHES_BUFFER_SIZE = "WsDiscovery.MaxResolveMatchesBufferSize";
}
