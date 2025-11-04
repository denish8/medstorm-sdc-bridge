package org.jvnet.jaxb.lang;

import org.jvnet.jaxb.locator.ObjectLocator;

public interface EqualsStrategy {

    default boolean equals(ObjectLocator thisLocator, ObjectLocator thatLocator,
                           Object lhs, Object rhs, boolean lhsSet, boolean rhsSet) {
        if (!lhsSet && !rhsSet) {
            return true;
        }
        if (lhs == rhs) {
            return true;
        }
        if (lhs == null || rhs == null) {
            return false;
        }
        return lhs.equals(rhs);
    }
}
