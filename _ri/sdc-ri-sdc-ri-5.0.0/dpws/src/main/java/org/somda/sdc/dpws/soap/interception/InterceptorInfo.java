package org.somda.sdc.dpws.soap.interception;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Package private information set to encapsulate information needed to invoke interceptor callbacks.
 */
class InterceptorInfo implements Comparable<InterceptorInfo> {
    private final Integer sequenceNumber;
    private final Interceptor callbackObject;
    private final Method callbackMethod;

    InterceptorInfo(Interceptor callbackObject, Method callbackMethod, Integer sequenceNumber) {
        this.callbackObject = callbackObject;
        this.callbackMethod = callbackMethod;
        this.sequenceNumber = sequenceNumber;
    }

    Method getCallbackMethod() {
        return callbackMethod;
    }

    Interceptor getCallbackObject() {
        return callbackObject;
    }

    int getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * Compares the interceptors sequence number in order to sort {@linkplain InterceptorInfo} objects.
     *
     * @param rhs right-hand side to compare against.
     */
    @Override
    public int compareTo(InterceptorInfo rhs) {
        return sequenceNumber.compareTo(rhs.sequenceNumber);
    }

    /**
     * Checks if this interceptor's sequence number is equal to the right-hand side.
     *
     * @param rhs right-hand side to compare against.
     * @return
     */
    @Override
    public boolean equals(Object rhs) {
        if (this == rhs) {
            return true;
        }
        if (rhs == null || getClass() != rhs.getClass()) {
            return false;
        }
        InterceptorInfo that = (InterceptorInfo) rhs;
        return sequenceNumber.equals(that.sequenceNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sequenceNumber);
    }
}
