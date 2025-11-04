package org.jvnet.jaxb.lang;

import org.jvnet.jaxb.locator.ObjectLocator;

public interface ToStringStrategy {

    default StringBuilder appendStart(ObjectLocator locator, Object object, StringBuilder buffer) {
        return buffer != null ? buffer : new StringBuilder();
    }

    default StringBuilder appendField(ObjectLocator locator, Object object,
                                      String fieldName, StringBuilder buffer,
                                      Object value, boolean valueSet) {
        if (buffer == null) {
            buffer = new StringBuilder();
        }
        // very compact: fieldName=value
        if (fieldName != null) {
            buffer.append(fieldName).append('=').append(value).append(';');
        }
        return buffer;
    }

    default StringBuilder appendEnd(ObjectLocator locator, Object object, StringBuilder buffer) {
        return buffer != null ? buffer : new StringBuilder();
    }
}
