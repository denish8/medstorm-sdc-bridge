package org.somda.sdc.dpws.http;

import com.google.common.collect.ListMultimap;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.model.ObjectStringifier;
import org.somda.sdc.common.model.Stringified;
import org.somda.sdc.dpws.soap.SoapConstants;

import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Optional;

/**
 * Handler for parsing Content-Types accepted by SDCri.
 * <p>
 * This is currently limited to three content-types: text/xml, application/xml and application/soap+xml.
 */
public class ContentType {
    private static final Logger LOG = LogManager.getLogger();
    private static final String CHARSET = "charset";
    private static final String BOUNDARY = "boundary";

    @Stringified
    private final ContentTypes contentType;
    @Stringified
    private final Charset charset;
    @Stringified
    private final String boundary;

    ContentType(ContentTypes contentType, @Nullable Charset charset, @Nullable String boundary) {
        this.contentType = contentType;
        this.boundary = boundary;

        if (charset == null) {
            this.charset = contentType.defaultEncoding;
        } else {
            this.charset = charset;
        }
    }

    /**
     * Parses a charset element to a java {@linkplain Charset}.
     *
     * @param charset to parse
     * @return {@linkplain Optional} containing charset if understood by java, empty otherwise
     */
    private static Optional<Charset> parseCharset(@Nullable String charset) {
        if (charset != null && !charset.isBlank()) {
            try {
                return Optional.of(Charset.forName(charset));
            } catch (final UnsupportedCharsetException ex) {
                LOG.debug("Could not parse unknown charset {}", charset);
            }
        }
        return Optional.empty();
    }

    /**
     * Parses the content-type entry from a {@link ListMultimap} containing header entries.
     *
     * <em>Http header keys must be lower case, i.e. content-type, not Content-Type/</em>
     *
     * @param headers to parse content-type from
     * @return parsed content-type if successful, empty optional otherwise
     */
    public static Optional<ContentType> fromListMultimap(ListMultimap<String, String> headers) {
        var contentTypeList = headers.get(HttpHeaders.CONTENT_TYPE.toLowerCase());
        if (contentTypeList.size() != 1) {
            return Optional.empty();
        }
        var contentTypeEntry = contentTypeList.get(0);
        var elements = contentTypeEntry.split(";");
        final var contentTypeOpt = ContentTypes.fromMime(elements[0]);
        if (contentTypeOpt.isEmpty()) {
            return Optional.empty();
        }
        final var contentType = contentTypeOpt.get();

        Charset charset = null;
        String boundary = null;
        if (elements.length > 1) {
            // skip the first element, it's the media-type again
            for (int i = 1; i < elements.length; i++) {
                var element = elements[i];
                var parts = element.strip().split("=");
                assert parts.length == 2;
                var key = parts[0];
                var value = parts[1];

                if (CHARSET.equalsIgnoreCase(key)) {
                    var parsedCharset = parseCharset(value);
                    if (parsedCharset.isEmpty()) {
                        LOG.error("Unknown charset provided with content type {}. Charset: {}", contentType, value);
                        return Optional.empty();
                    }
                    charset = parsedCharset.get();
                } else if (BOUNDARY.equalsIgnoreCase(key)) {
                    boundary = value;
                }
            }
        }
        return Optional.of(new ContentType(contentType, charset, boundary));
    }

    /**
     * Parses the content-type {@linkplain Header} element provided by apache http client.
     *
     * @param header to parse content-type from
     * @return parsed content-type if parseable, empty optional otherweise
     */
    public static Optional<ContentType> fromApache(@Nullable Header header) {
        if (header == null || header.getElements().length != 1) {
            return Optional.empty();
        }
        final var headerElement = header.getElements()[0];
        final var contentTypeOpt = ContentTypes.fromMime(headerElement.getName());
        if (contentTypeOpt.isEmpty()) {
            return Optional.empty();
        }
        final var contentType = contentTypeOpt.get();
        Charset charset = null;
        String boundary = null;
        var charsetParam = headerElement.getParameterByName(CHARSET);
        var boundaryParam = headerElement.getParameterByName(BOUNDARY);
        if (charsetParam != null) {
            final String s = charsetParam.getValue();
            var parsedCharset = parseCharset(s);
            if (parsedCharset.isEmpty()) {
                LOG.error("Unknown charset provided with content type {}. charset: {}", contentType, s);
                return Optional.empty();
            }
            charset = parsedCharset.get();
        }
        if (boundaryParam != null) {
            boundary = boundaryParam.getValue();
        }

        return Optional.of(new ContentType(contentType, charset, boundary));
    }

    public Charset getCharset() {
        return charset;
    }

    public ContentTypes getContentType() {
        return contentType;
    }

    public String getBoundary() {
        return boundary;
    }

    @Override
    public String toString() {
        return ObjectStringifier.stringify(this);
    }

    /**
     * Representation of all accepted content-types as well as their default charset, if it is given.
     */
    public enum ContentTypes {
        TEXT_XML(SoapConstants.MEDIA_TYPE_WSDL, StandardCharsets.ISO_8859_1),
        APPLICATION_XML(SoapConstants.MEDIA_TYPE_XML, null),
        // although the registration specifically says that an omitted charset provides no information,
        // mdpws (tries) to enforce UTF-8 for all soap+xml messages
        APPLICATION_SOAP_XML(SoapConstants.MEDIA_TYPE_SOAP, StandardCharsets.UTF_8);

        private final String contentType;
        private final Charset defaultEncoding;

        ContentTypes(String contentType, @Nullable Charset defaultEncoding) {
            this.contentType = contentType;
            this.defaultEncoding = defaultEncoding;
        }

        /**
         * Maps a mime-type to the respective {@linkplain ContentTypes} enum value.
         *
         * @param mime to map to content types value
         * @return enum value if present, empty {@linkplain Optional} otherwise
         */
        public static Optional<ContentTypes> fromMime(String mime) {
            for (ContentTypes contentType : ContentTypes.values()) {
                if (contentType.contentType.equalsIgnoreCase(mime)) {
                    return Optional.of(contentType);
                }
            }
            return Optional.empty();
        }

        public Charset getDefaultEncoding() {
            return defaultEncoding;
        }

        public String getContentType() {
            return contentType;
        }
    }
}
