package org.somda.sdc.dpws.soap.wsaddressing;

import javax.xml.namespace.QName;

/**
 * WS-Addressing constants.
 *
 * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-core-20060509/">Web Services Addressing 1.0 - Core</a>
 * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-soap-20060509/">Web Services Addressing 1.0 - SOAP Binding</a>
 */
public class WsAddressingConstants {
    /**
     * Package that includes all JAXB generated WS-Addressing objects.
     */
    public static final String JAXB_CONTEXT_PACKAGE = "org.somda.sdc.dpws.soap.wsaddressing.model";

    /**
     * Resource path to WS-Addressing XML Schema.
     */
    public static final String SCHEMA_PATH = "ws-addressing-1.0-schema.xsd";

    /**
     * WS-Addressing 1.0 namespace.
     *
     * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-core-20060509/#namespaces">Namespaces</a>
     */
    public static final String NAMESPACE = "http://www.w3.org/2005/08/addressing";

    /**
     * Defines the preferred prefix for the WS-Addressing 1.0 namespace.
     */
    public static final String NAMESPACE_PREFIX = "wsa";

    /**
     * WS-Addressing wsa:Action header element.
     *
     * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-core-20060509/#msgaddrprops"
     * >Message Addressing Properties</a>
     */
    public static final QName ACTION = new QName(NAMESPACE, "Action");

    /**
     * WS-Addressing wsa:MessageID header element.
     *
     * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-core-20060509/#msgaddrprops"
     * >Message Addressing Properties</a>
     */
    public static final QName MESSAGE_ID = new QName(NAMESPACE, "MessageID");

    /**
     * WS-Addressing wsa:ReplyTo header element.
     *
     * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-core-20060509/#msgaddrprops"
     * >Message Addressing Properties</a>
     */
    public static final QName RELATES_TO = new QName(NAMESPACE, "RelatesTo");

    /**
     * WS-Addressing wsa:To header element.
     *
     * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-core-20060509/#msgaddrprops"
     * >Message Addressing Properties</a>
     */
    public static final QName TO = new QName(NAMESPACE, "To");

    /**
     * WS-Addressing wsa:ReferenceParameters header element.
     *
     * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-core-20060509/#msgaddrprops"
     * >Message Addressing Properties</a>
     */
    public static final QName REFERENCE_PARAMETERS = new QName(NAMESPACE, "ReferenceParameters");

    /**
     * ActionNotSupported fault QName.
     *
     * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-soap-20060509/#actionfault">Action Not Supported</a>
     */
    public static final QName ACTION_NOT_SUPPORTED = new QName(NAMESPACE, "ActionNotSupported");

    /**
     * MessageAddressingHeaderRequired fault QName.
     *
     * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-soap-20060509/#missingmapfault"
     * >Message Addressing Header Required</a>
     */
    public static final QName MESSAGE_ADDRESSING_HEADER_REQUIRED =
            new QName(NAMESPACE, "MessageAddressingHeaderRequired");

    /**
     * WS-Addressing wsa:Action for faults.
     *
     * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-soap-20060509/#faults">Faults</a>
     */
    public static final String FAULT_ACTION = NAMESPACE + "/fault";

    /**
     * WS-Addressing anonymous wsa:To/wsa:ReplyTo endpoint address.
     *
     * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-soap-20060509/#anonaddress"
     * >Use of Anonymous Address in SOAP Response Endpoints</a>
     */
    public static final String ANONYMOUS = NAMESPACE + "/anonymous";

    /**
     * IsReferenceParameter attribute.
     *
     * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-soap-20060509/#additionalinfoset"
     * >Additional Infoset Items</a>
     */
    public static final QName IS_REFERENCE_PARAMETER = new QName(NAMESPACE, "IsReferenceParameter");

    /**
     * QName of the WS-Addressing Action element.
     */
    public static final QName QNAME_ACTION = new QName(NAMESPACE, "Action");

    /**
     * Predefined URI for the relationship property in WSAdressing, when messageId is missing.
     *
     * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-core-20060509/#abstractmaps">
     *     WSAddressing Property Definitions</a>
     */
    public static final String UNSPECIFIED_MESSAGE = "http://www.w3.org/2005/08/addressing/unspecified";
}