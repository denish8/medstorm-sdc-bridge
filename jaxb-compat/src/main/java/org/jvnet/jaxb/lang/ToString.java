package org.jvnet.jaxb.lang;

import jakarta.xml.bind.annotation.XmlTransient;

@XmlTransient
public interface ToString {

    default String toString(Object locator, StringBuilder buffer) {
        return buffer != null ? buffer.toString() : "";
    }

    default StringBuilder append(Object locator, StringBuilder buffer) {
        return buffer != null ? buffer : new StringBuilder();
    }
}
