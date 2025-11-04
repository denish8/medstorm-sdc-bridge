package org.somda.sdc.dpws.http;

import com.google.common.collect.ListMultimap;

/**
 * A basic http response containing status code and body.
 */
public class HttpResponse {
    private final int statusCode;
    private final byte[] body;
    private final ListMultimap<String, String> header;

    public HttpResponse(int statusCode, byte[] body, ListMultimap<String, String> header) {
        this.statusCode = statusCode;
        this.body = body;
        this.header = header;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public byte[] getBody() {
        return body;
    }

    public ListMultimap<String, String> getHeader() {
        return header;
    }
}
