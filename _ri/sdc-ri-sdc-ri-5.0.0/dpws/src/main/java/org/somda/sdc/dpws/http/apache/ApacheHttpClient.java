package org.somda.sdc.dpws.http.apache;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.http.HttpClient;
import org.somda.sdc.dpws.http.apache.helper.ApacheClientHelper;
import org.somda.sdc.dpws.soap.exception.TransportException;

import java.io.IOException;
import java.net.SocketException;

/**
 * HTTP client implementation using a configured apache http client as backend.
 */
public class ApacheHttpClient implements HttpClient {
    private static final Logger LOG = LogManager.getLogger();

    private final org.apache.http.client.HttpClient client;
    private final Logger instanceLogger;

    @AssistedInject
    ApacheHttpClient(@Assisted org.apache.http.client.HttpClient client,
                     @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.client = client;
    }

    @Override
    public org.somda.sdc.dpws.http.HttpResponse sendGet(String url) throws TransportException {
        var get = new HttpGet(url);
        instanceLogger.debug("Sending GET request to {}", url);
        var response = executeRequest(client, get, url);
        return parseResponse(response, url);
    }

    private org.somda.sdc.dpws.http.HttpResponse parseResponse(HttpResponse response, String url)
            throws TransportException {
        var statusCode = response.getStatusLine().getStatusCode();
        byte[] content;
        try {
            content = response.getEntity().getContent().readAllBytes();
        } catch (IOException e) {
            instanceLogger.error("Error while reading response from {}. Message: {}", url, e.getMessage());
            instanceLogger.trace("Error while reading response from {}", url, e);
            throw new TransportException("Error while reading response", e);
        }

        var headers = ApacheClientHelper.allHeadersToMultimap(response.getAllHeaders());

        // finally consume the entire entity
        try {
            EntityUtils.consume(response.getEntity());
        } catch (IOException e) {
            instanceLogger.error("Error while consuming response entity from {}. Message: {}", url, e.getMessage());
            instanceLogger.trace("Error while consuming response entity from {}", url, e);
            throw new TransportException("Error while consuming response entity", e);
        }

        return new org.somda.sdc.dpws.http.HttpResponse(statusCode, content, headers);
    }

    private HttpResponse executeRequest(org.apache.http.client.HttpClient client, HttpRequestBase request,
                                        String endpoint)
            throws TransportException {
        try {
            return client.execute(request);
        } catch (SocketException e) {
            instanceLogger.error("Unexpected SocketException on request to {}. Message: {}", endpoint,
                    e.getMessage());
            instanceLogger.trace("Unexpected SocketException on request to {}", endpoint, e);
            throw new TransportException("Unexpected SocketException on request", e);
        } catch (IOException e) {
            instanceLogger.error("Unexpected IOException on request to {}. Message: {}", endpoint, e.getMessage());
            instanceLogger.trace("Unexpected IOException on request to {}", endpoint, e);
            throw new TransportException("Unexpected IOException on request", e);
        }
    }
}
