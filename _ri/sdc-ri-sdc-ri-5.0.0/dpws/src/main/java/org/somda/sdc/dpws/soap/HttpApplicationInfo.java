package org.somda.sdc.dpws.soap;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Utility class to provide application layer information for http.
 */
public class HttpApplicationInfo extends ApplicationInfo {

    private final ListMultimap<String, String> headers;
    private final String transactionId;
    private final String requestUri;

    /**
     * Creates an instance using http headers.
     * <p>
     * <em>All keys will be converted to lower case.</em>
     *
     * @param httpHeaders multimap of available headers.
     * @param transactionId id of the request response transaction.
     * @param requestUri the http request-uri, null for http response messages.
     */
    public HttpApplicationInfo(
            ListMultimap<String, String> httpHeaders,
            String transactionId, @Nullable String requestUri
    ) {
        this.headers = ArrayListMultimap.create();
        // convert all entries to lower case
        httpHeaders.forEach((key, value) -> headers.put(key.toLowerCase(), value));
        this.transactionId = transactionId;
        this.requestUri = requestUri;
    }

    /**
     * Retrieve http headers as multimap.
     * <p>
     * Each value seen for a key will be added to the list of values for said key.
     * All keys are lower case.
     *
     * @return {@linkplain ListMultimap} of all headers
     */
    public ListMultimap<String, String> getHeaders() {
        return ArrayListMultimap.create(headers);
    }

    /**
     * Returns an identifier for the underlying HTTP request.
     *
     * @return the identifier, e.g. to be used to deduce relations between request and response messages.
     */
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Retrieve the Optional of http request-uri, empty in case of a http response message.
     *
     * @return {@linkplain Optional} of the request-uri
     */
    public Optional<String> getRequestUri() {
        return Optional.ofNullable(requestUri);
    }
}
