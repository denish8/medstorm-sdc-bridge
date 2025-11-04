package org.jvnet.jaxb.locator;

/**
 * Minimal legacy ObjectLocator contract compatible with jaxb2-basics 0.6.x.
 */
public interface ObjectLocator {
    /** The current object this locator points at (may be null). */
    Object getObject();

    /** Parent in the locator chain (may be null for root). */
    ObjectLocator getParentLocator();

    /** If this step was created for a bean property, its name; otherwise null. */
    String getPropertyName();

    /** If this step was created for a list/array item, its index; otherwise -1. */
    int getIndex();

    /** Create a child locator that points to a list/array element. */
    ObjectLocator item(int index, Object item);

    /** Create a child locator that points to a bean property value. */
    ObjectLocator property(String propertyName, Object value);
}
