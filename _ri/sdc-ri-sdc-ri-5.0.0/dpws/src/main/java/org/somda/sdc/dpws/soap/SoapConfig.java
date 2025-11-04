package org.somda.sdc.dpws.soap;

import javax.xml.bind.JAXBContext;

/**
 * Configuration of the SOAP package.
 *
 * @see org.somda.sdc.dpws.guice.DefaultDpwsConfigModule
 */
public class SoapConfig {

    /**
     * Sets the context path for JAXB marshalling and unmarshalling.
     * <p>
     * Internal context path elements will be added automatically.
     * This configuration item uses the same String format as used in {@link JAXBContext#newInstance(String)}.
     * <ul>
     * <li>Data type: {@linkplain String}
     * <li>Use: optional
     * </ul>
     */
    public static final String JAXB_CONTEXT_PATH = "SoapConfig.JaxbContextPath";

    /**
     * Colon-separated list of XML Schemas to validate against in case validation is enabled.
     * <p>
     * Please note that the order of the files matters.
     * If schema X uses schema Y, then schema Y is required to be listed before schema X.
     * <p>
     * The defined paths are resource paths.
     * XML Schemas used by DPWS are added automatically. Recognized namespaces are
     * <ul>
     * <li>http://www.w3.org/2003/05/soap-envelope
     * <li>http://www.w3.org/2005/08/addressing
     * <li>http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01
     * <li>http://schemas.xmlsoap.org/ws/2004/08/eventing
     * <li>http://schemas.xmlsoap.org/ws/2004/09/mex
     * <li>http://schemas.xmlsoap.org/ws/2004/09/transfer
     * <li>http://docs.oasis-open.org/ws-dd/ns/dpws/2009/01
     * <li>http://schemas.xmlsoap.org/wsdl/
     * <li>http://www.w3.org/XML/1998/namespace
     * </ul>
     * <p>
     * <ul>
     * <li>Data type: {@linkplain String}
     * <li>Use: optional
     * </ul>
     *
     * @see #VALIDATE_SOAP_MESSAGES
     */
    public static final String JAXB_SCHEMA_PATH = "SoapConfig.JaxbSchemaPath";

    /**
     * Defines a mapping of namespace prefixes to namespace URIS.
     * <p>
     * The configuration key is used to define custom namespace prefixes and optimize namespace usage.
     * The application will use the mappings on the root element in generated XML instances in favor to let
     * JAXB define where to append a namespace, which can lead to repeated usage and herewith inflating documents.
     * <p>
     * Some DPWS-specific prefix-to-namespace mappings are predefined:
     * <ul>
     * <li>XML Schema instance declarations: {xsi:http://www.w3.org/2001/XMLSchema-instance}
     * <li>WS-Addressing: {wsa:http://www.w3.org/2005/08/addressing}
     * <li>WS-Eventing: {wse:http://schemas.xmlsoap.org/ws/2004/08/eventing}
     * <li>WS-Discovery: {wsd:http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01}
     * <li>WS-MetadataExchange: {wsm:http://schemas.xmlsoap.org/ws/2004/09/mex}
     * <li>WS-Transfer: {wst:http://schemas.xmlsoap.org/ws/2004/09/transfer}
     * <li>DPWS: {dpws:http://docs.oasis-open.org/ws-dd/ns/dpws/2009/01}
     * <li>SOAP 1.2: {s12:http://www.w3.org/2003/05/soap-envelope}
     * </ul>
     * <p>
     * Internally, the prefix-to-namespace mappings are stored as a namespace to prefix-namespace map, which allows you
     * to overwrite prefixes by customizing the namespace in the config.
     * Configuration metadata:
     * <ul>
     * <li>Data type: {@linkplain String}
     * <li>Use: optional
     * </ul>
     *
     * @see org.somda.sdc.common.util.PrefixNamespaceMappingParser
     */
    public static final String NAMESPACE_MAPPINGS = "SoapConfig.NamespaceMappings";

    /**
     * Defines if SOAP message validation is enabled (true) or not (false).
     *
     * <ul>
     * <li>Data type: {@linkplain Boolean}
     * <li>Use: optional
     * </ul>
     */
    public static final String VALIDATE_SOAP_MESSAGES = "SoapConfig.ValidateSoapMessages";

    /**
     * Defines if outgoing messages should contain a comment with SDCri version information.
     *
     * <ul>
     *  <li>Data type: {@linkplain Boolean}
     *  <li>Use: optional
     * </ul>
     */
    public static final String METADATA_COMMENT = "SoapConfig.MetadataComment";
}
