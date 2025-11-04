// package org.jvnet.jaxb.lang;

// import org.jvnet.jaxb.locator.ObjectLocator;

// /**
//  * Minimal strategy that just calls toString() on the object itself.
//  */
// public interface ToStringStrategy {

//     ToStringStrategy INSTANCE = new ToStringStrategy() {
//         @Override
//         public StringBuilder appendStart(ObjectLocator locator, Object object, StringBuilder buffer) {
//             buffer.append(object.getClass().getSimpleName()).append('[');
//             return buffer;
//         }

//         @Override
//         public StringBuilder appendEnd(ObjectLocator locator, Object object, StringBuilder buffer) {
//             buffer.append(']');
//             return buffer;
//         }
//     };

//     StringBuilder appendStart(ObjectLocator locator, Object object, StringBuilder buffer);

//     StringBuilder appendEnd(ObjectLocator locator, Object object, StringBuilder buffer);
// }


package org.jvnet.jaxb.lang;

public interface ToStringStrategy extends org.jvnet.jaxb2_commons.lang.ToStringStrategy {}
