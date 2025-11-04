// package org.jvnet.jaxb.lang;

// import org.jvnet.jaxb.locator.ObjectLocator;

// /**
//  * Minimal ToString interface used by some generated classes.
//  */
// public interface ToString {

//     /**
//      * Build a string using the given strategy.
//      */
//     StringBuilder append(ObjectLocator locator, StringBuilder buffer, ToStringStrategy strategy);

//     /**
//      * Convenience shape some generators expect.
//      */
//     StringBuilder append(StringBuilder buffer);

//     /**
//      * Object-style toString – declared, not implemented.
//      */
//     String toString();
// }


package org.jvnet.jaxb.lang;

public interface ToString extends org.jvnet.jaxb2_commons.lang.ToString {}
