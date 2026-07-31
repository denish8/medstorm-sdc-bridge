// package org.jvnet.jaxb.lang;

// import org.jvnet.jaxb.locator.ObjectLocator;

// /**
//  * Minimal version of jaxb2-basics Equals.
//  */
// public interface Equals {

//     /**
//      * Strategy-based equality used by generated classes.
//      */
//     boolean equals(ObjectLocator thisLocator,
//                    ObjectLocator thatLocator,
//                    Object object,
//                    EqualsStrategy strategy);

//     /**
//      * Plain Object-style equals – no default body here,
//      * we just declare it so generated code compiles.
//      */
//     boolean equals(Object other);
// }


package org.jvnet.jaxb.lang;

public interface Equals extends org.jvnet.jaxb2_commons.lang.Equals {}
