package org.jvnet.jaxb.lang;

public final class JAXBEqualsStrategy
        extends org.jvnet.jaxb2_commons.lang.JAXBEqualsStrategy
        implements EqualsStrategy {

    public static final EqualsStrategy INSTANCE = new JAXBEqualsStrategy();

    private JAXBEqualsStrategy() {
        super();
    }

    public static JAXBEqualsStrategy getInstance() {
        return (JAXBEqualsStrategy) INSTANCE;
    }
}
