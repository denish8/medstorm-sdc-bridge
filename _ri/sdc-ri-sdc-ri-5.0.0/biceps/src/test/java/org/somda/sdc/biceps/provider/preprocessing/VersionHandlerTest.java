package org.somda.sdc.biceps.provider.preprocessing;

import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.UnitTestUtil;
import org.somda.sdc.biceps.common.*;
import org.somda.sdc.biceps.common.storage.factory.MdibStorageFactory;
import org.somda.sdc.biceps.common.storage.MdibStorage;
import org.somda.sdc.biceps.guice.DefaultBicepsConfigModule;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.testutil.MockModelFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.org.somda.common.LoggingTestWatcher;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@ExtendWith(LoggingTestWatcher.class)
class VersionHandlerTest {
    private static final UnitTestUtil UT = new UnitTestUtil(new DefaultBicepsConfigModule() {
        @Override
        protected void customConfigure() {
            // Configure to avoid copying and make comparison easier
            bind(CommonConfig.COPY_MDIB_OUTPUT,
                    Boolean.class,
                    false);
        }
    });

    private MdibStorage mdibStorage;
    private VersionHandler versionHandler;
    private String mdsHandle;
    private String vmdHandle;
    private MdsDescriptor mdsDescriptor;
    private MdsState mdsState;
    private VmdDescriptor vmdDescriptor;
    private VmdState vmdState;
    private String systemContextHandle;
    private SystemContextDescriptor systemContextDescriptor;
    private SystemContextState systemContextState;
    private String patientContextHandle;
    private PatientContextDescriptor patientContextDescriptor;
    private PatientContextState patientContextState1;
    private PatientContextState patientContextState2;
    private String patientContextStateHandle1;
    private String patientContextStateHandle2;

    @BeforeEach
    void beforeEach() throws Exception {
        // Given a version handler and sample input
        mdibStorage = UT.getInjector().getInstance(MdibStorageFactory.class).createMdibStorage();
        versionHandler = UT.getInjector().getInstance(VersionHandler.class);

        mdsHandle = "mds";
        mdsDescriptor = MockModelFactory.createDescriptor(mdsHandle, BigInteger.valueOf(-1), MdsDescriptor.class);
        mdsState = MockModelFactory.createState(mdsHandle, BigInteger.valueOf(-1), BigInteger.valueOf(-1), MdsState.class);

        vmdHandle = "vmd";
        vmdDescriptor = MockModelFactory.createDescriptor(vmdHandle, BigInteger.valueOf(-1), VmdDescriptor.class);
        vmdState = MockModelFactory.createState(vmdHandle, BigInteger.valueOf(-1), BigInteger.valueOf(-1), VmdState.class);

        systemContextHandle = "systemcontext";
        systemContextDescriptor = MockModelFactory.createDescriptor(systemContextHandle, BigInteger.valueOf(-1),
                SystemContextDescriptor.class);
        systemContextState = MockModelFactory.createState(systemContextHandle, BigInteger.valueOf(-1), BigInteger.valueOf(-1),
                SystemContextState.class);

        patientContextHandle = "patientcontext";
        patientContextStateHandle1 = "patientcontextstate1";
        patientContextStateHandle2 = "patientcontextstate2";
        patientContextHandle = "patientcontext";
        patientContextDescriptor = MockModelFactory.createDescriptor(patientContextHandle, BigInteger.valueOf(-1),
                PatientContextDescriptor.class);
        patientContextState1 = MockModelFactory.createContextState(patientContextStateHandle1, patientContextHandle,
                BigInteger.valueOf(-1), BigInteger.valueOf(-1), PatientContextState.class);
        patientContextState2 = MockModelFactory.createContextState(patientContextStateHandle2, patientContextHandle,
                BigInteger.valueOf(-1), BigInteger.valueOf(-1), PatientContextState.class);
    }

    @Test
    void deleteEntity() throws VersioningException {
        final BigInteger expectedDescriptorVersion = mdsDescriptor.getDescriptorVersion();
        final MdibDescriptionModifications modifications = MdibDescriptionModifications.create();
        modifications.delete(mdsDescriptor);

        // When a deletion comes up
        apply(modifications);

        // Then expect nothing to be incremented
        assertEquals(expectedDescriptorVersion, mdsDescriptor.getDescriptorVersion());
    }

    @Test
    void insertEntity() throws Exception {
        {
            // When an insert comes up with no missing state
            final MdibDescriptionModifications modifications = MdibDescriptionModifications.create();
            modifications.insert(mdsDescriptor);

            // Then expect a runtime exception to be thrown
            assertThrows(Exception.class, () -> apply(modifications));
        }

        {
            // When a valid insert comes up
            final MdibDescriptionModifications modifications = MdibDescriptionModifications.create();
            modifications.insert(mdsDescriptor, mdsState);
            apply(modifications);

            // Then expect versions to be set to initial value
            assertEquals(BigInteger.ZERO, mdsDescriptor.getDescriptorVersion());
            assertEquals(BigInteger.ZERO, mdsState.getDescriptorVersion());
            assertEquals(BigInteger.ZERO, mdsState.getStateVersion());
        }
    }

    @Test
    void updateEntity() throws VersioningException {
        // Given a version handler
        final MdibDescriptionModifications modifications = MdibDescriptionModifications.create();
        modifications.update(mdsDescriptor, mdsState);

        {
            // When an update comes up with no insertion in advance
            // Then expect a runtime exception to be thrown
            assertThrows(Exception.class, () -> apply(modifications));
        }

        {
            // When a valid update comes up (element was inserted before using the version handler)
            final MdibDescriptionModifications insertModifications = MdibDescriptionModifications.create();
            insertModifications.insert(mdsDescriptor, mdsState);
            apply(insertModifications);

            assertEquals(BigInteger.ZERO, mdsDescriptor.getDescriptorVersion());
            assertEquals(BigInteger.ZERO, mdsState.getDescriptorVersion());
            apply(modifications);

            // Then expect versions to be incremented accordingly
            assertEquals(BigInteger.ONE, mdsDescriptor.getDescriptorVersion());
            assertEquals(BigInteger.ONE, mdsState.getDescriptorVersion());
        }

        {
            // When a another valid update comes up with the state being set in the MDIB storage but not in the
            // modification set
            final MdibDescriptionModifications updateModifications = MdibDescriptionModifications.create();
            updateModifications.update(mdsDescriptor);
            apply(modifications);

            // Then expect versions to be incremented accordingly
            assertEquals(BigInteger.TWO, mdsDescriptor.getDescriptorVersion());
            assertEquals(BigInteger.TWO, mdsState.getDescriptorVersion());
        }
    }

    @Test
    void childInsertionWithParentInMdibStorage() throws VersioningException {
        // Given a pre-inserted MDS
        final MdibDescriptionModifications mdsModifications = MdibDescriptionModifications.create();
        mdsModifications.insert(mdsDescriptor, mdsState);
        apply(mdsModifications);
        assertEquals(BigInteger.ZERO, mdsDescriptor.getDescriptorVersion());
        assertEquals(BigInteger.ZERO, mdsState.getDescriptorVersion());
        assertEquals(BigInteger.ZERO, mdsState.getStateVersion());

        // When a child is inserted below the MDS
        final MdibDescriptionModifications vmdModifications = MdibDescriptionModifications.create();
        vmdModifications.insert(vmdDescriptor, vmdState, mdsHandle);
        apply(vmdModifications);

        // Then expect the MDS version to be incremented
        assertEquals(BigInteger.ONE, mdsDescriptor.getDescriptorVersion());
        assertEquals(BigInteger.ONE, mdsState.getDescriptorVersion());
        assertEquals(BigInteger.ONE, mdsState.getStateVersion());
    }

    @Test
    void childInsertionWithMissingParent() {
        // When a child is inserted below a non-existing MDS
        final MdibDescriptionModifications vmdModifications = MdibDescriptionModifications.create();
        vmdModifications.insert(vmdDescriptor, vmdState, mdsHandle);
        versionHandler.beforeFirstModification(vmdModifications, mdibStorage);

        // Then expect an exception to be thrown
        assertThrows(Exception.class, () ->
                versionHandler.process(vmdModifications, vmdModifications.getModifications().get(0), mdibStorage));
    }

    @Test
    void childInsertionWithParentInModifications() throws VersioningException {
        // Given an MDS starting at a defined version to be inserted together with a child
        final MdibDescriptionModifications mdsModifications = MdibDescriptionModifications.create();
        mdsModifications.insert(mdsDescriptor, mdsState);
        apply(mdsModifications, false);

        {
            // When an MDS and a connected VMD are inserted
            final MdibDescriptionModifications vmdModifications = MdibDescriptionModifications.create();
            vmdModifications.insert(mdsDescriptor, mdsState);
            vmdModifications.insert(vmdDescriptor, vmdState, mdsHandle);

            apply(vmdModifications, false);

            // Then expect the MDS version to be incremented
            assertEquals(BigInteger.ONE, mdsDescriptor.getDescriptorVersion());
            assertEquals(BigInteger.ONE, mdsState.getDescriptorVersion());
            assertEquals(BigInteger.ONE, mdsState.getStateVersion());
        }

        {
            // When an MDS and a connected VMD are inserted (reverse order)
            final MdibDescriptionModifications vmdModifications = MdibDescriptionModifications.create();
            vmdModifications.insert(vmdDescriptor, vmdState, mdsHandle);
            vmdModifications.insert(mdsDescriptor, mdsState);

            // Then expect an exception to be thrown
            assertThrows(Exception.class, () -> apply(vmdModifications));
        }
    }

    @Test
    void childDeletionWithParentInMdibStorage() throws VersioningException {
        // Given a pre-inserted MDS and VMD
        final MdibDescriptionModifications modifications = MdibDescriptionModifications.create();
        modifications.insert(mdsDescriptor, mdsState);
        modifications.insert(vmdDescriptor, vmdState, mdsHandle);
        apply(modifications);

        // When the VMD child is deleted below the MDS
        final MdibDescriptionModifications vmdModifications = MdibDescriptionModifications.create();
        vmdModifications.delete(vmdDescriptor);
        apply(vmdModifications);

        // Then expect the MDS version to be incremented
        assertEquals(BigInteger.ONE, mdsDescriptor.getDescriptorVersion());
        assertEquals(BigInteger.ONE, mdsState.getDescriptorVersion());
        assertEquals(BigInteger.ONE, mdsState.getStateVersion());
    }

    @Test
    void childDeletionOfNonExistingEntity() {
        // When a child is deleted below a non-existing MDS
        final MdibDescriptionModifications vmdModifications = MdibDescriptionModifications.create();
        vmdModifications.delete(vmdDescriptor);
        // Then expect an exception to be thrown
        assertThrows(Exception.class, () -> apply(vmdModifications));
    }

    @Test
    void childDeletionWithParentInModifications() throws VersioningException {
        // Given an MDS starting at a defined version to be updated together with a deleted child
        final MdibDescriptionModifications modifications = MdibDescriptionModifications.create();
        modifications.insert(mdsDescriptor, mdsState);
        modifications.insert(vmdDescriptor, vmdState, mdsHandle);
        apply(modifications);

        {
            // When an MDS is updated and the connected VMD is deleted
            final MdibDescriptionModifications vmdModifications = MdibDescriptionModifications.create();
            vmdModifications.update(mdsDescriptor, mdsState);
            vmdModifications.delete(vmdDescriptor);

            apply(vmdModifications, false);

            // Then expect the MDS version to be incremented
            assertEquals(BigInteger.ONE, mdsDescriptor.getDescriptorVersion());
            assertEquals(BigInteger.ONE, mdsState.getDescriptorVersion());
            assertEquals(BigInteger.ONE, mdsState.getStateVersion());
        }

        {
            // When an MDS is updated and the connected VMD is deleted (reverse order)
            final MdibDescriptionModifications vmdModifications = MdibDescriptionModifications.create();
            vmdModifications.delete(vmdDescriptor);
            vmdModifications.update(mdsDescriptor, mdsState);

            apply(vmdModifications, false);

            // Then expect the MDS version to be incremented
            assertEquals(BigInteger.TWO, mdsDescriptor.getDescriptorVersion());
            assertEquals(BigInteger.TWO, mdsState.getDescriptorVersion());
            assertEquals(BigInteger.TWO, mdsState.getStateVersion());
        }
    }

    @Test
    void insertionAndUpdateOfMultiStates() throws VersioningException {
        {
            // When an insert comes up without states
            final MdibDescriptionModifications modifications = MdibDescriptionModifications.create();
            modifications.insert(mdsDescriptor, mdsState);
            modifications.insert(systemContextDescriptor, systemContextState, mdsHandle);
            modifications.insert(patientContextDescriptor, systemContextHandle);

            apply(modifications, false);

            // Then expect the versioning to succeed
            assertEquals(BigInteger.ZERO, patientContextDescriptor.getDescriptorVersion());
        }

        {
            // When an insert comes up with states
            final MdibDescriptionModifications modifications = MdibDescriptionModifications.create();
            modifications.insert(mdsDescriptor, mdsState);
            modifications.insert(systemContextDescriptor, systemContextState, mdsHandle);
            modifications.insert(patientContextDescriptor, List.of(patientContextState1, patientContextState2), systemContextHandle);

            apply(modifications, false);

            // Then expect the versioning to succeed
            assertEquals(BigInteger.ONE, patientContextDescriptor.getDescriptorVersion());
            assertEquals(BigInteger.ONE, patientContextState1.getDescriptorVersion());
            assertEquals(BigInteger.ONE, patientContextState2.getDescriptorVersion());
            assertEquals(BigInteger.ZERO, patientContextState1.getStateVersion());
            assertEquals(BigInteger.ZERO, patientContextState2.getStateVersion());
        }

        {
            // When an update comes up
            final MdibDescriptionModifications modifications = MdibDescriptionModifications.create();
            modifications.insert(mdsDescriptor, mdsState);
            modifications.insert(systemContextDescriptor, systemContextState, mdsHandle);
            modifications.update(patientContextDescriptor, List.of(patientContextState1, patientContextState2));

            apply(modifications, false);

            // Then expect the versioning to succeed
            assertEquals(BigInteger.TWO, patientContextDescriptor.getDescriptorVersion());
            assertEquals(BigInteger.TWO, patientContextState1.getDescriptorVersion());
            assertEquals(BigInteger.TWO, patientContextState2.getDescriptorVersion());
            assertEquals(BigInteger.ONE, patientContextState1.getStateVersion());
            assertEquals(BigInteger.ONE, patientContextState2.getStateVersion());
        }
    }

    @Test
    void insertionUpdateOfMultiStatesWithASubsetOfStates() throws VersioningException {
        // Given a pre-inserted PatientContextDescriptor with two states
        MdibDescriptionModifications modifications = MdibDescriptionModifications.create();
        modifications.insert(mdsDescriptor, mdsState);
        modifications.insert(systemContextDescriptor, systemContextState, mdsHandle);
        modifications.insert(patientContextDescriptor, List.of(patientContextState1, patientContextState2),  systemContextHandle);
        apply(modifications);
        assertEquals(BigInteger.ZERO, patientContextDescriptor.getDescriptorVersion());
        assertEquals(BigInteger.ZERO, patientContextState1.getStateVersion());
        assertEquals(BigInteger.ZERO, patientContextState1.getDescriptorVersion());
        assertEquals(BigInteger.ZERO, patientContextState2.getStateVersion());
        assertEquals(BigInteger.ZERO, patientContextState2.getDescriptorVersion());

        // When an update for a MultiState comes up that affects only a subset of the states
        modifications = MdibDescriptionModifications.create();
        modifications.update(patientContextDescriptor, List.of(patientContextState1));
        apply(modifications);
        assertEquals(BigInteger.ONE, patientContextDescriptor.getDescriptorVersion());
        assertEquals(BigInteger.ONE, patientContextState1.getStateVersion());
        assertEquals(BigInteger.ONE, patientContextState1.getDescriptorVersion());

        // Then all non-affected states must also point to the new descriptor version and increase their state version
        assertEquals(BigInteger.ONE, patientContextState2.getStateVersion());
        assertEquals(BigInteger.ONE, patientContextState2.getDescriptorVersion());
    }

    @Test
    void stateModifications() throws VersioningException {
        final MdibDescriptionModifications descriptionModifications = MdibDescriptionModifications.create();
        descriptionModifications.insert(mdsDescriptor, mdsState);
        descriptionModifications.insert(systemContextDescriptor, systemContextState, mdsHandle);
        descriptionModifications.insert(patientContextDescriptor, patientContextState1, systemContextHandle);
        apply(descriptionModifications, false);

        {
            final MdibStateModifications modifications = MdibStateModifications.create(MdibStateModifications.Type.COMPONENT);
            modifications.add(mdsState);

            apply(modifications);

            assertEquals(BigInteger.ZERO, mdsState.getDescriptorVersion());
            assertEquals(BigInteger.ONE, mdsState.getStateVersion());
        }

        {
            final MdibStateModifications modifications = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT);
            modifications.add(patientContextState1);
            modifications.add(patientContextState2);

            apply(modifications);

            assertEquals(BigInteger.ZERO, patientContextState1.getDescriptorVersion());
            assertEquals(BigInteger.ONE, patientContextState1.getStateVersion());
            assertEquals(BigInteger.ZERO, patientContextState2.getDescriptorVersion());
            assertEquals(BigInteger.ZERO, patientContextState2.getStateVersion());
        }
    }

    private void apply(MdibStateModifications modifications) throws VersioningException {
        versionHandler.beforeFirstModification(modifications, mdibStorage);
        for (AbstractState modification : modifications.getStates()) {
            versionHandler.process(modifications, modification, mdibStorage);
        }
        versionHandler.afterLastModification(modifications, mdibStorage);
    }

    private void apply(MdibDescriptionModifications modifications, boolean applyOnStorage) throws VersioningException {
        versionHandler.beforeFirstModification(modifications, mdibStorage);
        for (MdibDescriptionModification modification : modifications.getModifications()) {
            versionHandler.process(modifications, modification, mdibStorage);
        }
        versionHandler.afterLastModification(modifications, mdibStorage);

        if (applyOnStorage) {
            mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), mock(BigInteger.class), modifications);
        }
    }

    private void apply(MdibDescriptionModifications modifications) throws VersioningException {
        apply(modifications, true);
    }
}