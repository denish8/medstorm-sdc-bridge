package org.jvnet.jaxb.lang;

import org.jvnet.jaxb.locator.ObjectLocator;

/**
 * Minimal no-op copy strategy so old JAXB2 Basics-generated classes can load.
 */
public interface CopyStrategy {

    default boolean shouldBeCopiedAndSet(ObjectLocator locator, boolean value) {
        return value;
    }

    default Object copy(ObjectLocator locator, Object value, boolean valueSet) {
        // just return the value as-is
        return value;
    }
}
