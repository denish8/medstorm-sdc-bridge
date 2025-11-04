package org.somda.sdc.dpws.soap;

import com.google.common.util.concurrent.Service;
import org.somda.sdc.dpws.soap.model.Envelope;

import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;

/**
 * JAXB SOAP marshalling service.
 */
public interface SoapMarshalling extends Service {
    /**
     * Marshals a SOAP message.
     *
     * @param envelope     The envelope of the SOAP message.
     * @param outputStream The output stream where to write marshalled data to.
     * @throws JAXBException if marshalling to output stream failed
     */
    void marshal(Envelope envelope, OutputStream outputStream) throws JAXBException;

    /**
     * Unmarshals a SOAP message.
     *
     * @param inputStream the input message to unmarshal a SOAP message from.
     * @return the unmarshalled SOAP envelope.
     * @throws JAXBException      if unmarshalling fails.
     * @throws ClassCastException if the cast to {@link Envelope} fails.
     */
    Envelope unmarshal(InputStream inputStream) throws JAXBException, ClassCastException;

    /**
     * Unmarshals a SOAP message.
     *
     * @param reader the input message to unmarshal a SOAP message from.
     * @return the unmarshalled SOAP envelope.
     * @throws JAXBException      if unmarshalling fails.
     * @throws ClassCastException if the cast to {@link Envelope} fails.
     */
    Envelope unmarshal(Reader reader) throws JAXBException, ClassCastException;
}
