package org.somda.sdc.dpws.device.helper;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.io.CharStreams;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.http.ContentType;
import org.somda.sdc.dpws.http.HttpException;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;
import org.somda.sdc.dpws.soap.MarshallingService;
import org.somda.sdc.dpws.soap.RequestResponseServer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.somda.sdc.dpws.device.helper.RequestResponseServerHttpHandler.NO_CONTENT_TYPE_MESSAGE;

class RequestResponseServerHttpHandlerTest extends DpwsTest {

    private static final String DUMMY_TRANSACTION_ID = "0";
    private static final String DUMMY_REQUEST_URI = "someRequestUri";
    private Injector injector;
    private MarshallingService mockMarshalling;

    @BeforeEach
    public void setUp() throws Exception {
        mockMarshalling = mock(MarshallingService.class, Mockito.RETURNS_DEEP_STUBS);
        var mockServer = mock(RequestResponseServer.class);
        var overrideModule = new AbstractModule() {
            @Override
            protected void configure() {
                super.configure();
                bind(MarshallingService.class).toInstance(mockMarshalling);
                bind(RequestResponseServer.class).toInstance(mockServer);
            }
        };
        overrideBindings(overrideModule);

        super.setUp();
        injector = getInjector();

    }

    @Test
    @DisplayName("RequestResponseServerHttpHandler shall fail without a content-type")
    void testContentTypeMissing() {
        final String expectedString = "The quick brown fox jumps over the lazy dog ý ☂";

        ListMultimap<String, String> headers = ArrayListMultimap.create();
        headers.put(HttpHeaders.DATE, "tomorrow");
        var reqResServer = injector.getInstance(RequestResponseServerHttpHandler.class);
        var mockContext = mock(CommunicationContext.class);
        var applicationInfo = new HttpApplicationInfo(headers, DUMMY_TRANSACTION_ID, DUMMY_REQUEST_URI);
        when(mockContext.getApplicationInfo()).thenReturn(applicationInfo);

        // encoding doesn't matter here, we should fail before that
        var requestStream = new ByteArrayInputStream(expectedString.getBytes(StandardCharsets.UTF_8));
        var responseStream = new ByteArrayOutputStream();

        var error = assertThrows(HttpException.class, () -> reqResServer.handle(requestStream, responseStream, mockContext));
        assertTrue(error.getMessage().contains(NO_CONTENT_TYPE_MESSAGE)); // missing content-type message
    }

    @Test
    @DisplayName("RequestResponseServerHttpHandler shall properly handle implicit latin1 encoding using text/xml")
    void testContentTypeEncodingCorrectTextXmlImplicit() throws Exception {
        final String expectedString = "The quick brown fox jumps over the lazy dog ý";
        final var contentType = ContentType.ContentTypes.TEXT_XML;
        assertNotNull(contentType.getDefaultEncoding());

        ListMultimap<String, String> headers = ArrayListMultimap.create();
        headers.put(HttpHeaders.DATE.toLowerCase(), "tomorrow");
        headers.put(HttpHeaders.CONTENT_TYPE.toLowerCase(), contentType.getContentType());
        var reqResServer = injector.getInstance(RequestResponseServerHttpHandler.class);
        var mockContext = mock(CommunicationContext.class);
        var applicationInfo = new HttpApplicationInfo(headers, DUMMY_TRANSACTION_ID, DUMMY_REQUEST_URI);
        when(mockContext.getApplicationInfo()).thenReturn(applicationInfo);

        var requestStream = new ByteArrayInputStream(expectedString.getBytes(contentType.getDefaultEncoding())); // doesn't matter
        var responseStream = new ByteArrayOutputStream();

        reqResServer.handle(requestStream, responseStream, mockContext);
        ArgumentCaptor<Reader> readerCaptor = ArgumentCaptor.forClass(Reader.class);
        verify(mockMarshalling, times(1)).unmarshal(readerCaptor.capture());

        var reader = readerCaptor.getValue();
        var content = CharStreams.toString(reader);
        assertEquals(expectedString, content, "Reader content does not match input");
    }

    @Test
    @DisplayName("RequestResponseServerHttpHandler shall properly handle explicit utf-16 encoding using text/xml")
    void testContentTypeEncodingCorrectTextXmlExplicitUtf8() throws Exception {
        final String expectedString = "The quick brown fox jumps over the lazy dog ý ☂";
        final var contentType = ContentType.ContentTypes.TEXT_XML;
        var encoding = StandardCharsets.UTF_16LE;

        ListMultimap<String, String> headers = ArrayListMultimap.create();
        headers.put(HttpHeaders.DATE.toLowerCase(), "tomorrow");
        headers.put(HttpHeaders.CONTENT_TYPE.toLowerCase(), contentType.getContentType() + "; charset=" + encoding.displayName());
        var reqResServer = injector.getInstance(RequestResponseServerHttpHandler.class);
        var mockContext = mock(CommunicationContext.class);
        var applicationInfo = new HttpApplicationInfo(headers, DUMMY_TRANSACTION_ID, DUMMY_REQUEST_URI);
        when(mockContext.getApplicationInfo()).thenReturn(applicationInfo);

        var requestStream = new ByteArrayInputStream(expectedString.getBytes(encoding)); // doesn't matter
        var responseStream = new ByteArrayOutputStream();

        reqResServer.handle(requestStream, responseStream, mockContext);
        ArgumentCaptor<Reader> readerCaptor = ArgumentCaptor.forClass(Reader.class);
        verify(mockMarshalling, times(1)).unmarshal(readerCaptor.capture());

        var reader = readerCaptor.getValue();
        var content = CharStreams.toString(reader);
        assertEquals(expectedString, content, "Reader content does not match input");
    }

    @Test
    @DisplayName("RequestResponseServerHttpHandler shall let JAXB handle encoding for application/xml")
    void testMarshallWithoutReader() throws Exception {
        final String expectedString = "The quick brown fox jumps over the lazy dog ý ☂";
        final var contentType = ContentType.ContentTypes.APPLICATION_XML;
        var encoding = StandardCharsets.UTF_8;

        ListMultimap<String, String> headers = ArrayListMultimap.create();
        headers.put(HttpHeaders.DATE.toLowerCase(), "tomorrow");
        headers.put(HttpHeaders.CONTENT_TYPE.toLowerCase(), contentType.getContentType());
        var reqResServer = injector.getInstance(RequestResponseServerHttpHandler.class);
        var mockContext = mock(CommunicationContext.class);
        var applicationInfo = new HttpApplicationInfo(headers, DUMMY_TRANSACTION_ID, DUMMY_REQUEST_URI);
        when(mockContext.getApplicationInfo()).thenReturn(applicationInfo);

        var requestStream = new ByteArrayInputStream(expectedString.getBytes(encoding)); // doesn't matter
        var responseStream = new ByteArrayOutputStream();

        reqResServer.handle(requestStream, responseStream, mockContext);
        ArgumentCaptor<InputStream> inputStreamCaptor = ArgumentCaptor.forClass(InputStream.class);
        // marshalling must receive the raw input stream
        verify(mockMarshalling, times(1)).unmarshal(inputStreamCaptor.capture());
        verify(mockMarshalling, times(0)).unmarshal(isA(Reader.class));
        var inputStream = inputStreamCaptor.getValue();
        var content = new String(inputStream.readAllBytes(), encoding);
        assertEquals(expectedString, content, "InputStream passed for marshalling was modified");
    }

}
