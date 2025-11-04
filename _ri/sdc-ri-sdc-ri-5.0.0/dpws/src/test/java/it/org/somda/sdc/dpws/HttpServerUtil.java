package it.org.somda.sdc.dpws;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.HttpHeaders;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.somda.sdc.dpws.soap.SoapConstants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class HttpServerUtil {
    private static final Logger LOG = LogManager.getLogger(HttpServerUtil.class);


    public static HttpServer spawnHttpServer(InetSocketAddress addr, HttpHandler httpHandler) throws IOException {
        HttpServer server = HttpServer.create(addr, 0);
        server.createContext("/", httpHandler);
        server.setExecutor(null); // creates a default executor
        server.start();
        return server;
    }

    public static class GzipResponseHandler implements HttpHandler {
        public static final String TEST_HEADER_KEY = "SDCriTestHeader";
        public static final String TEST_HEADER_VALUE = "anAmazingValue";

        private final byte[] compressedResponse;

        GzipResponseHandler(byte[] response) throws IOException {
            ByteArrayOutputStream responseBais = new ByteArrayOutputStream();
            try (GZIPOutputStream gzos = new GZIPOutputStream(responseBais)) {
                gzos.write(response);
            }
            this.compressedResponse = responseBais.toByteArray();
        }

        @Override
        public void handle(HttpExchange t) throws IOException {

            List<String> strings = t.getRequestHeaders().get(HttpHeaders.ACCEPT_ENCODING);
            if (strings.stream().noneMatch(x -> x.contains("gzip"))) {
                LOG.error("No Accept-Encoding with value gzip in request header");
                throw new RuntimeException("No Accept-Encoding with value gzip in request header");
            }

            t.getResponseHeaders().add(TEST_HEADER_KEY, TEST_HEADER_VALUE);
            t.getResponseHeaders().add(HttpHeaders.CONTENT_ENCODING, "gzip");
            t.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, SoapConstants.MEDIA_TYPE_SOAP);
            t.sendResponseHeaders(200, compressedResponse.length);
            OutputStream os = t.getResponseBody();
            os.write(compressedResponse);
            os.close();
        }
    }

}
