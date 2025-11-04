package org.somda.sdc.dpws.soap.wstransfer;

/**
 * WS-Transfer constants.
 *
 * @see <a href="https://www.w3.org/Submission/2006/SUBM-WS-Transfer-20060927/">WS-Transfer specification</a>
 */
public class WsTransferConstants {
    public static final String JAXB_CONTEXT_PACKAGE = "org.somda.sdc.dpws.soap.wstransfer.model";

    /**
     * Resource path to WS-TransferGet XML Schema.
     */
    public static final String SCHEMA_PATH = "ws-transfer-schema.xsd";

    /**
     * WS-Transfer namespace.
     *
     * @see <a href="https://www.w3.org/Submission/2006/SUBM-WS-Transfer-20060927/#XML_Namespaces">XML Namespaces</a>
     */
    public static final String NAMESPACE = "http://schemas.xmlsoap.org/ws/2004/09/transfer";

    /**
     * Defines the preferred prefix for the WS-Transfer namespace.
     */
    public static final String NAMESPACE_PREFIX = "wst";

    /**
     * Get request action URI.
     *
     * @see <a href="https://www.w3.org/Submission/2006/SUBM-WS-Transfer-20060927/#Get">Get</a>
     */
    public static final String WSA_ACTION_GET = NAMESPACE + "/Get";

    /**
     * Get response action URI.
     *
     * @see <a href="https://www.w3.org/Submission/2006/SUBM-WS-Transfer-20060927/#Get">Get</a>
     */
    public static final String WSA_ACTION_GET_RESPONSE = NAMESPACE + "/GetResponse";
}
