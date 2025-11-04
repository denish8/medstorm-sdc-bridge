package org.somda.sdc.dpws.soap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.common.guice.AbstractConfigurationModule;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.NetworkSinkMock;
import org.somda.sdc.dpws.helper.JaxbMarshalling;
import org.somda.sdc.dpws.soap.factory.EnvelopeFactory;
import org.somda.sdc.dpws.soap.model.Envelope;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants;
import org.somda.sdc.dpws.soap.wsdiscovery.model.HelloType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ObjectFactory;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.MarshalException;
import javax.xml.bind.UnmarshalException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JaxbSoapMarshallingSchemaTest extends DpwsTest {
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        overrideBindings(new AbstractConfigurationModule() {
            @Override
            protected void defaultConfigure() {
                bind(SoapConfig.VALIDATE_SOAP_MESSAGES,
                        Boolean.class,
                        true);
            }
        });
        super.setUp();
        getInjector().getInstance(JaxbMarshalling.class).startAsync().awaitRunning();
        getInjector().getInstance(SoapMarshalling.class).startAsync().awaitRunning();
    }

    @Test
    void marshallValidMessage() throws Exception {
        ObjectFactory wsdFactory = getInjector().getInstance(ObjectFactory.class);
        // create a hello message with EndpointReference present
        HelloType helloType = wsdFactory.createHelloType();

        var wsaFactory = getInjector().getInstance(org.somda.sdc.dpws.soap.wsaddressing.model.ObjectFactory.class);
        var expectedEpr = wsaFactory.createEndpointReferenceType();
        var eprAddress = wsaFactory.createAttributedURIType();
        eprAddress.setValue("http://test-xAddr1");
        expectedEpr.setAddress(eprAddress);
        helloType.setEndpointReference(expectedEpr);

        List<String> xAddrs = new ArrayList<>();
        xAddrs.add("http://test-xAddr1");
        xAddrs.add("http://test-xAddr2");
        helloType.setXAddrs(xAddrs);
        JAXBElement<HelloType> expectedHello = wsdFactory.createHello(helloType);

        EnvelopeFactory envelopeFactory = getInjector().getInstance(EnvelopeFactory.class);
        Envelope expectedEnvelope = envelopeFactory.createEnvelope(WsDiscoveryConstants.WSA_ACTION_HELLO,
                WsDiscoveryConstants.WSA_UDP_TO, expectedHello);

        SoapMarshalling marshalling = getInjector().getInstance(SoapMarshalling.class);
        NetworkSinkMock mockNetworkSink = getInjector().getInstance(NetworkSinkMock.class);

        try (OutputStream os = mockNetworkSink.createOutputStream()) {
            marshalling.marshal(expectedEnvelope, os);
        }
    }

    @Test
    void marshallInvalidMessage() throws Exception {
        ObjectFactory wsdFactory = getInjector().getInstance(ObjectFactory.class);

        // create a hello message with missing EndpointReference, which should trigger a marshalling error
        HelloType helloType = wsdFactory.createHelloType();
        List<String> xAddrs = new ArrayList<>();
        xAddrs.add("http://test-xAddr1");
        xAddrs.add("http://test-xAddr2");
        helloType.setXAddrs(xAddrs);
        JAXBElement<HelloType> expectedHello = wsdFactory.createHello(helloType);

        EnvelopeFactory envelopeFactory = getInjector().getInstance(EnvelopeFactory.class);
        Envelope expectedEnvelope = envelopeFactory.createEnvelope(WsDiscoveryConstants.WSA_ACTION_HELLO,
                WsDiscoveryConstants.WSA_UDP_TO, expectedHello);

        SoapMarshalling marshalling = getInjector().getInstance(SoapMarshalling.class);
        NetworkSinkMock mockNetworkSink = getInjector().getInstance(NetworkSinkMock.class);

        assertThrows(MarshalException.class, () -> {
            try (OutputStream os = mockNetworkSink.createOutputStream()) {
                marshalling.marshal(expectedEnvelope, os);
            }
        });
    }

    @Test
    void testUnmarshallingValidMessage() throws Exception {
        ObjectFactory wsdFactory = getInjector().getInstance(ObjectFactory.class);
        // create a hello message with EndpointReference present
        HelloType helloType = wsdFactory.createHelloType();

        var wsaFactory = getInjector().getInstance(org.somda.sdc.dpws.soap.wsaddressing.model.ObjectFactory.class);
        var expectedEpr = wsaFactory.createEndpointReferenceType();
        var eprAddress = wsaFactory.createAttributedURIType();
        eprAddress.setValue("http://test-xAddr1");
        expectedEpr.setAddress(eprAddress);
        helloType.setEndpointReference(expectedEpr);

        List<String> xAddrs = new ArrayList<>();
        xAddrs.add("http://test-xAddr1");
        xAddrs.add("http://test-xAddr2");
        helloType.setXAddrs(xAddrs);
        JAXBElement<HelloType> expectedHello = wsdFactory.createHello(helloType);

        EnvelopeFactory envelopeFactory = getInjector().getInstance(EnvelopeFactory.class);
        Envelope expectedEnvelope = envelopeFactory.createEnvelope(WsDiscoveryConstants.WSA_ACTION_HELLO,
                WsDiscoveryConstants.WSA_UDP_TO, expectedHello);

        SoapMarshalling marshalling = getInjector().getInstance(SoapMarshalling.class);

        var os = new ByteArrayOutputStream();
        marshalling.marshal(expectedEnvelope, os);

        System.out.println(new String(os.toByteArray()));

        var is = new ByteArrayInputStream(os.toByteArray());

        var result = marshalling.unmarshal(is);
        JAXBElement<HelloType> resultHello = (JAXBElement<HelloType>) result.getBody().getAny().get(0);

        assertEquals(resultHello.getValue().getEndpointReference().getAddress().getValue(),
                expectedEpr.getAddress().getValue());
    }

    @Test
    void unmarshallInvalidMessage() throws Exception {

        // missing EndpointReference but otherwise valid message
        final String wrongMessage = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<s12:Envelope xmlns:s12=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                "    xmlns:wsa=\"http://www.w3.org/2005/08/addressing\" \n" +
                "    xmlns:dpws=\"http://docs.oasis-open.org/ws-dd/ns/dpws/2009/01\" \n" +
                "    xmlns:ns4=\"http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01\" \n" +
                "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\" \n" +
                "    xmlns:wst=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" \n" +
                "    xmlns:wsm=\"http://schemas.xmlsoap.org/ws/2004/09/mex\" \n" +
                "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
                "    xmlns:wsd=\"http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01/\">\n" +
                "    <s12:Header>\n" +
                "        <wsa:Action>http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01/Hello</wsa:Action>\n" +
                "        <wsa:To>urn:docs-oasis-open-org:ws-dd:ns:discovery:2009:01</wsa:To>\n" +
                "    </s12:Header>\n" +
                "    <s12:Body>\n" +
                "        <ns4:Hello>\n" +
                "            <ns4:XAddrs>http://test-xAddr1 http://test-xAddr2</ns4:XAddrs>\n" +
                "            <ns4:MetadataVersion>0</ns4:MetadataVersion>\n" +
                "        </ns4:Hello>\n" +
                "    </s12:Body>\n" +
                "</s12:Envelope>\n";

        var is = new ByteArrayInputStream(wrongMessage.getBytes(Charset.defaultCharset()));
        SoapMarshalling marshalling = getInjector().getInstance(SoapMarshalling.class);

        assertThrows(UnmarshalException.class, () -> {
            marshalling.unmarshal(is);
        });
    }
}
