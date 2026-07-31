package org.jvnet.jaxb.locator;

public final class ItemObjectLocator implements org.jvnet.jaxb.locator.ObjectLocator {
    private final org.jvnet.jaxb.locator.ObjectLocator parent;
    private final int index;
    private final Object object; // the located value

    public ItemObjectLocator(org.jvnet.jaxb.locator.ObjectLocator parent, int index, Object object) {
        this.parent = parent;
        this.index = index;
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
    public int getIndex() {               // index for [i]
        return index;
    }

    public String getPropertyName() {     // not applicable for item()
        return null;
    }

    // Optional helper (no @Override!)
    public Object getValue() {
        return object;
    }

    @Override
    public String toString() {
        return (parent != null ? parent.toString() + "[" + index + "]" : "[" + index + "]");
    }
}
