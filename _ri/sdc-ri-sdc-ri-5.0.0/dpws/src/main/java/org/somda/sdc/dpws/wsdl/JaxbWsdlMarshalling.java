package org.somda.sdc.dpws.wsdl;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.helper.JaxbMarshalling;
import org.somda.sdc.dpws.wsdl.model.ObjectFactory;
import org.somda.sdc.dpws.wsdl.model.TDefinitions;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Creates XML input and output streams from WSDL {@link TDefinitions} instances by using JAXB.
 */
public class JaxbWsdlMarshalling extends AbstractIdleService implements WsdlMarshalling {
    private static final Logger LOG = LogManager.getLogger(JaxbWsdlMarshalling.class);

    private final Logger instanceLogger;
    private final ObjectFactory wsdlFactory;
    private final JaxbMarshalling jaxbMarshalling;

    @Inject
    JaxbWsdlMarshalling(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                        ObjectFactory wsdlFactory,
                        JaxbMarshalling jaxbMarshalling) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.wsdlFactory = wsdlFactory;
        this.jaxbMarshalling = jaxbMarshalling;
    }

    @Override
    protected void startUp() throws Exception {
        instanceLogger.info("WSDL marshalling started");
    }

    @Override
    protected void shutDown() {
        instanceLogger.info("WSDL marshalling stopped");
    }

    /**
     * Takes a WSDL definition and marshals it.
     *
     * @param wsdlDefinition the WSDL to marshal.
     * @param outputStream   the destination of the marshalled data.
     * @throws JAXBException if marshalling fails.
     */
    @Override
    public void marshal(TDefinitions wsdlDefinition, OutputStream outputStream) throws JAXBException {
        checkRunning();
        jaxbMarshalling.marshal(wsdlFactory.createDefinitions(wsdlDefinition), outputStream);
    }

    private void checkRunning() {
        if (!isRunning()) {
            throw new RuntimeException("Try to marshal, but WSDL marshalling service is not running. " +
                    "Please check if the DPWS framework is up and running.");
        }
    }

    /**
     * Takes an input stream and unmarshals it to a WSDL definition.
     *
     * @param inputStream the input stream to unmarshal.
     * @return the unmarshalled WSDL definition.
     * @throws JAXBException      if unmarshalling fails.
     * @throws ClassCastException in case unmarshalled data could not be cast to a JAXB element.
     */
    @Override
    @SuppressWarnings("unchecked")
    public TDefinitions unmarshal(InputStream inputStream) throws JAXBException, ClassCastException {
        checkRunning();
        return ((JAXBElement<TDefinitions>) (jaxbMarshalling.unmarshal(inputStream))).getValue();
    }
}
