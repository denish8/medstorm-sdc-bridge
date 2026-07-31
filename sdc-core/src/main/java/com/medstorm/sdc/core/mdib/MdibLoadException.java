package com.medstorm.sdc.core.mdib;

/**
 * Thrown when an MDIB could not be read or committed to an
 * {@link org.somda.sdc.biceps.provider.access.LocalMdibAccess}.
 *
 * <p>Wraps the four unrelated failure types the underlying SDC-ri calls can raise
 * ({@code JAXBException}, {@code ClassCastException}, {@code IOException} and
 * {@code PreprocessingException}) so callers have one thing to catch.
 */
public class MdibLoadException extends Exception {

    public MdibLoadException(String message, Throwable cause) {
        super(message, cause);
    }

    public MdibLoadException(String message) {
        super(message);
    }
}
