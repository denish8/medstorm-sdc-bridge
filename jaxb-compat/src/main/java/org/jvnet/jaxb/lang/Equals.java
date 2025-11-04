package org.jvnet.jaxb.lang;

import jakarta.xml.bind.annotation.XmlTransient;

@XmlTransient
public interface Equals {

    default boolean equals(Object locatorThis, Object locatorThat, Object that, boolean value) {
        return value;
    }
}
