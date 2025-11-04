package org.somda.sdc.dpws.soap.wsaddressing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.factory.EnvelopeFactory;
import org.somda.sdc.dpws.soap.factory.RequestResponseClientFactory;
import org.somda.sdc.dpws.soap.factory.SoapMessageFactory;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WsAddressingClientInterceptorTest extends DpwsTest {

    private RequestResponseClient client;
    private EnvelopeFactory envelopeFactory;
    private SoapMessageFactory soapMessageFactory;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        envelopeFactory = getInjector().getInstance(EnvelopeFactory.class);
        soapMessageFactory = getInjector().getInstance(SoapMessageFactory.class);
        RequestResponseClientFactory requestResponseClientFactory = getInjector()
                .getInstance(RequestResponseClientFactory.class);

        client = requestResponseClientFactory.createRequestResponseClient(request -> {
            SoapMessage response = soapMessageFactory.createSoapMessage(
                    envelopeFactory.createEnvelope("http://response", null));
            request.getWsAddressingHeader().getMessageId().ifPresent(attributedURIType ->
                    response.getWsAddressingHeader().setMessageId(attributedURIType));
            return response;
        });
        client.register(getInjector().getInstance(WsAddressingClientInterceptor.class));
    }

    @Test
    void processMessage() throws Exception {
        SoapMessage req = soapMessageFactory.createSoapMessage(envelopeFactory
                .createEnvelope("http://request", null));
        SoapMessage res = client.sendRequestResponse(req);
        assertTrue(res.getWsAddressingHeader().getMessageId().isPresent());
    }
}