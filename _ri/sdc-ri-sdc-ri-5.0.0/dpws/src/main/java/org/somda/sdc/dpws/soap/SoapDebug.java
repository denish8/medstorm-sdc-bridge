package org.somda.sdc.dpws.soap;

import org.somda.sdc.dpws.soap.wsaddressing.model.AttributedURIType;

import javax.xml.bind.JAXBElement;
import java.util.Optional;

/**
 * Utility class to create textual representations of {@link SoapMessage} objects.
 */
public class SoapDebug {
    /**
     * Gets textual representation of a given SOAP message.
     *
     * @param msg the message to create a text representation from.
     * @return string consisting of the message's action, message id and message type.
     */
    public static String get(SoapMessage msg) {
        StringBuilder sb = new StringBuilder();
        sb.append("SoapMsg(");

        appendAction(sb, msg);
        appendMsgId(sb, msg);
        appendMsgType(sb, msg);

        sb.append(")");
        return sb.toString();
    }

    /**
     * Gets brief textual representation of a given SOAP message.
     *
     * @param msg the message to create a text representation from.
     * @return string consisting of the message's message id.
     */
    public static String getBrief(SoapMessage msg) {
        StringBuilder sb = new StringBuilder();
        sb.append("SoapMsg(");

        Optional<AttributedURIType> uri = msg.getWsAddressingHeader().getMessageId();
        if (uri.isPresent()) {
            sb.append(uri.get().getValue());
        } else {
            sb.append("<unknown>");
        }

        var relatesToOptional = msg.getWsAddressingHeader().getRelatesTo();
        relatesToOptional.ifPresent(relatesTo -> sb.append(" --> ").append(relatesTo.getValue()));

        sb.append(")");
        return sb.toString();
    }

    private static void appendAction(StringBuilder sb, SoapMessage msg) {
        msg.getWsAddressingHeader().getAction().ifPresent(uri ->
                sb.append(String.format("action=[%s];", uri.getValue())));
    }

    private static void appendMsgId(StringBuilder sb, SoapMessage msg) {
        msg.getWsAddressingHeader().getMessageId().ifPresent(uri ->
                sb.append(String.format("msgId=[%s];", uri.getValue())));
    }

    private static void appendMsgType(StringBuilder sb, SoapMessage msg) {
        if (msg.getOriginalEnvelope().getBody().getAny().size() == 0) {
            sb.append("bodyType=[n/a];");
        } else {
            Object obj = msg.getOriginalEnvelope().getBody().getAny().get(0);
            String name = obj.getClass().getSimpleName();
            if (obj instanceof JAXBElement) {
                name = ((JAXBElement) obj).getName().toString();
            }
            sb.append(String.format("bodyType=[%s];", name));
        }
    }
}
