package org.somda.sdc.dpws.soap.factory;

import com.google.inject.Inject;
import org.somda.sdc.dpws.soap.SoapConstants;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.model.Detail;
import org.somda.sdc.dpws.soap.model.Fault;
import org.somda.sdc.dpws.soap.model.Faultcode;
import org.somda.sdc.dpws.soap.model.Faultreason;
import org.somda.sdc.dpws.soap.model.ObjectFactory;
import org.somda.sdc.dpws.soap.model.Reasontext;
import org.somda.sdc.dpws.soap.model.Subcode;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingConstants;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory to create SOAP fault structures.
 * <ul>
 * <li>Parameter names are in accordance with the SOAP fault specification. Other parameter names are explained.
 * <li>The implied reason text's language is 'en'.
 * </ul>
 * @see <a href="https://www.w3.org/TR/2007/REC-soap12-part1-20070427/#soapfault">SOAP Fault</a>
 * @see <a href="https://www.w3.org/TR/2006/REC-ws-addr-soap-20060509/#id2270167">SOAP 1.2 Fault Binding</a>
 */
public class SoapFaultFactory {
    private final ObjectFactory soapFactory;
    private final SoapUtil soapUtil;

    @Inject
    SoapFaultFactory(ObjectFactory soapFactory,
                     SoapUtil soapUtil) {
        this.soapFactory = soapFactory;
        this.soapUtil = soapUtil;
    }

    /**
     * Creates a fault SOAP message based on an action URI, code, subcode, reason text and arbitrary details.
     *
     * @param actionUri wsa:Action to put into the resulting {@link SoapMessage} header.
     * @param code the SOAP fault code.
     * @param subcode the SOAP fault subcode.
     * @param reasonText the fault reason.
     * @param detail the fault details.
     * @return a {@link SoapMessage} containing the fault.
     */
    public SoapMessage createFault(String actionUri, QName code, QName subcode, String reasonText,
                                   @Nullable Object detail) {
        Subcode scObj = new Subcode();
        scObj.setValue(subcode);

        Faultcode fcObj = new Faultcode();
        fcObj.setValue(code);
        fcObj.setSubcode(scObj);

        Reasontext rtObj = new Reasontext();
        rtObj.setValue(reasonText);
        rtObj.setLang("en");
        List<Reasontext> rtListObj = new ArrayList<>();
        rtListObj.add(rtObj);

        Faultreason frObj = new Faultreason();
        frObj.setText(rtListObj);

        Fault f = new Fault();
        f.setCode(fcObj);
        f.setReason(frObj);

        if (detail != null) {
            List<Object> anyObjList = new ArrayList<>();
            anyObjList.add(detail);
            Detail dObj = new Detail();
            dObj.setAny(anyObjList);
            f.setDetail(dObj);
        }

        return soapUtil.createMessage(actionUri, soapFactory.createFault(f));
    }

    /**
     * Creates a fault SOAP message based on an action URI, code, subcode and reason text.
     *
     * @param actionUri wsa:Action to put into the resulting {@link SoapMessage} header.
     * @param code the SOAP fault code.
     * @param subcode the SOAP fault subcode.
     * @param reasonText the fault reason.
     * @return a {@link SoapMessage} containing the fault.
     */
    public SoapMessage createFault(String actionUri, QName code, QName subcode, String reasonText) {
        return createFault(actionUri, code, subcode, reasonText, null);
    }

    /**
     * Creates a fault SOAP message based on an action URI, code and subcode.
     *
     * @param actionUri wsa:Action to put into the resulting {@link SoapMessage} header.
     * @param code the SOAP fault code.
     * @param subcode the SOAP fault subcode.
     * @return a {@link SoapMessage} containing the fault.
     */
    public SoapMessage createFault(String actionUri, QName code, QName subcode) {
        return createFault(actionUri, code, subcode, "", null);
    }

    /**
     * Creates a soap:Receiver fault with a given text.
     *
     * @param reasonText the fault reason.
     * @return a {@link SoapMessage} containing the fault.
     */
    public SoapMessage createReceiverFault(String reasonText) {
        return createFault(
                WsAddressingConstants.FAULT_ACTION, SoapConstants.RECEIVER,
                SoapConstants.DEFAULT_SUBCODE, reasonText
        );
    }

    /**
     * Creates a soap:Sender fault with a given text.
     *
     * @param reasonText the fault reason.
     * @return a {@link SoapMessage} containing the fault.
     */
    public SoapMessage createSenderFault(String reasonText) {
        return createFault(
                WsAddressingConstants.FAULT_ACTION, SoapConstants.SENDER,
                SoapConstants.DEFAULT_SUBCODE, reasonText
        );
    }
}
