package org.somda.sdc.dpws.http;

import java.net.URI;

/**
 * Helper class to build valid HTTP(S) URIs.
 */
public class HttpUriBuilder {
    /**
     * Creates an URI based on the given data.
     *
     * @param scheme the scheme to insert into the URI.
     * @param host   the host to insert into the URI.
     * @param port   the port to insert into the URI.
     * @return a URI as string.
     */
    public String buildUri(String scheme, String host, int port) {
        return URI.create(scheme + "://" + host + ":" + port).toString();
    }

    /**
     * Creates an HTTP URI with given host and port.
     *
     * @param host the host to insert into the URI.
     * @param port the port to insert into the URI.
     * @return a URI as string.
     */
    public String buildUri(String host, int port) {
        return buildUri("http", host, port);
    }

    /**
     * Creates an HTTPS URI with given host an port.
     *
     * @param host the host to insert into the URI.
     * @param port the port to insert into the URI.
     * @return a URI as string.
     */
    public String buildSecuredUri(String host, int port) {
        return buildUri("https", host, port);
    }
}
