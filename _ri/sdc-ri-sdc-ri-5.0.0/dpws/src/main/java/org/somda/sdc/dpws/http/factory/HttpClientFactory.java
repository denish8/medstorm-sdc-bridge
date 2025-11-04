package org.somda.sdc.dpws.http.factory;

import org.somda.sdc.dpws.http.HttpClient;

/**
 * Guice factory for {@linkplain HttpClient}s.
 */
public interface HttpClientFactory {

    /**
     * Creates an http client.
     *
     * @return a transport binding bound to endpointUri.
     */
    HttpClient createHttpClient();

}
