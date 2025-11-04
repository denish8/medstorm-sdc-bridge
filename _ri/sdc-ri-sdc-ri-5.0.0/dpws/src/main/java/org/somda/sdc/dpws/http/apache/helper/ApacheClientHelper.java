package org.somda.sdc.dpws.http.apache.helper;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.http.Header;

import java.util.Arrays;
import java.util.Map;

/**
 * Utilities for the apache http client.
 */
public class ApacheClientHelper {

    /**
     * Converts apache headers into a {@linkplain ListMultimap}.
     *
     * @param allHeaders array of apache {@linkplain Header} elements.
     * @return {@linkplain Map} of all header entries.
     */
    public static ListMultimap<String, String> allHeadersToMultimap(Header[] allHeaders) {
        ListMultimap<String, String> mappedHeaders = ArrayListMultimap.create();
        Arrays.stream(allHeaders).forEach(header -> mappedHeaders.put(
                header.getName(),
                header.getValue()
        ));
        return mappedHeaders;

    }
}
