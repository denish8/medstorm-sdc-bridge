package org.somda.sdc.dpws.helper;

import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingConstants;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants;
import org.somda.sdc.dpws.soap.wseventing.WsEventingConstants;
import org.somda.sdc.dpws.soap.wsmetadataexchange.WsMetadataExchangeConstants;
import org.somda.sdc.dpws.soap.wstransfer.WsTransferConstants;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Helper class to analyze XML intended to be written to a communication log directory.
 * <p>
 * This separate class exists only to improve testability.
 */
class CommunicationLogSoapXmlUtils {
    private static final String EMPTY_XML = "[empty]";
    private static final String ACTION_UNKNOWN = "[unknown]";

    /**
     * Short version {@link #moreReadable(String)} applied on {@link #findAction(byte[])}.
     *
     * @param xmlDocument the XML document to make a name from.
     * @return the short action name to be used for file names.
     */
    String makeNameElement(byte[] xmlDocument) {
        return moreReadable(findAction(xmlDocument));
    }

    /**
     * Extracts the WS-Addressing action from a SOAP document.
     *
     * @param xmlDocument the XML document to take the action from.
     * @return the action URI, or {@code [empty]} if bytes were empty, or {@code [unknown]} if action could not be
     * extracted.
     */
    String findAction(byte[] xmlDocument) {
        if (xmlDocument.length == 0) {
            return EMPTY_XML;
        }
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            // #218 prevent XXE attacks
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
            XMLStreamReader parser = factory.createXMLStreamReader(new ByteArrayInputStream(xmlDocument));
            QName actionElement = null;
            while (parser.hasNext()) {
                switch (parser.getEventType()) {
                    case XMLStreamConstants.END_DOCUMENT:
                        parser.close();
                        return "";

                    case XMLStreamConstants.START_ELEMENT:
                        actionElement = new QName(parser.getNamespaceURI(), parser.getLocalName());
                        break;

                    case XMLStreamConstants.CHARACTERS:
                        if (WsAddressingConstants.ACTION.equals(actionElement) && !parser.isWhiteSpace()) {
                            return parser.getText();
                        }
                        break;

                    default:
                        break;
                }
                parser.next();
            }
        } catch (XMLStreamException ignored) {
            // Left empty on purpose - it's uncritical if an exception is thrown
        }

        return ACTION_UNKNOWN;
    }

    /**
     * Creates a more readable and short version of the given action.
     *
     * @param action the action to parse.
     * @return a more readable version in accordance with:
     * <ul>
     * <li>If the action is specific to DPWS, a custom identifier is derived.
     * <li>If the action is unknown to DPWS, the last URI context path element is returned.
     * <li>The given action string if parsing failed.
     * </ul>
     */
    String moreReadable(String action) {
        switch (action) {
            case ACTION_UNKNOWN:
            case EMPTY_XML:
                return action;

            // WS-Eventing
            case WsEventingConstants.WSA_ACTION_SUBSCRIBE:
                return "WseSubscribe";
            case WsEventingConstants.WSA_ACTION_SUBSCRIBE_RESPONSE:
                return "WseSubscribeResponse";
            case WsEventingConstants.WSA_ACTION_RENEW:
                return "WseRenew";
            case WsEventingConstants.WSA_ACTION_RENEW_RESPONSE:
                return "WseRenewResponse";
            case WsEventingConstants.WSA_ACTION_GET_STATUS:
                return "WseGetStatus";
            case WsEventingConstants.WSA_ACTION_GET_STATUS_RESPONSE:
                return "WseGetStatusResponse";
            case WsEventingConstants.WSA_ACTION_UNSUBSCRIBE:
                return "WseUnsubscribe";
            case WsEventingConstants.WSA_ACTION_UNSUBSCRIBE_RESPONSE:
                return "WseUnsubscribeResponse";
            case WsEventingConstants.WSA_ACTION_SUBSCRIPTION_END:
                return "WseSubscriptionEnd";

            // WS-MetadataExchange
            case WsMetadataExchangeConstants.WSA_ACTION_GET_METADATA_REQUEST:
                return "MexRequest";
            case WsMetadataExchangeConstants.WSA_ACTION_GET_METADATA_RESPONSE:
                return "MexResponse";

            // WS-Transfer
            case WsTransferConstants.WSA_ACTION_GET:
                return "WstGet";
            case WsTransferConstants.WSA_ACTION_GET_RESPONSE:
                return "WstGetResponse";

            // WS-Discovery
            case WsDiscoveryConstants.WSA_ACTION_PROBE:
                return "WsdProbe";
            case WsDiscoveryConstants.WSA_ACTION_PROBE_MATCHES:
                return "WsdProbeMatches";
            case WsDiscoveryConstants.WSA_ACTION_RESOLVE:
                return "WsdResolve";
            case WsDiscoveryConstants.WSA_ACTION_RESOLVE_MATCHES:
                return "WsdResolveMatches";
            case WsDiscoveryConstants.WSA_ACTION_HELLO:
                return "WsdHello";
            case WsDiscoveryConstants.WSA_ACTION_BYE:
                return "WsdBye";
            default:
                break;
        }

        var indexLastSlash = action.lastIndexOf('/');
        if (indexLastSlash != -1) {
            return action.substring(indexLastSlash + 1);
        }

        return action;
    }

    /**
     * Accepts XML code and pretty-prints it with indentation of 4 characters.
     *
     * @param unformattedXml any unformatted/part-formatted XML.
     * @return pretty-printed XML or the unformatted code if an error ensues.
     */
    byte[] prettyPrint(byte[] unformattedXml) {
        if (unformattedXml.length == 0) {
            return unformattedXml;
        }

        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", 4);
            // #218 prevent XXE attacks
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            var output = new ByteArrayOutputStream();
            StreamResult xmlOutput = new StreamResult(output);

            Source xmlInput = new StreamSource(new ByteArrayInputStream(unformattedXml));
            transformer.transform(xmlInput, xmlOutput);

            return output.toByteArray();
            // CHECKSTYLE.OFF: IllegalCatch
        } catch (Exception e) {
            // CHECKSTYLE.ON: IllegalCatch
            return unformattedXml;
        }
    }
}
