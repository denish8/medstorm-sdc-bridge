package org.somda.sdc.biceps.consumer.preprocessing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.somda.sdc.biceps.UnitTestUtil;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.storage.MdibStorage;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.LocationContextState;
import org.somda.sdc.biceps.model.participant.PatientContextState;
import org.somda.sdc.biceps.testutil.MockModelFactory;
import test.org.somda.common.LoggingTestWatcher;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@ExtendWith(LoggingTestWatcher.class)
class DuplicateContextStateHandleHandlerTest {

    private static final UnitTestUtil UT = new UnitTestUtil();

    private MdibStorage mdibStorage;
    private DuplicateContextStateHandleHandler duplicateContextStateHandleHandler;

    private String contextStateHandle;
    private String patientContextDescriptorHandle;
    private String patientContextDescriptor2Handle;
    private String locationContextDescriptorHandle;

    private PatientContextState initialPatientContextState;
    private PatientContextState patientContextStateGoodModification;
    private PatientContextState patientContextStateBadModification;

    private LocationContextState locationContextStateBadInitial;
    private LocationContextState locationContextStateBadModification;

    @BeforeEach
    void beforeEach() {
        mdibStorage = mock(MdibStorage.class);
        duplicateContextStateHandleHandler = UT.getInjector().getInstance(DuplicateContextStateHandleHandler.class);

        contextStateHandle = "contextState";
        patientContextDescriptorHandle = "patientContextDescriptor1";
        patientContextDescriptor2Handle = "patientContextDescriptor2";
        locationContextDescriptorHandle = "locationContextDescriptor";

        initialPatientContextState = MockModelFactory.createContextState(contextStateHandle,
                patientContextDescriptorHandle, PatientContextState.class);
        patientContextStateGoodModification = MockModelFactory.createContextState(contextStateHandle,
                patientContextDescriptorHandle, PatientContextState.class);
        patientContextStateBadModification = MockModelFactory.createContextState(contextStateHandle,
                patientContextDescriptor2Handle, PatientContextState.class);

        // not really bad, but with the same state handle as the patientContextState
        locationContextStateBadInitial = MockModelFactory.createContextState(contextStateHandle,
                locationContextDescriptorHandle, LocationContextState.class);
        locationContextStateBadModification = MockModelFactory.createContextState(contextStateHandle,
                locationContextDescriptorHandle, LocationContextState.class);
    }

    @Test
    void noDuplicateHandles() throws DuplicateContextStateHandleException {
        final MdibStateModifications modifications = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT);
        modifications.add(patientContextStateGoodModification);

        List<AbstractState> expectedState = List.of(patientContextStateGoodModification);

        Mockito.when(mdibStorage.getState(contextStateHandle)).thenReturn(Optional.of(initialPatientContextState));

        apply(modifications);

        assertEquals(expectedState, modifications.getStates());
    }

    @Test
    void duplicateHandlesInModification() {
        final MdibStateModifications modifications = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT);
        modifications.add(patientContextStateGoodModification);
        modifications.add(patientContextStateBadModification);

        Mockito.when(mdibStorage.getState(contextStateHandle)).thenReturn(Optional.of(initialPatientContextState));

        assertThrows(DuplicateContextStateHandleException.class, () -> apply(modifications));
    }

    @Test
    void duplicateHandlesInStorage() {
        final MdibStateModifications modifications = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT);
        modifications.add(patientContextStateGoodModification);

        Mockito.when(mdibStorage.getStatesByType(any()))
                .thenReturn(List.of(initialPatientContextState, locationContextStateBadInitial));

        assertThrows(DuplicateContextStateHandleException.class, () -> apply(modifications));
    }

    @Test
    void differentContextStatesWithSameHandle() {
        final MdibStateModifications modifications = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT);
        modifications.add(patientContextStateGoodModification);
        modifications.add(locationContextStateBadModification);

        Mockito.when(mdibStorage.getState(contextStateHandle)).thenReturn(Optional.of(initialPatientContextState));

        assertThrows(DuplicateContextStateHandleException.class, () -> apply(modifications));
    }

    private void apply(MdibStateModifications modifications) throws DuplicateContextStateHandleException {
        duplicateContextStateHandleHandler.beforeFirstModification(modifications, mdibStorage);
        for (var state : modifications.getStates()) {
            duplicateContextStateHandleHandler.process(modifications, state, mdibStorage);
        }
        duplicateContextStateHandleHandler.afterLastModification(modifications, mdibStorage);
    }
}
