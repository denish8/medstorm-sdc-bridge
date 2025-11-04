package org.somda.sdc.common.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@linkplain JaxbUtil}.
 */
public class JaxbUtilImpl implements JaxbUtil {
    private static final Logger LOG = LogManager.getLogger(JaxbUtilImpl.class);

    @Override
    public <T> Optional<T> extractElement(Object element, QName elementType, Class<T> typeClass) {
        Optional<Object> extractedObj = extractElement(element, elementType);
        if (extractedObj.isPresent() && typeClass.isAssignableFrom(extractedObj.get().getClass())) {
            return Optional.of(typeClass.cast(extractedObj.get()));
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> extractElement(Object element, Class<T> typeClass) {
        try {
            if (element instanceof JAXBElement) {
                T value = ((JAXBElement<T>) element).getValue();
                return Optional.of(typeClass.cast(value));
            } else {
                return Optional.of(typeClass.cast(element));
            }
        } catch (ClassCastException e) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> extractElement(Object element, QName elementType) {
        try {
            JAXBElement<T> elementAsJaxb = (JAXBElement<T>) element;
            if (elementAsJaxb.getName().equals(elementType)) {
                return Optional.ofNullable(elementAsJaxb.getValue());
            }
        } catch (ClassCastException e) {
            LOG.trace("Object was not a JAXBElement, extracting elements failed but it's alright");
        }
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> extractFirstElementFromAny(List<Object> anyList, QName elementType, Class<T> typeClass) {
        if (anyList.isEmpty()) {
            return Optional.empty();
        }

        Optional<T> first = extractElement(anyList.get(0), elementType, typeClass);
        if (first.isPresent()) {
            try {
                return Optional.of(typeClass.cast(first.get()));
            } catch (ClassCastException ignored) {
                // ignore, empty optional will be returned
            }
        }

        return Optional.empty();
    }

    @Override
    public <T> Optional<T> extractFirstElementFromAny(List<Object> anyList, Class<T> typeClass) {
        if (anyList.isEmpty()) {
            return Optional.empty();
        }

        Optional<T> first = extractElement(anyList.get(0), typeClass);
        if (first.isPresent()) {
            try {
                return Optional.of(typeClass.cast(first.get()));
            } catch (ClassCastException ignored) {
                // ignore, empty optional will be returned
            }
        }

        return Optional.empty();
    }
}
