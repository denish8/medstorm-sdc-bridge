package org.somda.sdc.glue.common;

/**
 * General configuration of the SDC Glue common package.
 *
 * @see org.somda.sdc.glue.guice.DefaultGlueConfigModule
 */
public class CommonConfig {
    /**
     * Defines a mapping of namespace prefixes to namespace URIS relevant to BICEPS and MDPWS.
     * <p>
     * Configuration metadata:
     * <ul>
     * <li>Data type: {@linkplain String}
     * <li>Use: optional
     * </ul>
     *
     * @see org.somda.sdc.dpws.soap.SoapConfig#NAMESPACE_MAPPINGS
     */
    public static final String NAMESPACE_MAPPINGS = "SdcGlue.NamespaceMappings";
}
