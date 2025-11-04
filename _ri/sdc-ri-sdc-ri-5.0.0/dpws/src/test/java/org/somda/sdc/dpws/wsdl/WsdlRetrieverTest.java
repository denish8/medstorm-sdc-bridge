package org.somda.sdc.dpws.wsdl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.util.concurrent.Futures;
import com.google.inject.AbstractModule;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.helper.JaxbMarshalling;
import org.somda.sdc.dpws.http.ContentType;
import org.somda.sdc.dpws.http.HttpClient;
import org.somda.sdc.dpws.http.HttpResponse;
import org.somda.sdc.dpws.http.factory.HttpClientFactory;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.model.Envelope;
import org.somda.sdc.dpws.soap.wsaddressing.model.ReferenceParametersType;
import org.somda.sdc.dpws.soap.wsmetadataexchange.model.Metadata;
import org.somda.sdc.dpws.soap.wstransfer.TransferGetClient;

import javax.xml.bind.JAXBElement;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("UnstableApiUsage")
class WsdlRetrieverTest extends DpwsTest {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private static final String METADATA_TEMPLATE;

    static {
        try (var stream = WsdlRetrieverTest.class.getResourceAsStream("WsdlRetrieverTest/MetadataWsdlTemplate.xml")) {
            METADATA_TEMPLATE = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final String METADATA_SECTION_WSDL = ""
            + "<mex:MetadataSection Dialect='http://schemas.xmlsoap.org/wsdl/'>"
            + "%s"
            + "</mex:MetadataSection>";

    private static final String METADATA_WSDL_LOCATION = "<mex:Location>%s</mex:Location>";

    private static final String METADATA_REFERENCE_ADDRESS = ""
            + "<mex:MetadataReference>"
            + "<wsa10:Address>%s</wsa10:Address>"
            + "<wsa10:ReferenceParameters>"
            + "<wse:Identifier>%s</wse:Identifier>"
            + "</wsa10:ReferenceParameters>"
            + "</mex:MetadataReference>";

    private static final String METADATA_REFERENCE_ADDRESS_NO_REF_PARM = ""
            + "<mex:MetadataReference>"
            + "<wsa10:Address>%s</wsa10:Address>"
            + "</mex:MetadataReference>";

    private SoapMarshalling marshalling;
    private WsdlRetriever testClass;
    private WsdlMarshalling wsdlMarshalling;
    private JaxbMarshalling baseMarshalling;
    private HttpClient mockHttpClient;
    private TransferGetClient mockTransferGetClient;
    private SoapUtil soapUtil;

    @BeforeEach
    public void setUp() throws Exception {
        var mockFactory = mock(HttpClientFactory.class);
        mockTransferGetClient = mock(TransferGetClient.class);
        var override = new AbstractModule() {
            @Override
            protected void configure() {
                super.configure();
                bind(HttpClientFactory.class).toInstance(mockFactory);
                bind(TransferGetClient.class).toInstance(mockTransferGetClient);
            }
        };
        overrideBindings(override);
        super.setUp();

        mockHttpClient = mock(HttpClient.class);
        when(mockFactory.createHttpClient()).thenReturn(mockHttpClient);

        baseMarshalling = getInjector().getInstance(JaxbMarshalling.class);
        baseMarshalling.startAsync().awaitRunning(DEFAULT_TIMEOUT);

        marshalling = getInjector().getInstance(SoapMarshalling.class);
        marshalling.startAsync().awaitRunning(DEFAULT_TIMEOUT);

        wsdlMarshalling = getInjector().getInstance(WsdlMarshalling.class);
        wsdlMarshalling.startAsync().awaitRunning(DEFAULT_TIMEOUT);

        soapUtil = getInjector().getInstance(SoapUtil.class);

        testClass = getInjector().getInstance(WsdlRetriever.class);
    }

    @AfterEach
    void tearDown() throws TimeoutException {
        marshalling.stopAsync().awaitTerminated(DEFAULT_TIMEOUT);
        wsdlMarshalling.stopAsync().awaitTerminated(DEFAULT_TIMEOUT);
        baseMarshalling.stopAsync().awaitTerminated(DEFAULT_TIMEOUT);
    }

    /**
     * Tests WSDLs embedded into {@linkplain Metadata} message can be extracted and remain intact.
     *
     * @throws Exception on any exception
     */
    @Test
    void testWsdlEmbedded() throws Exception {
        Envelope message;
        try (var messageStream = WsdlRetrieverTest.class.getResourceAsStream("WsdlRetrieverTest/MetadataWsdlEmbedded.xml")) {
            message = marshalling.unmarshal(messageStream);
        }

        var metadata = (Metadata) message.getBody().getAny().get(0);
        var wsdls = testClass.retrieveWsdlFromMetadata(metadata);

        assertEquals(1, wsdls.size());

        var wsdl = wsdls.get(0);
        assertNotNull(wsdl);
        assertFalse(wsdl.isBlank());
        extractWsdl(wsdl);
    }

    /**
     * Tests whether WSDLs at locations are transferred.
     *
     * @throws Exception on any exception
     */
    @Test
    void testWsdlLocation() throws Exception {
        var testResponseContent = "Huhuhihihaha";
        var testLocation = "ftp://some.place/somewhere?overtherainbow";
        var testResponse = mock(HttpResponse.class, Mockito.RETURNS_DEEP_STUBS);
        when(testResponse.getBody()).thenReturn(testResponseContent.getBytes(StandardCharsets.UTF_8));

        when(mockHttpClient.sendGet(any())).thenReturn(testResponse);

        var locationMessage = String.format(
                METADATA_TEMPLATE,
                String.format(
                        METADATA_SECTION_WSDL,
                        String.format(
                                METADATA_WSDL_LOCATION,
                                testLocation
                        )
                )
        );

        Envelope message;
        message = marshalling.unmarshal(new ByteArrayInputStream(locationMessage.getBytes(StandardCharsets.UTF_8)));

        var metadata = (Metadata) message.getBody().getAny().get(0);
        var wsdl = testClass.retrieveWsdlFromMetadata(metadata);

        assertEquals(List.of(testResponseContent), wsdl);

        // correct endpoint called
        var captor = ArgumentCaptor.forClass(String.class);
        verify(mockHttpClient).sendGet(captor.capture());
        assertEquals(testLocation, captor.getValue());
    }

    /**
     * Tests whether WSDLs behind metadata references are retrieved successfully and reference parameters
     * are being attached correctly.
     *
     * @throws Exception on any exception
     */
    @Test
    @SuppressWarnings("unchecked")
    void testLocationEprRequest() throws Exception {
        var testLocation = "http://mahnamahna.dododododo";
        var testIdentifier = "Rubber duckie you're the one, you make bath time lots of fun";

        var referenceMessageData = String.format(
                METADATA_TEMPLATE,
                String.format(
                        METADATA_SECTION_WSDL,
                        String.format(
                                METADATA_REFERENCE_ADDRESS,
                                testLocation,
                                testIdentifier
                        )
                )
        );

        Envelope eprMessage;
        eprMessage = marshalling.unmarshal(new ByteArrayInputStream(referenceMessageData.getBytes(StandardCharsets.UTF_8)));

        SoapMessage embeddedWsdlMessage;
        try (var messageStream = WsdlRetrieverTest.class.getResourceAsStream("WsdlRetrieverTest/MetadataWsdlEmbedded.xml")) {
            embeddedWsdlMessage = soapUtil.createMessage(marshalling.unmarshal(messageStream));
        }

        when(mockTransferGetClient.sendTransferGet(any(), any(), any()))
                .thenReturn(Futures.immediateFuture(embeddedWsdlMessage));

        var metadata = (Metadata) eprMessage.getBody().getAny().get(0);
        var wsdls = testClass.retrieveWsdlFromMetadata(metadata);

        assertEquals(1, wsdls.size());

        var wsdl = wsdls.get(0);
        assertNotNull(wsdl);
        assertFalse(wsdl.isBlank());

        extractWsdl(wsdl);

        // ensure reference parameters are included
        var captor = ArgumentCaptor.forClass(ReferenceParametersType.class);
        verify(mockTransferGetClient).sendTransferGet(any(), any(), captor.capture());

        var identifier = captor.getValue().getAny().get(0);
        var castIdentifier = (JAXBElement<String>) identifier;
        assertEquals(testIdentifier, castIdentifier.getValue());
    }

    /**
     * Tests whether WSDLs behind metadata references are retrieved successfully.
     *
     * @throws Exception on any exception
     */
    @Test
    void testLocationEprRequestNoRefParm() throws Exception {
        var testLocation = "http://mahnamahna.dododododo";

        var referenceMessageData = String.format(
                METADATA_TEMPLATE,
                String.format(
                        METADATA_SECTION_WSDL,
                        String.format(
                                METADATA_REFERENCE_ADDRESS_NO_REF_PARM,
                                testLocation
                        )
                )
        );

        Envelope eprMessage;
        eprMessage = marshalling.unmarshal(new ByteArrayInputStream(referenceMessageData.getBytes(StandardCharsets.UTF_8)));

        SoapMessage embeddedWsdlMessage;
        try (var messageStream = WsdlRetrieverTest.class.getResourceAsStream("WsdlRetrieverTest/MetadataWsdlEmbedded.xml")) {
            embeddedWsdlMessage = soapUtil.createMessage(marshalling.unmarshal(messageStream));
        }

        when(mockTransferGetClient.sendTransferGet(any(), any()))
                .thenReturn(Futures.immediateFuture(embeddedWsdlMessage));

        var metadata = (Metadata) eprMessage.getBody().getAny().get(0);
        var wsdls = testClass.retrieveWsdlFromMetadata(metadata);

        assertEquals(1, wsdls.size());

        var wsdl = wsdls.get(0);
        assertNotNull(wsdl);
        assertFalse(wsdl.isBlank());

        extractWsdl(wsdl);
    }

    @Test
    void testConvertResponseToStringHttpHeader() {
        var testString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><dang>å</dang>";
        {
            var charset = StandardCharsets.UTF_16LE;
            ListMultimap<String, String> headers = ArrayListMultimap.create();
            headers.put(HttpHeaders.CONTENT_TYPE.toLowerCase(), "text/xml; charset=" + charset.displayName());
            var response = new HttpResponse(200, testString.getBytes(charset), headers);

            var result = testClass.convertResponseToString(response);
            assertEquals(testString, result);
        }
        {
            var charset = StandardCharsets.UTF_8;
            ListMultimap<String, String> headers = ArrayListMultimap.create();
            headers.put(HttpHeaders.CONTENT_TYPE.toLowerCase(), "text/xml; charset=" + charset.displayName());
            var response = new HttpResponse(200, testString.getBytes(charset), headers);

            var result = testClass.convertResponseToString(response);
            assertEquals(testString, result);
        }
        {
            var charset = StandardCharsets.ISO_8859_1;
            ListMultimap<String, String> headers = ArrayListMultimap.create();
            headers.put(HttpHeaders.CONTENT_TYPE.toLowerCase(), "text/xml");
            var response = new HttpResponse(200, testString.getBytes(charset), headers);

            var result = testClass.convertResponseToString(response);
            assertEquals(testString, result);
        }
    }


    @Test
    void testConvertResponseToStringProlog() {
        {
            var testString = "<?xml version=\"1.0\" encoding=\"UTF-16LE\"?><dang>å</dang>";
            var charset = StandardCharsets.UTF_16LE;
            ListMultimap<String, String> headers = ArrayListMultimap.create();
            headers.put(HttpHeaders.CONTENT_TYPE.toLowerCase(), "application/xml");
            var response = new HttpResponse(200, testString.getBytes(charset), headers);

            var result = testClass.convertResponseToString(response);
            assertEquals(testString, result);
        }
        {
            var testString = "<?xml version=\"1.0\" encoding=\"UTF-16BE\"?><dang>å</dang>";
            var charset = StandardCharsets.UTF_16BE;
            ListMultimap<String, String> headers = ArrayListMultimap.create();
            headers.put(HttpHeaders.CONTENT_TYPE.toLowerCase(), "application/xml");
            var response = new HttpResponse(200, testString.getBytes(charset), headers);

            var result = testClass.convertResponseToString(response);
            assertEquals(testString, result);
        }
        {
            var testString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><dang>å</dang>";
            var charset = StandardCharsets.UTF_8;
            ListMultimap<String, String> headers = ArrayListMultimap.create();
            headers.put(HttpHeaders.CONTENT_TYPE.toLowerCase(), "application/xml");
            var response = new HttpResponse(200, testString.getBytes(charset), headers);

            var result = testClass.convertResponseToString(response);
            assertEquals(testString, result);
        }
        {
            var testString = "<dang>☂</dang>"; // use character outside of latin-1
            // default should be utf-8 when using no prolog and content type without specified encoding
            var contentType = ContentType.ContentTypes.APPLICATION_XML;
            assertNull(contentType.getDefaultEncoding());
            var charset = StandardCharsets.UTF_8;
            ListMultimap<String, String> headers = ArrayListMultimap.create();
            headers.put(HttpHeaders.CONTENT_TYPE.toLowerCase(), contentType.getContentType());
            var response = new HttpResponse(200, testString.getBytes(charset), headers);

            var result = testClass.convertResponseToString(response);
            assertEquals(testString, result);
        }
    }


    /*
     Simple check to ensure wsdl is well formed and contains all necessary namespaces.
     */
    private void extractWsdl(String wsdlString) throws Exception {
        wsdlMarshalling.unmarshal(new ByteArrayInputStream(wsdlString.getBytes(StandardCharsets.UTF_8)));
    }
}
