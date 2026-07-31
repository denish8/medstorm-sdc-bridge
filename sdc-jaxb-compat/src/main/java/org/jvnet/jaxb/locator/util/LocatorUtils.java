package org.jvnet.jaxb.locator.util;

import org.jvnet.jaxb.locator.ItemObjectLocator;
import org.jvnet.jaxb.locator.ObjectLocator;
import org.jvnet.jaxb.locator.PropertyObjectLocator;

public final class LocatorUtils {
    private LocatorUtils() {}

    public static PropertyObjectLocator property(ObjectLocator parent, String name, Object value) {
        return new PropertyObjectLocator(parent, name, value);
    }

    public static ItemObjectLocator item(ObjectLocator parent, int index, Object value) {
        return new ItemObjectLocator(parent, index, value);
    }
}
