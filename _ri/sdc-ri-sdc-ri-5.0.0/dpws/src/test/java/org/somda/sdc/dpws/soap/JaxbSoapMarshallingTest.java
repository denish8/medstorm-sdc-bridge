package org.somda.sdc.dpws.soap;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.somda.sdc.common.guice.AbstractConfigurationModule;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.NetworkSinkMock;
import org.somda.sdc.dpws.helper.JaxbMarshalling;
import org.somda.sdc.dpws.soap.factory.EnvelopeFactory;
import org.somda.sdc.dpws.soap.factory.SoapFaultFactory;
import org.somda.sdc.dpws.soap.model.Envelope;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants;
import org.somda.sdc.dpws.soap.wsdiscovery.model.HelloType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ObjectFactory;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JaxbSoapMarshallingTest extends DpwsTest {
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        overrideBindings(new AbstractConfigurationModule() {
            @Override
            protected void defaultConfigure() {
                // no need for validation as tests to test general marshalling
                bind(SoapConfig.VALIDATE_SOAP_MESSAGES,
                        Boolean.class,
                        false);
            }
        });
        super.setUp();
        getInjector().getInstance(JaxbMarshalling.class).startAsync().awaitRunning();
        getInjector().getInstance(SoapMarshalling.class).startAsync().awaitRunning();
    }

    @SuppressWarnings("unchecked")
    @Test
    void marshallCompositeSoapMessage() throws JAXBException, IOException {
        ObjectFactory wsdFactory = getInjector().getInstance(ObjectFactory.class);
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

        try (OutputStream os = mockNetworkSink.createOutputStream()) {
            marshalling.marshal(expectedEnvelope, os);
        }

        try (InputStream inputStreams = mockNetworkSink.getLatest()) {
            Envelope actualEnvelope = marshalling.unmarshal(inputStreams);
            assertEquals(1, actualEnvelope.getBody().getAny().size());

            JAXBElement<HelloType> actualHello = (JAXBElement<HelloType>) actualEnvelope.getBody().getAny().get(0);
            assertThat(expectedHello.getValue().getXAddrs(), is(actualHello.getValue().getXAddrs()));
        }
    }

    @Test
    void marshalFault() throws Exception {
        SoapFaultFactory sff = getInjector().getInstance(SoapFaultFactory.class);
        SoapMessage faultMsg = sff.createReceiverFault("Test");
        SoapMarshalling marshalling = getInjector().getInstance(SoapMarshalling.class);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Envelope envelopeWithMappedHeaders = faultMsg.getEnvelopeWithMappedHeaders();
        marshalling.marshal(envelopeWithMappedHeaders, bos);
        System.out.println(bos);
        assertTrue(true);
    }

    @Test
    @DisplayName("Test whether a marshalled message created using SoapUtil contains the Action element only once")
    void testDuplicateHeaders() throws Exception {
        var action = "ftp://somda.org/upload";
        var soapUtil = getInjector().getInstance(SoapUtil.class);
        SoapMarshalling marshalling = getInjector().getInstance(SoapMarshalling.class);

        var message = soapUtil.createMessage(action);

        var messageBaos = new ByteArrayOutputStream();
        marshalling.marshal(message.getEnvelopeWithMappedHeaders(), messageBaos);

        var messageString = messageBaos.toString(StandardCharsets.UTF_8);

        // one opening and one closing tag
        assertEquals(2, StringUtils.countMatches(messageString, ":Action>"));
    }

    @Test
    @DisplayName("Ignore XML Prolog if Reader encoding is set")
    void testOverrideXmlPrologEncoding() throws Exception {
        // create a utf-16 message with utf-8 prolog
        // chose a character with different encoding in utf-8 and utf-16le, it must survive the translation in tact
        // unicode:  U+00E5
        // utf-8:    0xC3 0xA5
        // utf-16le: 0xE5 0x00
        var unicodeCharacter = Character.toString((char) 0x00E5);
        var action = "ftp://somda.org/upload" + unicodeCharacter;
        var soapUtil = getInjector().getInstance(SoapUtil.class);
        SoapMarshalling marshalling = getInjector().getInstance(SoapMarshalling.class);
        var message = soapUtil.createMessage(action);
        var messageBaos = new ByteArrayOutputStream();
        marshalling.marshal(message.getEnvelopeWithMappedHeaders(), messageBaos);

        var messageString = messageBaos.toString(StandardCharsets.UTF_8);
        var messageBytes16 = messageString.getBytes(StandardCharsets.UTF_16LE);

        var inputStream = new ByteArrayInputStream(messageBytes16);
        var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_16LE);

        var messageAgain = assertDoesNotThrow(() -> marshalling.unmarshal(reader));
        var soapMessageAgain = soapUtil.createMessage(messageAgain);

        assertEquals(action, soapMessageAgain.getWsAddressingHeader().getAction().get().getValue());
    }
}
