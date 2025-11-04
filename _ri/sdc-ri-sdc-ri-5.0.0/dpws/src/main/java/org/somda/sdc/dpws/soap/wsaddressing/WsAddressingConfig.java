package org.somda.sdc.dpws.soap.wsaddressing;

/**
 * Configuration of the WS-Addressing package.
 *
 * @see org.somda.sdc.dpws.guice.DefaultDpwsConfigModule
 */
public class WsAddressingConfig {
    /**
     * Controls the maximum amount of cached SOAP messages for SOAP message duplication detection.
     * <ul>
     * <li>Data type: {@linkplain Integer}
     * <li>Use: optional
     * </ul>
     */
    public static final String MESSAGE_ID_CACHE_SIZE = "WsAddressing.MessageIdCacheSize";

    /**
     * Set to true to let servers ignore message ids.
     * <ul>
     * <li>Data type: {@linkplain Boolean}
     * <li>Use: optional
     * </ul>
     */
    public static final String IGNORE_MESSAGE_IDS = "WsAddressing.IgnoreMessageIds";
}
