package org.somda.sdc.dpws.soap;

import com.google.inject.Inject;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.model.Envelope;

import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;

/**
 * Utility class to marshal/unmarshal SOAP messages.
 */
public class MarshallingService {
    private final SoapMarshalling soapMarshalling;
    private final SoapUtil soapUtil;

    @Inject
    MarshallingService(SoapUtil soapUtil,
                       SoapMarshalling soapMarshalling) {
        this.soapUtil = soapUtil;
        this.soapMarshalling = soapMarshalling;
    }

    /**
     * Marshals a SOAP message.
     *
     * @param msg the message to marshal.
     * @param os  the output stream where to write the XML message to.
     * @throws MarshallingException if any exception occurs during marshalling.
     */
    public void marshal(SoapMessage msg, OutputStream os) throws MarshallingException {
        try {
            soapMarshalling.marshal(msg.getEnvelopeWithMappedHeaders(), os);
        } catch (JAXBException e) {
            throw new MarshallingException(e);
        }
    }

    /**
     * Unmarshals a SOAP message from an input stream.
     *
     * @param is the input stream where to unmarshal from.
     * @return the unmarshalled object.
     * @throws MarshallingException if any exception occurs during unmarshalling.
     */
    public SoapMessage unmarshal(InputStream is) throws MarshallingException {
        try {
            Envelope env = soapMarshalling.unmarshal(is);
            return soapUtil.createMessage(env);
            // CHECKSTYLE.OFF: IllegalCatch
        } catch (Exception e) {
            // CHECKSTYLE.ON: IllegalCatch
            throw new MarshallingException(e);
        }
    }

    
    /**
     * Unmarshals a SOAP message from a reader.
     *
     * @param reader the input stream where to unmarshal from.
     * @return the unmarshalled object.
     * @throws MarshallingException if any exception occurs during unmarshalling.
     */
    public SoapMessage unmarshal(Reader reader) throws MarshallingException {
        try {
            Envelope env = soapMarshalling.unmarshal(reader);
            return soapUtil.createMessage(env);
            // CHECKSTYLE.OFF: IllegalCatch
        } catch (Exception e) {
            // CHECKSTYLE.ON: IllegalCatch
            throw new MarshallingException(e);
        }
    }
}
