package org.jvnet.jaxb.locator.util;

import org.jvnet.jaxb.locator.ObjectLocator;

/**
 * Simple immutable implementation of the legacy ObjectLocator.
 */
public final class SimpleObjectLocator implements ObjectLocator {
    private final ObjectLocator parent;
    private final Object object;
    private final String propertyName;
    private final int index;

    public SimpleObjectLocator(ObjectLocator parent, Object object, String propertyName, int index) {
        this.parent = parent;
        this.object = object;
        this.propertyName = propertyName;
        this.index = index;
    }

    /** Convenience factory for a root locator. */
    public static SimpleObjectLocator root(Object object) {
        return new SimpleObjectLocator(null, object, null, -1);
    }

    @Override public Object getObject() { return object; }
    @Override public ObjectLocator getParentLocator() { return parent; }
    @Override public String getPropertyName() { return propertyName; }
    @Override public int getIndex() { return index; }

    @Override
    public ObjectLocator item(int idx, Object item) {
        return new SimpleObjectLocator(this, item, null, idx);
    }

    @Override
    public ObjectLocator property(String name, Object value) {
        return new SimpleObjectLocator(this, value, name, -1);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (parent != null) sb.append(parent.toString());
        if (propertyName != null) sb.append('.').append(propertyName);
        if (index >= 0) sb.append('[').append(index).append(']');
        return sb.toString();
    }
}
