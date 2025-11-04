package org.somda.sdc.dpws.http.apache;

import com.google.common.io.CharStreams;
import com.google.inject.Injector;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.somda.sdc.dpws.CommunicationLogContext;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.TransportBindingException;
import org.somda.sdc.dpws.http.ContentType;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClientTransportBindingTest extends DpwsTest {

    private ClientTransportBindingFactory fac;
    private Injector injector;
    private HttpClient mockHttpClient;
    private String clientUri;
    private SoapMarshalling mockMarshalling;
    private SoapUtil mockSoapUtil;
    private ClientTransportBinding binding;
    private CommunicationLogContext communicationLogContext;

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        injector = getInjector();
        fac = injector.getInstance(ClientTransportBindingFactory.class);
        mockHttpClient = mock(HttpClient.class);
        clientUri = "http://somewhere.over.the.rainb.ow";
        mockMarshalling = mock(SoapMarshalling.class);
        mockSoapUtil = mock(SoapUtil.class, Mockito.RETURNS_DEEP_STUBS);
        communicationLogContext = new CommunicationLogContext("Whatever");

        binding = fac.create(mockHttpClient, clientUri, mockMarshalling, mockSoapUtil, communicationLogContext);
    }

    @Test
    @DisplayName("RequestResponseServerHttpHandler shall fail without a content-type")
    void testContentTypeMissing() throws JAXBException, IOException {
        var mockSoapMessage = mock(SoapMessage.class, Mockito.RETURNS_DEEP_STUBS);
        var requestContent = "heya";
        var requestEncoding = StandardCharsets.UTF_8;

        doAnswer(invocation -> {
            var os = invocation.getArgument(1, OutputStream.class);
            os.write(requestContent.getBytes(requestEncoding));
            return null;
        }).when(mockMarshalling).marshal(any(), any());

        var mockEntity = mock(HttpEntity.class);
        when(mockEntity.getContentType()).thenReturn(null);
        var mockResponse = mock(HttpResponse.class, Mockito.RETURNS_DEEP_STUBS);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockHttpClient.execute(any(), (HttpContext) any())).thenReturn(mockResponse);

        var error = assertThrows(TransportBindingException.class, () -> binding.onRequestResponse(mockSoapMessage));
        assertTrue(error.getMessage().contains("Could not parse content type from element"));
    }


    @Test
    @DisplayName("RequestResponseServerHttpHandler shall handle implicit iso-8859-1 in text/xml")
    void testContentTypeTextXmlImplicitIso() throws JAXBException, IOException, SoapFaultException {
        var mockSoapMessage = mock(SoapMessage.class, Mockito.RETURNS_DEEP_STUBS);
        var requestContent = "heya";
        var requestEncoding = StandardCharsets.UTF_8;

        var responseContent = "The quick brown fox jumps over the lazy dog ý";
        var responseType = ContentType.ContentTypes.TEXT_XML;
        var responseEncoding = responseType.getDefaultEncoding();
        assertNotNull(responseEncoding);

        doAnswer(invocation -> {
            var os = invocation.getArgument(1, OutputStream.class);
            os.write(requestContent.getBytes(requestEncoding));
            return null;
        }).when(mockMarshalling).marshal(any(), any());

        var header = new BasicHeader(HttpHeaders.CONTENT_TYPE, responseType.getContentType());
        var mockEntity = mock(HttpEntity.class);
        when(mockEntity.getContentType()).thenReturn(header);
        when(mockEntity.getContent()).thenReturn(new ByteArrayInputStream(responseContent.getBytes(responseEncoding)));
        var mockResponse = mock(HttpResponse.class, Mockito.RETURNS_DEEP_STUBS);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockHttpClient.execute(any(), (HttpContext) any())).thenReturn(mockResponse);

        var response = binding.onRequestResponse(mockSoapMessage);

        var readerCaptor = ArgumentCaptor.forClass(Reader.class);
        verify(mockMarshalling, times(1)).unmarshal(readerCaptor.capture());

        var reader = readerCaptor.getValue();
        var actualResponseContent = CharStreams.toString(reader);

        assertEquals(responseContent, actualResponseContent);
    }

    @Test
    @DisplayName("RequestResponseServerHttpHandler shall handle explicit utf-16be in text/xml")
    void testContentTypeTextXmlExplicitUtf16() throws JAXBException, IOException, SoapFaultException {
        var mockSoapMessage = mock(SoapMessage.class, Mockito.RETURNS_DEEP_STUBS);
        var requestContent = "heya";
        var requestEncoding = StandardCharsets.UTF_8;

        var responseContent = "The quick brown fox jumps over the lazy dog ý ☂";
        var responseType = ContentType.ContentTypes.TEXT_XML;
        var responseEncoding = StandardCharsets.UTF_16BE;
        assertNotNull(responseEncoding);

        doAnswer(invocation -> {
            var os = invocation.getArgument(1, OutputStream.class);
            os.write(requestContent.getBytes(requestEncoding));
            return null;
        }).when(mockMarshalling).marshal(any(), any());

        var header = new BasicHeader(HttpHeaders.CONTENT_TYPE, responseType.getContentType() + "; charset=" + responseEncoding.displayName());
        var mockEntity = mock(HttpEntity.class);
        when(mockEntity.getContentType()).thenReturn(header);
        when(mockEntity.getContent()).thenReturn(new ByteArrayInputStream(responseContent.getBytes(responseEncoding)));
        var mockResponse = mock(HttpResponse.class, Mockito.RETURNS_DEEP_STUBS);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockHttpClient.execute(any(), (HttpContext) any())).thenReturn(mockResponse);

        var response = binding.onRequestResponse(mockSoapMessage);

        var readerCaptor = ArgumentCaptor.forClass(Reader.class);
        verify(mockMarshalling, times(1)).unmarshal(readerCaptor.capture());

        var reader = readerCaptor.getValue();
        var actualResponseContent = CharStreams.toString(reader);

        assertEquals(responseContent, actualResponseContent);
    }


    @Test
    @DisplayName("RequestResponseServerHttpHandler shall let jaxb handle application/xml without charset")
    void testContentTypeApplicationXml() throws JAXBException, IOException, SoapFaultException {
        var mockSoapMessage = mock(SoapMessage.class, Mockito.RETURNS_DEEP_STUBS);
        var requestContent = "heya";
        var requestEncoding = StandardCharsets.UTF_8;

        var responseContent = "The quick brown fox jumps over the lazy dog ý ☂";
        var responseType = ContentType.ContentTypes.APPLICATION_XML;
        var responseEncoding = StandardCharsets.UTF_8;
        assertNotNull(responseEncoding);

        doAnswer(invocation -> {
            var os = invocation.getArgument(1, OutputStream.class);
            os.write(requestContent.getBytes(requestEncoding));
            return null;
        }).when(mockMarshalling).marshal(any(), any());

        var header = new BasicHeader(HttpHeaders.CONTENT_TYPE, responseType.getContentType());
        var mockEntity = mock(HttpEntity.class);
        when(mockEntity.getContentType()).thenReturn(header);
        when(mockEntity.getContent()).thenReturn(new ByteArrayInputStream(responseContent.getBytes(responseEncoding)));
        var mockResponse = mock(HttpResponse.class, Mockito.RETURNS_DEEP_STUBS);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockHttpClient.execute(any(), (HttpContext) any())).thenReturn(mockResponse);

        var response = binding.onRequestResponse(mockSoapMessage);

        var readerCaptor = ArgumentCaptor.forClass(Reader.class);
        var streamCaptor = ArgumentCaptor.forClass(InputStream.class);
        // don't use reader, let jaxb figure it out
        verify(mockMarshalling, times(0)).unmarshal(readerCaptor.capture());
        verify(mockMarshalling, times(1)).unmarshal(streamCaptor.capture());

        var inputStream = streamCaptor.getValue();
        var actualResponseContent = new String(inputStream.readAllBytes(), responseEncoding);

        assertEquals(responseContent, actualResponseContent);
    }
}
