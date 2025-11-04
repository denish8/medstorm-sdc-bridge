package it.org.somda.sdc.dpws;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.somda.sdc.dpws.CommunicationLogContext;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.TransportBinding;
import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.helper.JaxbMarshalling;
import org.somda.sdc.dpws.http.apache.ClientTransportBinding;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.factory.EnvelopeFactory;
import org.somda.sdc.dpws.soap.factory.SoapMessageFactory;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class ApacheHttpClientTransportBindingFactoryImplIT extends DpwsTest {

    private TransportBindingFactory transportBindingFactory;
    private SoapMessageFactory soapMessageFactory;
    private EnvelopeFactory envelopeFactory;
    private SoapMarshalling marshalling;

    @BeforeEach
    public void setUp() throws Exception {
        var override = new DefaultDpwsConfigModule() {
            @Override
            public void customConfigure() {
                bind(DpwsConfig.HTTP_GZIP_COMPRESSION, Boolean.class, true);
            }
        };
        this.overrideBindings(override);
        super.setUp();
        transportBindingFactory = getInjector().getInstance(TransportBindingFactory.class);
        soapMessageFactory = getInjector().getInstance(SoapMessageFactory.class);
        envelopeFactory = getInjector().getInstance(EnvelopeFactory.class);
        getInjector().getInstance(JaxbMarshalling.class).startAsync().awaitRunning();
        marshalling = getInjector().getInstance(SoapMarshalling.class);
        marshalling.startAsync().awaitRunning();
    }

    @Test
    void testGzipCompression() throws Exception {
        URI baseUri = URI.create("http://127.0.0.1:0/");

        String expectedResponse = "Sehr geehrter Kaliba, netter Versuch\n" +
                "Kritische Texte, Weltverbesserer-Blues;";

        JAXBElement<String> jaxbElement = new JAXBElement<>(
                new QName("root-element"),
                String.class, expectedResponse
        );

        var responseEnvelope = createASoapMessage();
        responseEnvelope.getOriginalEnvelope().getBody().getAny().add(jaxbElement);

        // make bytes out of the expected response
        ByteArrayOutputStream expectedResponseStream = new ByteArrayOutputStream();
        marshalling.marshal(responseEnvelope.getEnvelopeWithMappedHeaders(), expectedResponseStream);

        var responseBytes = expectedResponseStream.toByteArray();

        // spawn the http server
        HttpServerUtil.GzipResponseHandler handler = new HttpServerUtil.GzipResponseHandler(responseBytes);
        var inetSocketAddress = new InetSocketAddress(baseUri.getHost(), baseUri.getPort());
        var server = HttpServerUtil.spawnHttpServer(inetSocketAddress, handler);

        // replace the port
        baseUri = new URI(
                baseUri.getScheme(),
                baseUri.getUserInfo(),
                baseUri.getHost(),
                server.getAddress().getPort(),
                baseUri.getPath(),
                baseUri.getQuery(),
                baseUri.getFragment());

        // make request to our server
        TransportBinding httpBinding1 = transportBindingFactory.createHttpBinding(baseUri.toString(), null);
        SoapMessage response = httpBinding1.onRequestResponse(createASoapMessage());

        ByteArrayOutputStream actualResponseStream = new ByteArrayOutputStream();
        marshalling.marshal(response.getEnvelopeWithMappedHeaders(), actualResponseStream);

        // response bytes should exactly match our expected bytes, transparently decompressed
        assertArrayEquals(expectedResponseStream.toByteArray(), actualResponseStream.toByteArray());
    }

    @Test
    @DisplayName("test that factory uses same client for all bindings")
    void testNoDuplicateClient() throws Exception {
        URI baseUri = URI.create("http://127.0.0.1:5000/");

        // create binding and retrieve http client
        ClientTransportBinding httpBinding1 = (ClientTransportBinding) transportBindingFactory.createHttpBinding(
                baseUri.toString(),
                null
        );
        final var client1 = FieldUtils.readDeclaredField(httpBinding1, "client", true);

        // create another binding with a communication context, client must remain the same
        final var ctx = new CommunicationLogContext("dummy");
        ClientTransportBinding httpBinding2 = (ClientTransportBinding) transportBindingFactory.createHttpBinding(
                baseUri.toString(),
                ctx
        );
        final var client2 = FieldUtils.readDeclaredField(httpBinding2, "client", true);

        assertNotNull(client1);
        assertSame(client1, client2);
    }

    private SoapMessage createASoapMessage() {
        return soapMessageFactory.createSoapMessage(envelopeFactory.createEnvelope());
    }
}
