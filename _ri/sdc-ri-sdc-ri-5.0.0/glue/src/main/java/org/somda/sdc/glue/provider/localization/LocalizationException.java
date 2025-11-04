package org.somda.sdc.glue.provider.localization;

/**
 * Indicates that error occurs trying to get localized text using {@linkplain LocalizationService}.
 */
public class LocalizationException extends RuntimeException {
    public LocalizationException(String message) {
        super(message);
    }

    public LocalizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
