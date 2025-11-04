package org.somda.sdc.dpws.soap.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.model.Envelope;

/**
 * Guice factory interface to create {@linkplain SoapMessage} instances.
 */
public interface SoapMessageFactory {
    /**
     * Creates a SOAP message.
     *
     * @param envelope the envelope that is enclosed by the {@link SoapMessage}.
     *                 All known headers will be parsed and copied to the convenience header access.
     * @return a new {@link SoapMessage} instance containing the envelope.
     */
    SoapMessage createSoapMessage(@Assisted Envelope envelope);
}
