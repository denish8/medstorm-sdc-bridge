package org.somda.sdc.common.util;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Optional;

/**
 * Utility functions for JAXB.
 */
public interface JaxbUtil {

    /**
     * Tries to cast the element to given type object and returns it as an {@linkplain Optional}.
     *
     * @param element   the element to inspect.
     * @param typeClass the QName type specification.
     * @param <T>       the casted generic type.
     * @return {@linkplain JAXBElement#getValue()} or {@linkplain Optional#empty()} on error.
     */
    <T> Optional<T> extractElement(Object element, Class<T> typeClass);

    /**
     * Tries to cast the element to a {@linkplain JAXBElement} object with given QName type and returns it as an
     * {@linkplain Optional}.
     *
     * @param element     the element to inspect.
     * @param elementType the QName type specification.
     * @param <T>         the generic type to cast to.
     * @return {@linkplain JAXBElement#getValue()} or {@linkplain Optional#empty()} on error.
     */
    <T> Optional<T> extractElement(Object element, QName elementType);

    /**
     * Same as {@link #extractElement(Object, QName)} but with QName JAXB type class in addition.
     *
     * @param element     the element to inspect.
     * @param elementType the QName type specification.
     * @param typeClass   Java JAXB class that matches elementType.
     * @param <T>         the generic type to cast to.
     * @return {@linkplain JAXBElement#getValue()} or {@linkplain Optional#empty()} on error.
     */
    <T> Optional<T> extractElement(Object element, QName elementType, Class<T> typeClass);

    /**
     * From a list of {@link JAXBElement} objects, this function retrieves the first element in the list
     * <p>
     * Additionally, it tries to cast it to the QName type given by elementType.
     *
     * @param anyList     a list of {@link JAXBElement} objects.
     * @param elementType the QName type specification.
     * @param typeClass   Java JAXB class that matches elementType.
     * @param <T>         the generic type to cast to.
     * @return {@linkplain Optional} of the first element's {@link JAXBElement#getValue()} or {@link Optional#empty()}
     * if the list is empty or an error occurred.
     */
    <T> Optional<T> extractFirstElementFromAny(List<Object> anyList, QName elementType, Class<T> typeClass);

    /**
     * Same as {@link #extractFirstElementFromAny(List, QName, Class)}, but without comparing QName in advance.
     *
     * @param anyList   a list of {@link JAXBElement} objects.
     * @param typeClass Java JAXB class that matches elementType.
     * @param <T>       the generic type to cast to.
     * @return {@linkplain Optional} of the first element's {@link JAXBElement#getValue()} or {@link Optional#empty()}
     * if the list is empty or an error occurred.
     */
    <T> Optional<T> extractFirstElementFromAny(List<Object> anyList, Class<T> typeClass);
}
