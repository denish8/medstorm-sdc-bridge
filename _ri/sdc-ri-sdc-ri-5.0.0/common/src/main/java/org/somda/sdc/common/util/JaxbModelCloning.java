package org.somda.sdc.common.util;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBSource;
import javax.xml.namespace.QName;

/**
 * Abstract JAXB objects model cloning class with common methods.
 * <p>
 * Object cloning is done by JAXB marshalling and unmarshalling.
 */
public class JaxbModelCloning {

    private final JAXBContext jaxbContext;


    protected JaxbModelCloning(String jaxbContextPackages) {
        try {
            jaxbContext = JAXBContext.newInstance(jaxbContextPackages);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a deep copy of given BICEPS or DPWS model object.
     *
     * @param object the object to copy.
     * @param <T> BICEPS or DPWS model class.
     * @return deep copy of given object.
     */
    public <T> T deepCopy(T object) {
        return deepCopy(object, (Class<T>) object.getClass());
    }

    private <T> T deepCopy(T object, Class<T> clazz) {
        try {
            JAXBElement<T> contentObject = new JAXBElement<>(new QName(clazz.getSimpleName()), clazz, object);
            JAXBSource source = new JAXBSource(jaxbContext, contentObject);
            var unmarshaller = jaxbContext.createUnmarshaller();
            return unmarshaller.unmarshal(source, clazz).getValue();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}
