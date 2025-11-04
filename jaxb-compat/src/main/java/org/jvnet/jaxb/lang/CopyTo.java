package org.jvnet.jaxb.lang;

/**
 * Old jvnet-style CopyTo interface, no JAXB types, no annotations.
 */
public interface CopyTo {

    /**
     * Simplest variant – many generated classes call exactly this.
     */
    Object copyTo(Object target);

    /**
     * Variant with strategy.
     */
    Object copyTo(CopyStrategy strategy, Object target);

    /**
     * Some generators emitted this.
     */
    Object createNewInstance();
}
