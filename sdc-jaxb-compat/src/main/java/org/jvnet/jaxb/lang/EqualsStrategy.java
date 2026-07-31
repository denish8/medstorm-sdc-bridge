// package org.jvnet.jaxb.lang;

// import org.jvnet.jaxb.locator.ObjectLocator;

// /**
//  * Minimal equality strategy.
//  */
// public interface EqualsStrategy {

//     EqualsStrategy INSTANCE = new EqualsStrategy() {
//         @Override
//         public boolean equals(ObjectLocator leftLocator, ObjectLocator rightLocator,
//                               Object left, Object right) {
//             if (left == right) {
//                 return true;
//             }
//             if (left == null || right == null) {
//                 return false;
//             }
//             return left.equals(right);
//         }
//     };

//     boolean equals(ObjectLocator leftLocator, ObjectLocator rightLocator,
//                    Object left, Object right);
// }


package org.jvnet.jaxb.lang;

public interface EqualsStrategy extends org.jvnet.jaxb2_commons.lang.EqualsStrategy {}
