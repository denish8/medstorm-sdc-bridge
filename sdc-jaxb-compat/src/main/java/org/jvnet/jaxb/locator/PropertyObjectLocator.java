package org.jvnet.jaxb.locator;

public final class PropertyObjectLocator implements org.jvnet.jaxb.locator.ObjectLocator {
    private final org.jvnet.jaxb.locator.ObjectLocator parent;
    private final String propertyName;
    private final Object object; // the located value

    public PropertyObjectLocator(org.jvnet.jaxb.locator.ObjectLocator parent, String propertyName, Object object) {
        this.parent = parent;
        this.propertyName = propertyName;
        this.object = object;
    }

    // === ObjectLocator (shim) ===
    @Override
    public org.jvnet.jaxb.locator.ObjectLocator getParentLocator() {
        return parent;
    }

    @Override
    public ItemObjectLocator item(int idx, Object val) {
        return new ItemObjectLocator(this, idx, val);
    }

    @Override
    public PropertyObjectLocator property(String name, Object val) {
        return new PropertyObjectLocator(this, name, val);
    }

    @Override
    public Object getObject() {           // NEW: required by ObjectLocator
        return object;
    }

    // Convenience accessors expected by your shim usage
    public String getPropertyName() {     // property for .prop
        return propertyName;
    }

    public int getIndex() {               // not applicable for property()
        return -1;
    }

    // Optional helper (no @Override!)
    public Object getValue() {
        return object;
    }

    @Override
    public String toString() {
        return (parent != null ? parent.toString() + "." + propertyName : propertyName);
    }
}
