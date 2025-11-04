// package org.jvnet.jaxb.lang;

// import org.jvnet.jaxb.locator.ObjectLocator;

// /**
//  * Minimal JAXB2-Basics compatible CopyTo interface.
//  * Some generated SDC model classes call copyTo(...) with a locator + strategy.
//  */
// public interface CopyTo {
//     /**
//      * Copy this object using the given strategy.
//      */
//     Object copyTo(ObjectLocator locator, Object target, CopyStrategy strategy);

//     /**
//      * Convenience: copy without locator/strategy.
//      */
//     default Object copyTo(Object target) {
//         return copyTo(null, target, CopyStrategy.INSTANCE);
//     }
// }


package org.jvnet.jaxb.lang;

public interface CopyTo extends org.jvnet.jaxb2_commons.lang.CopyTo {}
