package org.somda.sdc.dpws.client;

/**
 * Configuration keys for the client.
 *
 * @see org.somda.sdc.dpws.guice.DefaultDpwsConfigModule
 */
public class ClientConfig {
    /**
     * Controls maximum waiting time to get ResolveMatches information fetched from WS-Discovery.
     * <ul>
     * <li>Data type: {@linkplain java.time.Duration}
     * <li>Use: optional
     * </ul>
     */
    public static final String MAX_WAIT_FOR_RESOLVE_MATCHES = "Dpws.Client.MaxWaitForResolveMatches";

    /**
     * Configures auto-resolve for probes and hellos without XAddrs.
     * <ul>
     * <li>Data type: {@linkplain Boolean}
     * <li>Use: optional
     * </ul>
     */
    public static final String AUTO_RESOLVE = "Dpws.Client.AutoResolve";
}
