package org.somda.sdc.dpws.wsdl;

import com.google.common.util.concurrent.Service;
import org.somda.sdc.dpws.wsdl.model.TDefinitions;

import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * JAXB WSDL marshalling service.
 */
public interface WsdlMarshalling extends Service {
    /**
     * Marshals a WSDL document.
     *
     * @param wsdlDefinitions the WSDL definition to marshal.
     * @param outputStream    the output stream where to write marshalled data to.
     * @throws JAXBException if marshalling to output stream failed
     */
    void marshal(TDefinitions wsdlDefinitions, OutputStream outputStream) throws JAXBException;

    /**
     * Unmarshals a WSDL document.
     *
     * @param inputStream the input WSDL definition to unmarshal.
     * @return the unmarshalled WSDL definition.
     * @throws JAXBException      if unmarshalling fails.
     * @throws ClassCastException if the cast to {@link TDefinitions} fails.
     */
    TDefinitions unmarshal(InputStream inputStream) throws JAXBException, ClassCastException;
}
