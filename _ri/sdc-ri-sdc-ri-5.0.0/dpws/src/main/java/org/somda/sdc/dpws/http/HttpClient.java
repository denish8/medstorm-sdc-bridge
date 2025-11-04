package org.somda.sdc.dpws.http;

import org.somda.sdc.dpws.soap.exception.TransportException;

/**
 * Simple HTTP client to enable performing requests.
 */
public interface HttpClient {
    /**
     * Sends a HTTP GET request to a specified url.
     *
     * @param url to send request to
     * @return response body
     * @throws TransportException on transport related issues, such as connection refused
     */
    HttpResponse sendGet(String url) throws TransportException;
}
