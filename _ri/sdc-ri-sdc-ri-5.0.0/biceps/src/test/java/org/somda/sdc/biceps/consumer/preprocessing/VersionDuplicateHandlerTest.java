package org.somda.sdc.biceps.consumer.preprocessing;

import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.UnitTestUtil;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.storage.MdibStorage;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.testutil.MockModelFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import test.org.somda.common.LoggingTestWatcher;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith(LoggingTestWatcher.class)
class VersionDuplicateHandlerTest {
    private static final UnitTestUtil UT = new UnitTestUtil();

    private static final int STATE_VERSION_COUNT = 5;

    private MdibStorage mdibStorage;
    private VersionDuplicateHandler versionHandler;
    private String mdsHandle;
    private String vmdHandle;
    private String patientContextHandle;
    private String locationContextHandle;

    private MdsState[] mdsStates;
    private VmdState[] vmdStates;
    private PatientContextState[] patientContextStates;
    private LocationContextState[] locationContextStates;

    @BeforeEach
    void beforeEach() {
        // Given a version duplicate handler and sample input
        mdibStorage = mock(MdibStorage.class);
        versionHandler = UT.getInjector().getInstance(VersionDuplicateHandler.class);

        mdsHandle = "mds";
        vmdHandle = "vmd";
        patientContextHandle = "patientContextState";
        locationContextHandle = "locationContextState";

        mdsStates = initVersions(version -> MockModelFactory.createState(mdsHandle, version, MdsState.class),
                MdsState.class, STATE_VERSION_COUNT);
        vmdStates = initVersions(version -> MockModelFactory.createState(vmdHandle, version, VmdState.class),
                VmdState.class, STATE_VERSION_COUNT);

        // explicitly set one state version to null, to trigger implied value handling
        mdsStates[0].setStateVersion(null);

        patientContextStates = initVersions(version -> MockModelFactory.createContextState(patientContextHandle,
                "parent", version, PatientContextState.class), PatientContextState.class, STATE_VERSION_COUNT);
        locationContextStates = initVersions(version -> MockModelFactory.createContextState(locationContextHandle,
                "parent", LocationContextState.class), LocationContextState.class, STATE_VERSION_COUNT);
    }

    @Test
    void noVersionsSeen() {
        {
            // When versions of single states were not seen before
            final MdibStateModifications modifications = MdibStateModifications.create(MdibStateModifications.Type.COMPONENT);
            modifications.add(mdsStates[1]);
            modifications.add(vmdStates[1]);
            List<AbstractState> expectedStates = new ArrayList<>(modifications.getStates());

            Mockito.when(mdibStorage.getState(mdsHandle)).thenReturn(Optional.of(mdsStates[0]));
            Mockito.when(mdibStorage.getState(vmdHandle)).thenReturn(Optional.of(vmdStates[0]));

            apply(modifications);

            // Then expect every state to pass
            assertEquals(expectedStates, modifications.getStates());
        }

        {
            // When versions of multi states were not seen before
            final MdibStateModifications modifications = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT);
            modifications.add(patientContextStates[1]);
            modifications.add(locationContextStates[1]);
            List<AbstractState> expectedStates = new ArrayList<>(modifications.getStates());

            Mockito.when(mdibStorage.getState(mdsHandle)).thenReturn(Optional.of(patientContextStates[0]));
            Mockito.when(mdibStorage.getState(vmdHandle)).thenReturn(Optional.of(locationContextStates[0]));

            apply(modifications);

            // Then expect every state to pass
            assertEquals(expectedStates, modifications.getStates());
        }
    }


    @Test
    void stateVersionSeen() {
        {
            // When versions of single states were seen before (same version)
            final MdibStateModifications modifications = MdibStateModifications.create(MdibStateModifications.Type.COMPONENT);
            modifications.add(mdsStates[1]);
            List<AbstractState> expectedStates = new ArrayList<>(modifications.getStates());
            modifications.add(vmdStates[1]);

            Mockito.when(mdibStorage.getState(mdsHandle)).thenReturn(Optional.of(mdsStates[0]));
            Mockito.when(mdibStorage.getState(vmdHandle)).thenReturn(Optional.of(vmdStates[1]));

            apply(modifications);

            // Then expect states seen with same version to be omitted
            assertEquals(expectedStates, modifications.getStates());
        }

        {
            // When versions of single states were seen before (greater version)
            final MdibStateModifications modifications = MdibStateModifications.create(MdibStateModifications.Type.COMPONENT);
            modifications.add(mdsStates[1]);
            List<AbstractState> expectedStates = new ArrayList<>(modifications.getStates());
            modifications.add(vmdStates[1]);

            Mockito.when(mdibStorage.getState(mdsHandle)).thenReturn(Optional.of(mdsStates[0]));
            Mockito.when(mdibStorage.getState(vmdHandle)).thenReturn(Optional.of(vmdStates[2]));

            apply(modifications);

            // Then expect states seen with greater version to be omitted
            assertEquals(expectedStates, modifications.getStates());
        }
    }

    @Test
    void multiStateVersionSeen() {
        {
            // When versions of multi states were seen before (same version)
            final MdibStateModifications modifications = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT);
            modifications.add(patientContextStates[1]);
            List<AbstractState> expectedStates = new ArrayList<>(modifications.getStates());
            modifications.add(locationContextStates[1]);

            Mockito.when(mdibStorage.getState(patientContextHandle)).thenReturn(Optional.of(patientContextStates[0]));
            Mockito.when(mdibStorage.getState(locationContextHandle)).thenReturn(Optional.of(locationContextStates[1]));

            apply(modifications);

            // Then expect states seen with same version to be omitted
            assertEquals(expectedStates, modifications.getStates());
        }

        {
            // When versions of multi states were seen before (greater version)
            final MdibStateModifications modifications = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT);
            modifications.add(patientContextStates[1]);
            List<AbstractState> expectedStates = new ArrayList<>(modifications.getStates());
            modifications.add(locationContextStates[1]);

            Mockito.when(mdibStorage.getState(patientContextHandle)).thenReturn(Optional.of(patientContextStates[0]));
            Mockito.when(mdibStorage.getState(locationContextHandle)).thenReturn(Optional.of(locationContextStates[2]));

            apply(modifications);

            // Then expect states seen with greater version to be omitted
            assertEquals(expectedStates, modifications.getStates());
        }
    }

    @Test
    void noStateInMdibStorage() {
        {
            // When single state is not found in MDIB storage
            final MdibStateModifications modifications = MdibStateModifications.create(MdibStateModifications.Type.COMPONENT);
            modifications.add(mdsStates[1]);
            modifications.add(vmdStates[1]);
            List<AbstractState> expectedStates = new ArrayList<>(modifications.getStates());

            Mockito.when(mdibStorage.getState(mdsHandle)).thenReturn(Optional.of(mdsStates[0]));
            Mockito.when(mdibStorage.getState(vmdHandle)).thenReturn(Optional.empty());

            apply(modifications);

            // Then expect the state to pass through
            assertEquals(expectedStates, modifications.getStates());
        }

        {
            // When multi state is not found in MDIB storage
            final MdibStateModifications modifications = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT);
            modifications.add(patientContextStates[1]);
            modifications.add(locationContextStates[1]);
            List<AbstractState> expectedStates = new ArrayList<>(modifications.getStates());

            Mockito.when(mdibStorage.getState(mdsHandle)).thenReturn(Optional.of(patientContextStates[0]));
            Mockito.when(mdibStorage.getState(vmdHandle)).thenReturn(Optional.empty());

            apply(modifications);

            // Then expect the state to pass through
            assertEquals(expectedStates, modifications.getStates());
        }
    }

    private void apply(MdibStateModifications modifications) {
        versionHandler.beforeFirstModification(modifications, mdibStorage);
        for (AbstractState state : modifications.getStates()) {
            versionHandler.process(modifications, state, mdibStorage);
        }
        versionHandler.afterLastModification(modifications, mdibStorage);
    }

    @SuppressWarnings("unchecked")
    private <T extends AbstractState> T[] initVersions(Function<BigInteger, T> createFnc, Class<T> theClass, int count) {
        final T[] states = (T[]) Array.newInstance(theClass, count);
        for (int i = 0; i < count; ++i) {
            states[i] = createFnc.apply(BigInteger.valueOf(i));
        }
        return states;
    }
}