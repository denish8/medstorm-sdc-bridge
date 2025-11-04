package org.somda.sdc.biceps.testutil;

import org.checkerframework.checker.units.qual.C;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.ContextAssociation;

import java.math.BigInteger;

/**
 * Test utility to create MDIB types with handle information.
 */
public class MockModelFactory {
    public static <T extends AbstractDescriptor> T createDescriptor(String handle, Class<T> type) {
        return createDescriptor(handle, BigInteger.ZERO, type);
    }

    public static <T extends AbstractDescriptor> T createDescriptor(String handle, BigInteger version, Class<T> type) {
        try {
            T instance = type.getDeclaredConstructor().newInstance();
            instance.setHandle(handle);
            instance.setDescriptorVersion(version);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T extends AbstractState> T createState(String handle, Class<T> type) {
        try {
            return createState(handle, BigInteger.ZERO, BigInteger.ZERO, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T extends AbstractState> T createState(String handle, BigInteger stateVersion, Class<T> type) {
        try {
            return createState(handle, stateVersion, BigInteger.ZERO, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T extends AbstractState> T createState(String handle,
                                                          BigInteger stateVersion,
                                                          BigInteger descriptorVersion,
                                                          Class<T> type) {
        try {
            T instance = type.getDeclaredConstructor().newInstance();
            instance.setDescriptorHandle(handle);
            instance.setStateVersion(stateVersion);
            instance.setDescriptorVersion(descriptorVersion);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T extends AbstractContextState> T createContextState(String handle, String descrHandle, Class<T> type) {
        return createContextState(handle, descrHandle, BigInteger.ZERO, BigInteger.ZERO, type);
    }

    public static <T extends AbstractContextState> T createContextState(String handle,
                                                                        String descrHandle,
                                                                        BigInteger stateVersion,
                                                                        Class<T> type) {
        return createContextState(handle, descrHandle, stateVersion, BigInteger.ZERO, type);
    }

    public static <T extends AbstractContextState> T createContextState(String handle,
                                                                        String descrHandle,
                                                                        BigInteger stateVersion,
                                                                        BigInteger descriptorVersion,
                                                                        Class<T> type) {
        try {
            T instance = type.getDeclaredConstructor().newInstance();
            instance.setContextAssociation(ContextAssociation.DIS);
            instance.setHandle(handle);
            instance.setDescriptorHandle(descrHandle);
            instance.setStateVersion(stateVersion);
            instance.setDescriptorVersion(descriptorVersion);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
