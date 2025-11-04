package org.somda.sdc.dpws.device.helper;

import java.net.URI;
import java.util.Optional;

/**
 * Helper to extract a base path from an URI.
 */
public class UriBaseContextPath {
    private final String basePath;

    /**
     * Constructor that accepts an URI and tries to cut out the base path on construction.
     *
     * @param uri the inspected URI.
     */
    public UriBaseContextPath(String uri) {
        this.basePath = deriveFrom(uri);
    }

    /**
     * Gets the extracted base path.
     *
     * @return the base path or an empty string if the parser was not able to find a base path.
     */
    public String get() {
        return basePath;
    }

    private String deriveFrom(String uri) {
        var parsedUri = URI.create(uri);
        final Optional<SupportedEprUriScheme> supportedUriScheme =
                getSupportedScheme(parsedUri.getScheme(), parsedUri.getSchemeSpecificPart());
        if (supportedUriScheme.isEmpty()) {
            return "";
        }
        switch (supportedUriScheme.get()) {
            case HTTP:
            case HTTPS:
                return parsedUri.getPath().substring(1); // skip preceding slash
            case URN_UUID:
            case URN_OID:
                return parsedUri.getSchemeSpecificPart()
                        .substring(supportedUriScheme.get().getSpecificPart().length() + 1);
            default:
                return "";
        }
    }

    private Optional<SupportedEprUriScheme> getSupportedScheme(String scheme, String schemeSpecificPart) {
        for (SupportedEprUriScheme supportedScheme : SupportedEprUriScheme.values()) {
            if (!supportedScheme.getSchemeName().equalsIgnoreCase(scheme)) {
                continue;
            }

            if (supportedScheme.getSpecificPart().isEmpty()) {
                return Optional.of(supportedScheme);
            }

            if (schemeSpecificPart.toLowerCase().startsWith(supportedScheme.getSpecificPart())) {
                return Optional.of(supportedScheme);
            }
        }

        return Optional.empty();
    }

}
