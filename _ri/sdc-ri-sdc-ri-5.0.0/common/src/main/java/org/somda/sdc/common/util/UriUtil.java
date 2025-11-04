package org.somda.sdc.common.util;

import java.util.UUID;

/**
 * Utility functions for URIs.
 */
public class UriUtil {
    /**
     * Creates a UUID URN from a hex-encoded UUID.
     *
     * @param hexDigitEncodedUuid the hex-encoded UUID.
     * @return a UUID URN.
     */
    public static String createUuid(String hexDigitEncodedUuid) {
        return "urn:uuid:" + hexDigitEncodedUuid;
    }

    /**
     * Creates a UUID URN from a {@linkplain UUID}.
     *
     * @param uuid the UUID to convert to a URN.
     * @return a UUID URN.
     */
    public static String createUuid(UUID uuid) {
        return createUuid(uuid.toString());
    }
}
