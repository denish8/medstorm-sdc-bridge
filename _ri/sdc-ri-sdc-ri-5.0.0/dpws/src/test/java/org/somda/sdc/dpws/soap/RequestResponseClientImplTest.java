package org.somda.sdc.dpws.soap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.helper.JaxbMarshalling;
import org.somda.sdc.dpws.soap.factory.RequestResponseClientFactory;
import org.somda.sdc.dpws.soap.interception.Direction;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.RequestObject;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.dpws.soap.model.Envelope;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.AttributedURIType;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class RequestResponseClientImplTest extends DpwsTest {
    private List<String> dispatchedSequence;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        getInjector().getInstance(JaxbMarshalling.class).startAsync().awaitRunning();
        getInjector().getInstance(SoapMarshalling.class).startAsync().awaitRunning();
        dispatchedSequence = new ArrayList<>();
    }

    @Test
    void sendRequestResponse() throws Exception {
        SoapMarshalling unmarshaller = getInjector().getInstance(SoapMarshalling.class);
        Envelope soapEnv = unmarshaller.unmarshal(getClass().getResourceAsStream("soap-envelope.xml"));

        SoapUtil soapUtil = getInjector().getInstance(SoapUtil.class);
        SoapMessage request = soapUtil.createMessage(soapEnv);
        final SoapMessage response = soapUtil.createMessage();
        AttributedURIType responseAction = getInjector().getInstance(WsAddressingUtil.class)
                .createAttributedURIType("http://response-action");
        response.getWsAddressingHeader().setAction(responseAction);

        RequestResponseClientFactory clientFactory = getInjector().getInstance(RequestResponseClientFactory.class);
        RequestResponseClient rrClient = clientFactory.createRequestResponseClient(req -> {
            dispatchedSequence.add("NETWORK");
            return response;
        });

        rrClient.register(new Interceptor() {
            @MessageInterceptor(direction = Direction.REQUEST)
            void onDelete(RequestObject rInfo) {
                dispatchedSequence.add("REQUEST(MAX)");
            }
        });

        rrClient.register(new Interceptor() {
            @MessageInterceptor(direction = Direction.REQUEST, sequenceNumber = 5)
            void onDelete(RequestObject rInfo) {
                dispatchedSequence.add("REQUEST(5)");
            }
        });

        rrClient.register(new Interceptor() {
            @MessageInterceptor(value = "http://example.com/fabrikam/mail/Delete", direction = Direction.REQUEST)
            void onDelete(RequestObject rInfo) {
                dispatchedSequence.add("REQUEST(ACTION, MAX)");
            }
        });

        // Shall be skipped since argument is missing
        rrClient.register(new Interceptor() {
            @MessageInterceptor(value = "http://example.com/fabrikam/mail/Delete", direction = Direction.REQUEST)
            void onDelete() {
                dispatchedSequence.add("INVALID REQUEST(ACTION, MAX)");
            }
        });

        // Shall be skipped since argument is RequestResponse, but should be Request
        rrClient.register(new Interceptor() {
            @MessageInterceptor(value = "http://example.com/fabrikam/mail/Delete", direction = Direction.REQUEST)
            void onDelete(RequestResponseObject rrInfo) {
                dispatchedSequence.add("INVALID REQUEST(ACTION, MAX)");
            }
        });

        rrClient.register(new Interceptor() {
            @MessageInterceptor(direction = Direction.RESPONSE)
            void onDelete(RequestResponseObject rrInfo) {
                dispatchedSequence.add("RESPONSE(MAX)");
            }
        });

        // Shall be skipped since response action is not "http://example.com/fabrikam/mail/Delete"
        rrClient.register(new Interceptor() {
            @MessageInterceptor(value = "http://example.com/fabrikam/mail/Delete", direction = Direction.RESPONSE)
            void onDelete(RequestResponseObject rrInfo) {
                dispatchedSequence.add("INVALID RESPONSE(ACTION, MAX)");
            }
        });

        rrClient.register(new Interceptor() {
            @MessageInterceptor(value = "http://response-action", direction = Direction.RESPONSE)
            void onDelete(RequestResponseObject rrInfo) {
                dispatchedSequence.add("RESPONSE(ACTION,MAX)");
            }
        });

        assertSame(response, rrClient.sendRequestResponse(request));

        assertEquals(6, dispatchedSequence.size());
        assertEquals("REQUEST(5)", dispatchedSequence.get(0));
        assertEquals("REQUEST(MAX)", dispatchedSequence.get(1));
        assertEquals("REQUEST(ACTION, MAX)", dispatchedSequence.get(2));
        assertEquals("NETWORK", dispatchedSequence.get(3));
        assertEquals("RESPONSE(MAX)", dispatchedSequence.get(4));
        assertEquals("RESPONSE(ACTION,MAX)", dispatchedSequence.get(5));
    }
}