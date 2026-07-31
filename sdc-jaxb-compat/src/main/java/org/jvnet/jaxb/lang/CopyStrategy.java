package org.jvnet.jaxb.lang;

import org.jvnet.jaxb.locator.ObjectLocator;

/**
 * Legacy JAXB2 Basics CopyStrategy interface expected by javax-era generated code.
 * This surface must match callers in dpws-model (org.jvnet.jaxb.lang.*).
 */
public interface CopyStrategy {

    /**
     * Decide if a property should be copied and set.
     * Signature must use org.jvnet.jaxb.locator.ObjectLocator.
     */
    Boolean shouldBeCopiedAndSet(ObjectLocator locator, boolean value);

    /**
     * Copy a value with locator context.
     */
    Object copy(ObjectLocator locator, Object value, boolean valueSet);

    /**
     * Simple copy without locator context.
     */
    Object copy(Object value);
}
