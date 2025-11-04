package it.org.somda.sdc.dpws.soap;

import com.google.inject.Inject;
import dpws_test_service.messages._2017._05._10.ObjectFactory;
import dpws_test_service.messages._2017._05._10.TestNotification;
import dpws_test_service.messages._2017._05._10.TestOperationRequest;
import dpws_test_service.messages._2017._05._10.TestOperationResponse;
import it.org.somda.sdc.dpws.TestServiceMetadata;
import org.somda.sdc.dpws.device.WebService;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.factory.SoapFaultFactory;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wseventing.ActionBasedEventSource;


public class DpwsTestService2 extends WebService {

    private final WsAddressingUtil wsaUtil;
    private final SoapUtil soapUtil;
    private final SoapFaultFactory soapFaultFactory;
    private final ObjectFactory objectFactory;
    private final ActionBasedEventSource actionBasedEventSource;

    @Inject
    DpwsTestService2(WsAddressingUtil wsaUtil,
                     SoapUtil soapUtil,
                     SoapFaultFactory soapFaultFactory,
                     ObjectFactory objectFactory,
                     ActionBasedEventSource actionBasedEventSource) {
        this.wsaUtil = wsaUtil;
        this.actionBasedEventSource = actionBasedEventSource;
        this.soapUtil = soapUtil;
        this.soapFaultFactory = soapFaultFactory;
        this.objectFactory = objectFactory;
        this.registerEventSource(actionBasedEventSource);
    }

    public void sendNotification(TestNotification notificationToSend) throws MarshallingException, TransportException {
        actionBasedEventSource.sendNotification(TestServiceMetadata.ACTION_NOTIFICATION_3, notificationToSend);
    }

    @MessageInterceptor(TestServiceMetadata.ACTION_OPERATION_REQUEST_3)
    void onTestOperation(RequestResponseObject rrObj) throws SoapFaultException {
        rrObj.getResponse().getWsAddressingHeader()
                .setAction(wsaUtil.createAttributedURIType(TestServiceMetadata.ACTION_OPERATION_RESPONSE_3));

        TestOperationRequest req = soapUtil.getBody(rrObj.getRequest(), TestOperationRequest.class).orElseThrow(() ->
                new SoapFaultException(soapFaultFactory.createSenderFault("SOAP body is malformed."),
                        rrObj.getRequest().getWsAddressingHeader().getMessageId().orElse(null)));

        TestOperationResponse res = objectFactory.createTestOperationResponse();
        res.setResult1(new StringBuilder(req.getParam1()).append(req.getParam1()).reverse().toString());
        res.setResult2(req.getParam2() * 8);

        soapUtil.setBody(res, rrObj.getResponse());
    }
}
