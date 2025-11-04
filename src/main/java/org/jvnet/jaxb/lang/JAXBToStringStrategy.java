package org.jvnet.jaxb.lang;

public final class JAXBToStringStrategy
        extends org.jvnet.jaxb2_commons.lang.JAXBToStringStrategy
        implements ToStringStrategy {

    public static final ToStringStrategy INSTANCE = new JAXBToStringStrategy();

    private JAXBToStringStrategy() {
        super();
    }

    public static JAXBToStringStrategy getInstance() {
        return (JAXBToStringStrategy) INSTANCE;
    }
}
