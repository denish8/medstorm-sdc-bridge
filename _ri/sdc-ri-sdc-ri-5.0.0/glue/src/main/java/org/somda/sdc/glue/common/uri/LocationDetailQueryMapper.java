package org.somda.sdc.glue.common.uri;

import com.google.common.base.Strings;
import jregex.Matcher;
import jregex.Pattern;
import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.biceps.model.participant.LocationDetail;
import org.somda.sdc.glue.GlueConstants;
import org.somda.sdc.glue.common.helper.UrlUtf8;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Utility class to map location detail to and from URIs in accordance with SDC Glue section 9.4.1.2.
 */
public class LocationDetailQueryMapper {

    private static final Pattern PATTERN = new Pattern(GlueConstants.URI_REGEX);
    private static final Pattern QUERY_VALIDATOR = new Pattern(GlueConstants.LOC_CTXT_QUERY);

    /**
     * Creates a URI out of a location context instance identifier and location detail.
     *
     * @param instanceIdentifier a location context instance identifier.
     * @param locationDetail     the location detail to append.
     * @return a URI with appended location detail parameters or
     * the URI if something went wrong during URI re-construction.
     * @throws UriMapperGenerationArgumentException in case no valid URI could be generated from the input.
     */
    public static String createWithLocationDetailQuery(InstanceIdentifier instanceIdentifier,
                                                       LocationDetail locationDetail)
            throws UriMapperGenerationArgumentException {
        final String uri = ContextIdentificationMapper.fromInstanceIdentifier(instanceIdentifier,
                ContextIdentificationMapper.ContextSource.Location);
        StringBuilder queryParams = new StringBuilder("?");
        int count = 0;
        for (LocationDetailFields field : LocationDetailFields.values()) {
            try {
                final Method getter = field.getGetter();
                final var key = field.getQueryKey();
                final var value = (String) getter.invoke(locationDetail);
                if (Strings.isNullOrEmpty(value)) {
                    continue;
                }
                if (count++ > 0) {
                    queryParams.append('&');
                }
                queryParams.append(key).append('=').append(UrlUtf8.encodePChars(value, true));
            } catch (NoSuchMethodException | IllegalAccessException |
                    InvocationTargetException | ClassCastException e) {
                throw new UriMapperGenerationArgumentException(
                        "Unexpected reflection exception occurred during location detail appending of field " +
                                field.toString());
            }
        }

        final String queryParamsString = queryParams.toString();
        final String resultingUri = uri +
                ("?".equals(queryParamsString) ? "" : queryParamsString);

        try {
            readLocationDetailQuery(resultingUri);
        } catch (UriMapperParsingException e) {
            throw new UriMapperGenerationArgumentException(
                    "No valid URI could be generated from the given LocationDetail: '" + locationDetail.toString()
                            + "' and InstanceIdentifier: '" + instanceIdentifier.toString() + "'");
        }

        return resultingUri;
    }

    /**
     * Reads location detail query parameters from the given URI.
     *
     * @param uri the URI to parse.
     * @return a {@link LocationDetail} instance in which every field is filled that has an existing location detail
     * query parameter in <em>uri</em>.
     * @throws UriMapperParsingException in case no valid URI was given.
     */
    public static LocationDetail readLocationDetailQuery(String uri) throws UriMapperParsingException {

        Matcher uriMatcher = PATTERN.matcher(uri);

        if (uriMatcher.matches()) {
            String queryString = uriMatcher.group("query");

            if (queryString == null) {
                return new LocationDetail();
            }
            Matcher queryMatcher = QUERY_VALIDATOR.matcher(queryString);

            if (queryMatcher.matches()) {

                final var locationDetail = new LocationDetail();
                final var queryItems = splitQuery(queryString);
                for (LocationDetailFields field : LocationDetailFields.values()) {
                    final var values = queryItems.get(field.getQueryKey());
                    if (values != null && !values.isEmpty()) {
                        try {
                            final Method setter = field.getSetter();
                            setter.invoke(locationDetail, values.get(0));
                        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {

                            throw new UriMapperParsingException(
                                    "Unexpected reflection exception occurred " +
                                            "during location detail reading of field " +
                                            "for the mapper " + LocationDetailQueryMapper.class + " " +
                                            e.toString());
                        }
                    }
                }
                return locationDetail;

            } else {
                throw new UriMapperParsingException(
                        "Invalid Query in the URI for the mapper " + LocationDetailQueryMapper.class.toString());
            }
        } else {
            throw new UriMapperParsingException(
                    "Invalid URI for the mapper " + LocationDetailQueryMapper.class.toString());
        }


    }

    private static Map<String, List<String>> splitQuery(String query) throws UriMapperParsingException {
        final Map<String, List<String>> queryPairs = new LinkedHashMap<>();
        final String[] keyValuePair = query.split("&");
        for (String pair : keyValuePair) {
            final int equalCharIndex = pair.indexOf("=");
            final String key = equalCharIndex > 0 ? UrlUtf8.decodePChars(pair.substring(0, equalCharIndex)) : pair;
            if (!queryPairs.containsKey(key)) {
                queryPairs.put(key, new LinkedList<>());
            } else {
                throw new UriMapperParsingException(
                        "More than one query segment with the key '" + key + "'");
            }
            if (equalCharIndex > 0 && pair.length() > equalCharIndex + 1) {
                queryPairs.get(key).add(UrlUtf8.decodePChars(pair.substring(equalCharIndex + 1)));
            }
        }
        return queryPairs;
    }

    private enum LocationDetailFields {
        FACILITY("fac", "Facility"),
        BUILDING("bldng", "Building"),
        POINT_OF_CARE("poc", "PoC"),
        FLOOR("flr", "Floor"),
        ROOM("rm", "Room"),
        BED("bed", "Bed");

        private final String queryKey;
        private final String name;

        LocationDetailFields(String queryKey, String name) {

            this.queryKey = queryKey;
            this.name = name;
        }

        String getQueryKey() {
            return queryKey;
        }

        Method getSetter() throws NoSuchMethodException {
            return LocationDetail.class.getDeclaredMethod("set" + name, String.class);
        }

        Method getGetter() throws NoSuchMethodException {
            return LocationDetail.class.getDeclaredMethod("get" + name);
        }
    }
}