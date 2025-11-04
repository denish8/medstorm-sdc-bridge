package it.org.somda.sdc.dpws;

import com.google.common.collect.ListMultimap;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.somda.sdc.dpws.*;
import org.somda.sdc.dpws.factory.CommunicationLogFactory;
import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.helper.JaxbMarshalling;
import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.http.apache.ClientTransportBinding;
import org.somda.sdc.dpws.http.jetty.JettyHttpServerHandler;
import org.somda.sdc.dpws.http.jetty.JettyHttpServerRegistry;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.factory.EnvelopeFactory;
import org.somda.sdc.dpws.soap.factory.SoapMessageFactory;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommunicationLogIT extends DpwsTest {

    private TransportBindingFactory transportBindingFactory;
    private SoapMessageFactory soapMessageFactory;
    private EnvelopeFactory envelopeFactory;
    private SoapMarshalling marshalling;
    private TestCommLogSink logSink;
    private JettyHttpServerRegistry httpServerRegistry;

    void setUp(boolean commlogInHandler) throws Exception {
        var dpwsOverride = new DefaultDpwsConfigModule() {
            @Override
            public void customConfigure() {
                // ensure commlog works with compression enabled and doesn't store compressed messages
                bind(DpwsConfig.HTTP_GZIP_COMPRESSION, Boolean.class, true);
                bind(DpwsConfig.HTTP_SERVER_COMMLOG_IN_HANDLER, Boolean.class, commlogInHandler);
            }
        };
        var override = new AbstractModule() {
            @Override
            protected void configure() {
                bind(CommunicationLogSink.class).to(TestCommLogSink.class).asEagerSingleton();
                install(new FactoryModuleBuilder()
                        .implement(CommunicationLog.class, CommunicationLogImpl.class)
                        .build(CommunicationLogFactory.class));
            }
        };
        this.overrideBindings(List.of(dpwsOverride, override));
        super.setUp();


        httpServerRegistry = getInjector().getInstance(JettyHttpServerRegistry.class);
        transportBindingFactory = getInjector().getInstance(TransportBindingFactory.class);
        soapMessageFactory = getInjector().getInstance(SoapMessageFactory.class);
        envelopeFactory = getInjector().getInstance(EnvelopeFactory.class);
        getInjector().getInstance(JaxbMarshalling.class).startAsync().awaitRunning();
        marshalling = getInjector().getInstance(SoapMarshalling.class);
        logSink = (TestCommLogSink) getInjector().getInstance(CommunicationLogSink.class);
        marshalling.startAsync().awaitRunning();
    }

    @AfterEach
    void tearDown() {
        marshalling.stopAsync().awaitTerminated();
        logSink.clear();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testClientCommlog(boolean commLogInHandler) throws Exception {
        setUp(commLogInHandler);
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
        var expectedResponseStream = new CloseableByteArrayOutputStream();
        marshalling.marshal(responseEnvelope.getEnvelopeWithMappedHeaders(), expectedResponseStream);

        var responseBytes = expectedResponseStream.toByteArray();

        // spawn the http server
        var handler = new HttpServerUtil.GzipResponseHandler(responseBytes);
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

        final var commLogContext = new CommunicationLogContext("txetnoCgoLnoitacinummoC" + UUID.randomUUID());

        // make requests to our server
        TransportBinding httpBinding1 = transportBindingFactory.createHttpBinding(baseUri.toString(), commLogContext);

        for (int i = 0; i < 100; i++) {

            var requestMessage = createASoapMessage();

            var actualRequestStream = new CloseableByteArrayOutputStream();
            marshalling.marshal(requestMessage.getEnvelopeWithMappedHeaders(), actualRequestStream);

            SoapMessage response = httpBinding1.onRequestResponse(requestMessage);

            var actualResponseStream = new CloseableByteArrayOutputStream();
            marshalling.marshal(response.getEnvelopeWithMappedHeaders(), actualResponseStream);

            // response bytes should exactly match our expected bytes
            assertArrayEquals(expectedResponseStream.toByteArray(), actualResponseStream.toByteArray());

            // requests must contain our message
            var req = logSink.getOutbound().get(0);
            var resp = logSink.getInbound().get(0);

            assertArrayEquals(actualRequestStream.toByteArray(), req.toByteArray());
            assertArrayEquals(expectedResponseStream.toByteArray(), resp.toByteArray());

            // ensure streams were closed
            assertTrue(req.isClosed());
            assertTrue(resp.isClosed());

            // ensure request headers are logged
            assertTrue(
                    logSink.getOutboundHeaders().get(0)
                            .get(ClientTransportBinding.USER_AGENT_KEY.toLowerCase())
                            .contains(ClientTransportBinding.USER_AGENT_VALUE)
            );
            // ensure response headers are logged
            assertTrue(
                    logSink.getInboundHeaders().get(0)
                            .get(HttpServerUtil.GzipResponseHandler.TEST_HEADER_KEY.toLowerCase())
                            .contains(HttpServerUtil.GzipResponseHandler.TEST_HEADER_VALUE)
            );

            // all headers must've been converted to lower case, these must be false
            assertFalse(
                    logSink.getOutboundHeaders().get(0)
                            .get(ClientTransportBinding.USER_AGENT_KEY)
                            .contains(ClientTransportBinding.USER_AGENT_VALUE)
            );
            assertFalse(
                    logSink.getInboundHeaders().get(0)
                            .get(HttpServerUtil.GzipResponseHandler.TEST_HEADER_KEY)
                            .contains(HttpServerUtil.GzipResponseHandler.TEST_HEADER_VALUE)
            );

            // communication contexts must be in request and response
            assertEquals(commLogContext, logSink.getOutboundCommunicationContexts().get(0).getCommunicationLogContext());
            assertEquals(commLogContext, logSink.getInboundCommunicationContexts().get(0).getCommunicationLogContext());

            assertEquals(CommunicationLog.MessageType.RESPONSE, logSink.getInboundMessageType());
            assertEquals(CommunicationLog.MessageType.REQUEST, logSink.getOutboundMessageType());

            compareTransactionIds(logSink.getInboundTransactionIds(), logSink.getOutboundTransactionIds());

            logSink.clear();
        }
    }

    static class SlowInputStream extends InputStream {

        private final byte[] payload;
        private final long delayMs;

        private int nextIndex;

        SlowInputStream(byte[] payload, int sleep) {
            this.payload = payload;
            this.delayMs = sleep;

            this.nextIndex = 0;
        }

        @Override
        public int read() throws IOException {
            // only sleep sometimes
            if (nextIndex % 10000 == 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    throw new IOException(e);
                }
            }
            if (payload.length <= nextIndex) {
                return -1;
            }
            return payload[nextIndex++];
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testServerCommlogStreamedRequest(boolean commLogInHandler) throws Exception {
        setUp(commLogInHandler);
        var baseUri = "http://127.0.0.1:0";
        var contextPath = "/ctxt/path1";

        final String baseRequest = "The quick brown fox jumps over the lazy dog";
        final String expectedResponse = "Franz jagt im komplett verwahrlosten Taxi quer durch Bayern";
        AtomicReference<String> resultString = new AtomicReference<>();

        httpServerRegistry.startAsync().awaitRunning();
        var srvUri1 = httpServerRegistry.registerContext(
            baseUri, true, contextPath, null, null, new HttpHandler() {
                @Override
                public void handle(InputStream inStream, OutputStream outStream, CommunicationContext communicationContext) {
                    try {
                        byte[] bytes = inStream.readAllBytes();
                        resultString.set(new String(bytes));

                        // write response
                        outStream.write(expectedResponse.getBytes());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });

        // as we explicitly want to set http headers and avoid the commlog, we build our own client
        HttpClient client = HttpClients.custom().setMaxConnPerRoute(1).build();

        // create post request and set custom header
        HttpPost post = new HttpPost(srvUri1);
        String customHeaderKey = "thecustomheader";
        String customHeaderValue = "theCUSTOMvalue";
        post.setHeader(customHeaderKey, customHeaderValue);

        // construct our request payload to be sure it is longer than one chunk
        var expectedRequest = baseRequest.repeat(100000);

        // attach payload
        var requestEntity = new InputStreamEntity(new SlowInputStream(expectedRequest.getBytes(), 50));
        // force chunking because of stream
        requestEntity.setChunked(true);
        post.setEntity(requestEntity);

        for (int i = 0; i < 1; i++) {
            HttpResponse response = client.execute(post);
            var responseBytes = response.getEntity().getContent().readAllBytes();

            // slurp up any leftover data
            EntityUtils.consume(response.getEntity());

            assertEquals(expectedRequest, resultString.get());
            assertArrayEquals(expectedResponse.getBytes(), responseBytes);

            var req = logSink.getInbound().get(0);
            var resp = logSink.getOutbound().get(0);

            assertArrayEquals(expectedRequest.getBytes(), req.toByteArray());
            assertArrayEquals(expectedResponse.getBytes(), resp.toByteArray());

            // ensure streams were closed by interceptors
            assertTrue(req.isClosed());
            assertTrue(resp.isClosed());

            // ensure request headers are logged
            assertTrue(
                logSink.getInboundHeaders().get(0).get(customHeaderKey)
                    .contains(customHeaderValue)
            );
            // ensure response headers are logged
            assertTrue(
                logSink.getOutboundHeaders().get(0)
                    .get(JettyHttpServerHandler.SERVER_HEADER_KEY.toLowerCase())
                    .contains(JettyHttpServerHandler.SERVER_HEADER_VALUE)
            );

            // all headers must've been converted to lower case, these must be false
            assertFalse(
                logSink.getInboundHeaders().get(0).get(customHeaderKey.toUpperCase())
                    .contains(customHeaderValue)
            );
            assertFalse(
                logSink.getOutboundHeaders().get(0)
                    .get(JettyHttpServerHandler.SERVER_HEADER_KEY)
                    .contains(JettyHttpServerHandler.SERVER_HEADER_VALUE)
            );

            assertEquals(CommunicationLog.MessageType.REQUEST, logSink.getInboundMessageType());
            assertEquals(CommunicationLog.MessageType.RESPONSE, logSink.getOutboundMessageType());

            compareTransactionIds(logSink.getInboundTransactionIds(), logSink.getOutboundTransactionIds());

            logSink.clear();
        }

    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testServerCommlog(boolean commLogInHandler) throws Exception {
        setUp(commLogInHandler);
        var baseUri = "http://127.0.0.1:0";
        var contextPath = "/ctxt/path1";

        final String expectedRequest = "The quick brown fox jumps over the lazy dog";
        final String expectedResponse = "Franz jagt im komplett verwahrlosten Taxi quer durch Bayern";
        AtomicReference<String> resultString = new AtomicReference<>();
        final var commLogContextPresent = new AtomicReference<CommunicationLogContext>();

        final var commLogContext = new CommunicationLogContext("txetnoCgoLnoitacinummoC" + UUID.randomUUID());

        httpServerRegistry.startAsync().awaitRunning();
        var srvUri1 = httpServerRegistry.registerContext(
                baseUri, true, contextPath, null, commLogContext, new HttpHandler() {
                    @Override
                    public void handle(InputStream inStream, OutputStream outStream, CommunicationContext communicationContext) {
                        try {
                            byte[] bytes = inStream.readAllBytes();
                            resultString.set(new String(bytes));

                            if (communicationContext.getCommunicationLogContext() != null) {
                                commLogContextPresent.set(communicationContext.getCommunicationLogContext());
                            }

                            // write response
                            outStream.write(expectedResponse.getBytes());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

        // as we explicitly want to set http headers and avoid the commlog, we build our own client
        HttpClient client = HttpClients.custom().setMaxConnPerRoute(1).build();

        // create post request and set custom header
        HttpPost post = new HttpPost(srvUri1);
        String customHeaderKey = "thecustomheader";
        String customHeaderValue = "theCUSTOMvalue";
        post.setHeader(customHeaderKey, customHeaderValue);

        // attach payload
        var requestEntity = new ByteArrayEntity(expectedRequest.getBytes());
        // force chunking to test for completeness
        requestEntity.setChunked(true);
        post.setEntity(requestEntity);

        for (int i = 0; i < 10; i++) {
            HttpResponse response = client.execute(post);
            var responseBytes = response.getEntity().getContent().readAllBytes();

            // TODO: Because of the Commlog in the server closing after the request is done, we need a little sleep here
            Thread.sleep(100);

            // slurp up any leftover data
            EntityUtils.consume(response.getEntity());

            assertEquals(expectedRequest, resultString.get());
            assertArrayEquals(expectedResponse.getBytes(), responseBytes);

            assertEquals(commLogContext, commLogContextPresent.get());

            var req = logSink.getInbound().get(0);
            var resp = logSink.getOutbound().get(0);

            assertArrayEquals(expectedRequest.getBytes(), req.toByteArray());
            assertArrayEquals(expectedResponse.getBytes(), resp.toByteArray());

            // ensure streams were closed by interceptors
            assertTrue(req.isClosed());
            assertTrue(resp.isClosed());

            // ensure request headers are logged
            assertTrue(
                    logSink.getInboundHeaders().get(0).get(customHeaderKey)
                            .contains(customHeaderValue)
            );
            // ensure response headers are logged
            assertTrue(
                    logSink.getOutboundHeaders().get(0)
                            .get(JettyHttpServerHandler.SERVER_HEADER_KEY.toLowerCase())
                            .contains(JettyHttpServerHandler.SERVER_HEADER_VALUE)
            );

            // all headers must've been converted to lower case, these must be false
            assertFalse(
                    logSink.getInboundHeaders().get(0).get(customHeaderKey.toUpperCase())
                            .contains(customHeaderValue)
            );
            assertFalse(
                    logSink.getOutboundHeaders().get(0)
                            .get(JettyHttpServerHandler.SERVER_HEADER_KEY)
                            .contains(JettyHttpServerHandler.SERVER_HEADER_VALUE)
            );

            assertEquals(CommunicationLog.MessageType.REQUEST, logSink.getInboundMessageType());
            assertEquals(CommunicationLog.MessageType.RESPONSE, logSink.getOutboundMessageType());

            // if we're using commlog in handler, this must be set
            if (commLogInHandler) {
                assertNotNull(logSink.getInboundCommunicationContexts().get(0).getCommunicationLogContext());
                assertNotNull(logSink.getOutboundCommunicationContexts().get(0).getCommunicationLogContext());
            } else {
                assertNull(logSink.getInboundCommunicationContexts().get(0).getCommunicationLogContext());
                assertNull(logSink.getOutboundCommunicationContexts().get(0).getCommunicationLogContext());
            }

            compareTransactionIds(logSink.getInboundTransactionIds(), logSink.getOutboundTransactionIds());

            logSink.clear();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testServerCommlogDuplicateKeys(boolean commLogInHandler) throws Exception {
        setUp(commLogInHandler);
        var baseUri = "http://127.0.0.1:0";
        var contextPath = "/ctxt/path1";

        final String expectedRequest = "The quick brown fox jumps over the lazy dog";
        final String expectedResponse = "Franz jagt im komplett verwahrlosten Taxi quer durch Bayern";
        AtomicReference<String> resultString = new AtomicReference<>();

        httpServerRegistry.startAsync().awaitRunning();
        var srvUri1 = httpServerRegistry.registerContext(
                baseUri, true, contextPath, null, null, new HttpHandler() {
                    @Override
                    public void handle(InputStream inStream, OutputStream outStream, CommunicationContext communicationContext) {
                        try {
                            byte[] bytes = inStream.readAllBytes();
                            resultString.set(new String(bytes));

                            // write response
                            outStream.write(expectedResponse.getBytes());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

        // as we explicitly want to set http headers and avoid the commlog, we build our own client
        HttpClient client = HttpClients.custom().setMaxConnPerRoute(1).build();

        // create post request and set custom header
        HttpPost post = new HttpPost(srvUri1);
        String customHeaderKey = "thecustomheader";
        String customHeaderValue = "theCUSTOMvalue";
        String customHeaderValue2 = "theCUSTOMvalue2";
        post.setHeader(customHeaderKey, customHeaderValue);
        // ensure an upper case value will be mapped to the same key
        post.addHeader(customHeaderKey.toUpperCase(), customHeaderValue2);

        // attach payload
        var requestEntity = new ByteArrayEntity(expectedRequest.getBytes());
        post.setEntity(requestEntity);

        for (int i = 0; i < 100; i++) {
            HttpResponse response = client.execute(post);
            var responseBytes = response.getEntity().getContent().readAllBytes();

            // slurp up any leftover data
            EntityUtils.consume(response.getEntity());

            assertEquals(expectedRequest, resultString.get());
            assertArrayEquals(expectedResponse.getBytes(), responseBytes);

            var req = logSink.getInbound().get(0);
            var resp = logSink.getOutbound().get(0);

            assertArrayEquals(expectedRequest.getBytes(), req.toByteArray());
            assertArrayEquals(expectedResponse.getBytes(), resp.toByteArray());

            // ensure request headers are logged
            assertEquals(
                    2,
                    logSink.getInboundHeaders().get(0).get(customHeaderKey).size()
            );
            assertTrue(
                    logSink.getInboundHeaders().get(0).get(customHeaderKey)
                            .contains(customHeaderValue)
            );
            assertTrue(
                    logSink.getInboundHeaders().get(0).get(customHeaderKey)
                            .contains(customHeaderValue2)
            );
            logSink.clear();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testDuplicateTransactionIds(boolean commLogInHandler) throws Exception {
        setUp(commLogInHandler);

        final Injector secondInjector = configureInjector(List.of(
                new DefaultDpwsConfigModule() {
                    @Override
                    public void customConfigure() {
                        // ensure commlog works with compression enabled and doesn't store compressed messages
                        bind(DpwsConfig.HTTP_GZIP_COMPRESSION, Boolean.class, true);
                    }
                },
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(CommunicationLogSink.class).toInstance(logSink);
                        install(new FactoryModuleBuilder()
                                .implement(CommunicationLog.class, CommunicationLogImpl.class)
                                .build(CommunicationLogFactory.class));
                    }
                }
        ));

        final TransportBindingFactory secondTransportBindingFactory = secondInjector.getInstance(TransportBindingFactory.class);
        secondInjector.getInstance(JaxbMarshalling.class).startAsync().awaitRunning();
        final TestCommLogSink secondLogSink = (TestCommLogSink) secondInjector.getInstance(CommunicationLogSink.class);
        final SoapMarshalling secondMarshalling = secondInjector.getInstance(SoapMarshalling.class);
        secondMarshalling.startAsync().awaitRunning();


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
        var expectedResponseStream = new CloseableByteArrayOutputStream();
        marshalling.marshal(responseEnvelope.getEnvelopeWithMappedHeaders(), expectedResponseStream);

        var responseBytes = expectedResponseStream.toByteArray();

        // spawn the http server
        var handler = new HttpServerUtil.GzipResponseHandler(responseBytes);
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

        // make requests to our server
        TransportBinding httpBinding1 = transportBindingFactory.createHttpBinding(baseUri.toString(), null);

        TransportBinding httpBinding2 = secondTransportBindingFactory.createHttpBinding(baseUri.toString(), null);
        for (int i = 0; i < 100; i++) {

            testSharedLogSink(marshalling, httpBinding1, logSink, expectedResponseStream);
            testSharedLogSink(secondMarshalling, httpBinding2, secondLogSink, expectedResponseStream);

            assertEquals(CommunicationLog.MessageType.RESPONSE, logSink.getInboundMessageType());
            assertEquals(CommunicationLog.MessageType.REQUEST, logSink.getOutboundMessageType());

            compareTransactionIds(logSink.getInboundTransactionIds(), logSink.getOutboundTransactionIds());

            logSink.clear();
        }
    }

    private void testSharedLogSink(SoapMarshalling soapMarshalling, TransportBinding httpBinding, TestCommLogSink logSink, ByteArrayOutputStream expectedResponseStream) throws Exception {
        var requestMessage2 = createASoapMessage();

        var actualRequestStream2 = new CloseableByteArrayOutputStream();
        soapMarshalling.marshal(requestMessage2.getEnvelopeWithMappedHeaders(), actualRequestStream2);

        SoapMessage response2 = httpBinding.onRequestResponse(requestMessage2);

        var actualResponseStream2 = new CloseableByteArrayOutputStream();
        soapMarshalling.marshal(response2.getEnvelopeWithMappedHeaders(), actualResponseStream2);

        // response bytes should exactly match our expected bytes
        assertArrayEquals(expectedResponseStream.toByteArray(), actualResponseStream2.toByteArray());

        // requests must contain our message
        var req2 = logSink.getOutbound().get(0);
        var resp2 = logSink.getInbound().get(0);

        assertArrayEquals(actualRequestStream2.toByteArray(), req2.toByteArray());
        assertArrayEquals(expectedResponseStream.toByteArray(), resp2.toByteArray());

        // ensure request headers are logged
        assertTrue(
                logSink.getOutboundHeaders().get(0)
                        .get(ClientTransportBinding.USER_AGENT_KEY.toLowerCase())
                        .contains(ClientTransportBinding.USER_AGENT_VALUE)
        );
        // ensure response headers are logged
        assertTrue(
                logSink.getInboundHeaders().get(0)
                        .get(HttpServerUtil.GzipResponseHandler.TEST_HEADER_KEY.toLowerCase())
                        .contains(HttpServerUtil.GzipResponseHandler.TEST_HEADER_VALUE)
        );

        // all headers must've been converted to lower case, these must be false
        assertFalse(
                logSink.getOutboundHeaders().get(0)
                        .get(ClientTransportBinding.USER_AGENT_KEY)
                        .contains(ClientTransportBinding.USER_AGENT_VALUE)
        );
        assertFalse(
                logSink.getInboundHeaders().get(0)
                        .get(HttpServerUtil.GzipResponseHandler.TEST_HEADER_KEY)
                        .contains(HttpServerUtil.GzipResponseHandler.TEST_HEADER_VALUE)
        );
    }

    private void compareTransactionIds(ArrayList<String> inboundTransactionIds, ArrayList<String> outboundTransactionIds) {
        final var inboundSet = new HashSet<>(inboundTransactionIds);
        final var outboundSet = new HashSet<>(outboundTransactionIds);

        //check uniqueness of transaction ids
        assertEquals(inboundTransactionIds.size(), inboundSet.size(), "Duplicate Inbound TransactionIds.");
        assertEquals(outboundTransactionIds.size(), outboundSet.size(), "Duplicate Outbound TransactionIds.");

        //every request has a response
        assertEquals(inboundSet, outboundSet, "Not all responses are associated with a request.");
    }

    static class CloseableByteArrayOutputStream extends ByteArrayOutputStream {
        private boolean closed;

        CloseableByteArrayOutputStream() {
            this.closed = false;
        }

        @Override
        public void close() throws IOException {
            super.close();
            this.closed = true;
        }

        private void ensureNotClosed() {
            if (closed) {
                throw new RuntimeException("Stream closed");
            }
        }

        @Override
        public synchronized void write(final int b) {
            ensureNotClosed();
            super.write(b);
        }

        @Override
        public synchronized void write(final byte[] b, final int off, final int len) {
            ensureNotClosed();
            super.write(b, off, len);
        }

        @Override
        public void write(final byte[] b) throws IOException {
            ensureNotClosed();
            super.write(b);
        }

        public boolean isClosed() {
            return closed;
        }
    }

    static class TestCommLogSink implements CommunicationLogSink {

        private final ArrayList<CloseableByteArrayOutputStream> inbound;
        private final ArrayList<CloseableByteArrayOutputStream> outbound;
        private final ArrayList<ListMultimap<String, String>> inboundHeaders;
        private final ArrayList<ListMultimap<String, String>> outboundHeaders;
        private CommunicationLog.MessageType inboundMessageType;
        private CommunicationLog.MessageType outboundMessageType;
        private final ArrayList<String> inboundTransactionIds;
        private final ArrayList<String> outboundTransactionIds;
        private final ArrayList<CommunicationContext> inboundCommunicationContexts;
        private final ArrayList<CommunicationContext> outboundCommunicationContexts;

        TestCommLogSink() {
            this.inbound = new ArrayList<>();
            this.outbound = new ArrayList<>();
            this.inboundHeaders = new ArrayList<>();
            this.outboundHeaders = new ArrayList<>();
            this.inboundTransactionIds = new ArrayList<>();
            this.outboundTransactionIds = new ArrayList<>();
            this.inboundCommunicationContexts = new ArrayList<>();
            this.outboundCommunicationContexts = new ArrayList<>();
        }

        @Override
        public OutputStream createTargetStream(CommunicationLog.TransportType path,
                                               CommunicationLog.Direction direction,
                                               CommunicationLog.MessageType messageType,
                                               CommunicationContext communicationContext) {
            var os = new CloseableByteArrayOutputStream();
            var appInfo = (HttpApplicationInfo) communicationContext.getApplicationInfo();
            if (CommunicationLog.Direction.INBOUND.equals(direction)) {
                inbound.add(os);
                inboundHeaders.add(appInfo.getHeaders());
                inboundMessageType = messageType;
                inboundTransactionIds.add(
                        ((HttpApplicationInfo) communicationContext.getApplicationInfo()).getTransactionId());
                inboundCommunicationContexts.add(communicationContext);
            } else {
                outbound.add(os);
                outboundHeaders.add(appInfo.getHeaders());
                outboundMessageType = messageType;
                outboundTransactionIds.add(
                        ((HttpApplicationInfo) communicationContext.getApplicationInfo()).getTransactionId());
                outboundCommunicationContexts.add(communicationContext);
            }
            return os;
        }

        public ArrayList<CloseableByteArrayOutputStream> getInbound() {
            return inbound;
        }

        public ArrayList<CloseableByteArrayOutputStream> getOutbound() {
            return outbound;
        }

        public ArrayList<ListMultimap<String, String>> getInboundHeaders() {
            return inboundHeaders;
        }

        public ArrayList<ListMultimap<String, String>> getOutboundHeaders() {
            return outboundHeaders;
        }

        public void clear() {
            outbound.clear();
            inbound.clear();
            inboundHeaders.clear();
            outboundHeaders.clear();
            inboundCommunicationContexts.clear();
            outboundCommunicationContexts.clear();
        }

        public CommunicationLog.MessageType getOutboundMessageType() {
            return outboundMessageType;
        }

        public CommunicationLog.MessageType getInboundMessageType() {
            return inboundMessageType;
        }

        public ArrayList<String> getInboundTransactionIds() {
            return inboundTransactionIds;
        }

        public ArrayList<String> getOutboundTransactionIds() {
            return outboundTransactionIds;
        }

        public ArrayList<CommunicationContext> getInboundCommunicationContexts() {
            return inboundCommunicationContexts;
        }

        public ArrayList<CommunicationContext> getOutboundCommunicationContexts() {
            return outboundCommunicationContexts;
        }
    }

    private SoapMessage createASoapMessage() {
        return soapMessageFactory.createSoapMessage(envelopeFactory.createEnvelope());
    }
}
