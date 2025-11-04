package org.somda.sdc.glue.common.helper;

import org.apache.http.client.utils.URLEncodedUtils;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;

/**
 * Helper to encode and decode URLs to and from UTF-8.
 */
public class UrlUtf8 {

    /**
     * Accepts a text and encodes it as valid pchar according to RFC3986.
     *
     * @param text the text to encode.
     * @return the encoded text or an empty string if text was null.
     */
    public static String encodePChars(@Nullable String text) {
        return encodePChars(text, false);
    }

    /**
     * Accepts a text and encodes it as valid pchar according to RFC3986.
     *
     * @param text the text to encode.
     * @param escapeAmpersand percent-encode ampersands (&amp;) in the text. This is required when the context already
     *                        uses ampersands as a delimiter, such as in the queries for
     *                        location context query transformation
     * @return the encoded text or an empty string if text was null.
     */
    public static String encodePChars(@Nullable String text, boolean escapeAmpersand) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        var encoded =  URLEncodedUtils.formatSegments(text).substring(1);
        if (escapeAmpersand) {
            encoded = encoded.replace("&", "%26");
        }
        return encoded;
    }

    /**
     * Accepts a text and decodes it as pchar according to RFC3986.
     *
     * @param text the text to decode.
     * @return the encoded text or an empty string if text was null.
     */
    public static String decodePChars(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        return URLEncodedUtils.parsePathSegments(text, StandardCharsets.UTF_8).get(0);
    }
}
