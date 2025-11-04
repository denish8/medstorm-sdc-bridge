package org.somda.sdc.biceps.common.storage;

import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.UnitTestUtil;
import org.somda.sdc.biceps.common.CommonConfig;
import org.somda.sdc.biceps.common.MdibDescriptionModification;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.preprocessing.DescriptorChildRemover;
import org.somda.sdc.biceps.common.storage.factory.MdibStorageFactory;
import org.somda.sdc.biceps.consumer.preprocessing.DuplicateContextStateHandleHandler;
import org.somda.sdc.biceps.consumer.preprocessing.VersionDuplicateHandler;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.AlertConditionDescriptor;
import org.somda.sdc.biceps.model.participant.AlertConditionState;
import org.somda.sdc.biceps.model.participant.AlertSystemDescriptor;
import org.somda.sdc.biceps.model.participant.AlertSystemState;
import org.somda.sdc.biceps.model.participant.ChannelDescriptor;
import org.somda.sdc.biceps.model.participant.ChannelState;
import org.somda.sdc.biceps.model.participant.ContextAssociation;
import org.somda.sdc.biceps.model.participant.EnsembleContextDescriptor;
import org.somda.sdc.biceps.model.participant.EnsembleContextState;
import org.somda.sdc.biceps.model.participant.LocationContextDescriptor;
import org.somda.sdc.biceps.model.participant.LocationContextState;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.biceps.model.participant.MdsDescriptor;
import org.somda.sdc.biceps.model.participant.MdsState;
import org.somda.sdc.biceps.model.participant.PatientContextDescriptor;
import org.somda.sdc.biceps.model.participant.PatientContextState;
import org.somda.sdc.biceps.model.participant.SystemContextDescriptor;
import org.somda.sdc.biceps.model.participant.SystemContextState;
import org.somda.sdc.biceps.model.participant.VmdDescriptor;
import org.somda.sdc.biceps.model.participant.VmdState;
import org.somda.sdc.biceps.provider.preprocessing.DuplicateChecker;
import org.somda.sdc.biceps.provider.preprocessing.HandleReferenceHandler;
import org.somda.sdc.biceps.provider.preprocessing.TypeConsistencyChecker;
import org.somda.sdc.biceps.provider.preprocessing.VersionHandler;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.biceps.testutil.MockModelFactory;
import org.somda.sdc.common.guice.AbstractConfigurationModule;
import test.org.somda.common.LoggingTestWatcher;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(LoggingTestWatcher.class)
class MdibStorageImplTest {
    private static final UnitTestUtil UT = new UnitTestUtil();

    private MdibStorage mdibStorage;

    @BeforeEach
    void setUp() {
        Injector injector = UT.getInjector();
        mdibStorage = injector.getInstance(MdibStorageFactory.class).createMdibStorage();
    }

    private void applyDescriptionWithVersion(MdibDescriptionModification.Type type,
                                             BigInteger version) {
        applyDescriptionWithVersion(mdibStorage, type, version);
    }

    private void applyDescriptionWithVersion(MdibStorage storage,
                                             MdibDescriptionModification.Type type,
                                             BigInteger version) {
        final MdibDescriptionModifications modifications = MdibDescriptionModifications.create();
        modifications.add(type,
                MockModelFactory.createDescriptor(Handles.MDS_0, version, MdsDescriptor.class),
                MockModelFactory.createState(Handles.MDS_0, version, MdsState.class));
        modifications.add(type,
                MockModelFactory.createDescriptor(Handles.MDS_1, version, MdsDescriptor.class),
                MockModelFactory.createState(Handles.MDS_1, version, MdsState.class));
        modifications.add(type,
                MockModelFactory.createDescriptor(Handles.SYSTEMCONTEXT_0, version, MdsDescriptor.class),
                MockModelFactory.createState(Handles.SYSTEMCONTEXT_0, version, SystemContextState.class),
                Handles.MDS_0);
        modifications.add(type,
                MockModelFactory.createDescriptor(Handles.SYSTEMCONTEXT_1, version, MdsDescriptor.class),
                MockModelFactory.createState(Handles.SYSTEMCONTEXT_1, version, SystemContextState.class),
                Handles.MDS_1);
        modifications.add(type,
                MockModelFactory.createDescriptor(Handles.VMD_0, version, VmdDescriptor.class),
                MockModelFactory.createState(Handles.VMD_0, version, VmdState.class),
                Handles.MDS_0);
        modifications.add(type,
                MockModelFactory.createDescriptor(Handles.VMD_1, version, VmdDescriptor.class),
                MockModelFactory.createState(Handles.VMD_1, version, VmdState.class),
                Handles.MDS_1);
        modifications.add(type,
                MockModelFactory.createDescriptor(Handles.CHANNEL_0, version, ChannelDescriptor.class),
                MockModelFactory.createState(Handles.CHANNEL_0, version, ChannelState.class),
                Handles.VMD_0);
        modifications.add(type,
                MockModelFactory.createDescriptor(Handles.CHANNEL_1, version, ChannelDescriptor.class),
                MockModelFactory.createState(Handles.CHANNEL_1, version, ChannelState.class),
                Handles.VMD_0);
        modifications.add(type,
                MockModelFactory.createDescriptor(Handles.CHANNEL_2, version, ChannelDescriptor.class),
                MockModelFactory.createState(Handles.CHANNEL_2, version, ChannelState.class),
                Handles.VMD_1);
        modifications.add(type,
                MockModelFactory.createDescriptor(Handles.CONTEXTDESCRIPTOR_0,
                        version,
                        PatientContextDescriptor.class),
                Arrays.asList(
                        MockModelFactory.createContextState(Handles.CONTEXT_0, Handles.CONTEXTDESCRIPTOR_0,
                                version, version, PatientContextState.class),
                        MockModelFactory.createContextState(Handles.CONTEXT_1, Handles.CONTEXTDESCRIPTOR_0,
                                version, version, PatientContextState.class)
                ),
                Handles.SYSTEMCONTEXT_0);

        modifications.add(type,
                MockModelFactory.createDescriptor(Handles.CONTEXTDESCRIPTOR_1,
                        version,
                        LocationContextDescriptor.class),
                Arrays.asList(
                        MockModelFactory.createContextState(Handles.CONTEXT_2, Handles.CONTEXTDESCRIPTOR_1,
                                version, version, LocationContextState.class)
                ),
                Handles.SYSTEMCONTEXT_0);

        storage.apply(mock(MdibVersion.class), mock(BigInteger.class), mock(BigInteger.class), modifications);
    }

    private void testWithVersion(List<String> testedHandles, BigInteger version) {
        testWithVersion(testedHandles, version, version);
    }

    private void testWithVersion(List<String> testedHandles, BigInteger descrVersion, BigInteger stateVersion) {
        testedHandles.forEach(handle -> {
            assertThat(mdibStorage.getEntity(handle).isPresent(), is(true));
            assertThat(mdibStorage.getEntity(handle).get().getDescriptor().getDescriptorVersion(), is(descrVersion));
            mdibStorage.getEntity(handle).get().getStates().forEach(state ->
                    assertThat(state.getStateVersion(), is(stateVersion)));
        });
    }

    @Test
    void writeDescription() {
        List<String> testedHandles = Arrays.asList(
                Handles.MDS_0,
                Handles.MDS_1,
                Handles.SYSTEMCONTEXT_0,
                Handles.SYSTEMCONTEXT_1,
                Handles.VMD_0,
                Handles.VMD_1,
                Handles.CHANNEL_0,
                Handles.CHANNEL_1,
                Handles.CHANNEL_2,
                Handles.CONTEXTDESCRIPTOR_0);

        applyDescriptionWithVersion(MdibDescriptionModification.Type.INSERT, BigInteger.ZERO);
        testWithVersion(testedHandles, BigInteger.ZERO);
        applyDescriptionWithVersion(MdibDescriptionModification.Type.UPDATE, BigInteger.ONE);
        testWithVersion(testedHandles, BigInteger.ONE);
        applyDescriptionWithVersion(MdibDescriptionModification.Type.UPDATE, BigInteger.TEN);
        testWithVersion(testedHandles, BigInteger.TEN);

        applyDescriptionWithVersion(MdibDescriptionModification.Type.DELETE, BigInteger.ZERO);
        for (String handle : testedHandles) {
            assertThat(mdibStorage.getEntity(handle).isPresent(), is(false));
        }
    }

    @Test
    void deleteChildUpdateParent() {
        // create content
        List<String> testedHandles = Arrays.asList(
                Handles.MDS_0,
                Handles.SYSTEMCONTEXT_0,
                Handles.VMD_0,
                Handles.CHANNEL_0,
                Handles.CHANNEL_1,
                Handles.CONTEXTDESCRIPTOR_0);

        applyDescriptionWithVersion(MdibDescriptionModification.Type.INSERT, BigInteger.ZERO);
        testWithVersion(testedHandles, BigInteger.ZERO);

        final var parentHandle = Handles.VMD_0;
        final var childHandle = Handles.CHANNEL_0;
        final var untouchedChildHandle = Handles.CHANNEL_1;

        assertTrue(mdibStorage.getEntity(parentHandle).get().getChildren().contains(childHandle));
        assertTrue(mdibStorage.getEntity(parentHandle).get().getChildren().contains(untouchedChildHandle));
        assertEquals(parentHandle, mdibStorage.getEntity(childHandle).get().getParent().get());


        // delete the child
        final MdibDescriptionModifications modifications = MdibDescriptionModifications.create();
        modifications.add(MdibDescriptionModification.Type.DELETE,
                MockModelFactory.createDescriptor(Handles.CHANNEL_0, BigInteger.ZERO, ChannelDescriptor.class),
                MockModelFactory.createState(Handles.CHANNEL_0, BigInteger.ZERO, ChannelState.class),
                Handles.VMD_0);
        var result = mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), mock(BigInteger.class), modifications);

        assertFalse(mdibStorage.getEntity(parentHandle).get().getChildren().contains(childHandle));
        assertTrue(mdibStorage.getEntity(parentHandle).get().getChildren().contains(untouchedChildHandle));
        assertEquals(1, result.getDeletedEntities().size());
        assertEquals(1, result.getUpdatedEntities().size());
    }

    @Test
    void mdibAccess() {
        applyDescriptionWithVersion(MdibDescriptionModification.Type.INSERT, BigInteger.ZERO);
        assertThat(mdibStorage.getEntity(Handles.UNKNOWN).isPresent(), is(false));
        assertThat(mdibStorage.getEntity(Handles.MDS_0).isPresent(), is(true));

        assertThat(mdibStorage.getDescriptor(Handles.UNKNOWN, MdsDescriptor.class).isPresent(), is(false));
        assertThat(mdibStorage.getDescriptor(Handles.VMD_0, MdsDescriptor.class).isPresent(), is(false));
        assertThat(mdibStorage.getDescriptor(Handles.VMD_0, VmdDescriptor.class).isPresent(), is(true));

        assertThat(mdibStorage.getContextStates(Handles.UNKNOWN, PatientContextState.class).isEmpty(), is(true));
        assertThat(mdibStorage.getContextStates(Handles.VMD_0, PatientContextState.class).isEmpty(), is(true));
        assertThat(mdibStorage.getContextStates(Handles.CONTEXTDESCRIPTOR_0, LocationContextState.class).isEmpty(), is(true));
        assertThat(mdibStorage.getContextStates(Handles.CONTEXTDESCRIPTOR_0, PatientContextState.class).size(), is(2));

        assertThat(mdibStorage.findContextStatesByType(PatientContextState.class).size(), is(2));
        assertThat(mdibStorage.findContextStatesByType(LocationContextState.class).size(), is(1));
    }

    @Test
    void writeStates() {
        List<String> testedHandles = Arrays.asList(
                Handles.ALERTSYSTEM_0,
                Handles.ALERTCONDITION_0,
                Handles.ALERTCONDITION_1);

        final MdibDescriptionModifications descriptionModifications = MdibDescriptionModifications.create();
        descriptionModifications.insert(
                MockModelFactory.createDescriptor(Handles.MDS_0, MdsDescriptor.class),
                MockModelFactory.createState(Handles.MDS_0, MdsState.class));
        descriptionModifications.insert(
                MockModelFactory.createDescriptor(Handles.ALERTSYSTEM_0, AlertSystemDescriptor.class),
                MockModelFactory.createState(Handles.ALERTSYSTEM_0, AlertSystemState.class),
                Handles.MDS_0);
        descriptionModifications.insert(
                MockModelFactory.createDescriptor(Handles.ALERTCONDITION_0, AlertConditionDescriptor.class),
                MockModelFactory.createState(Handles.ALERTCONDITION_0, AlertConditionState.class),
                Handles.ALERTSYSTEM_0);
        descriptionModifications.insert(
                MockModelFactory.createDescriptor(Handles.ALERTCONDITION_1, AlertConditionDescriptor.class),
                MockModelFactory.createState(Handles.ALERTCONDITION_1, AlertConditionState.class),
                Handles.ALERTSYSTEM_0);
        descriptionModifications.insert(
                MockModelFactory.createDescriptor(Handles.SYSTEMCONTEXT_0, SystemContextDescriptor.class),
                MockModelFactory.createState(Handles.SYSTEMCONTEXT_0, SystemContextState.class),
                Handles.MDS_0
            );
        descriptionModifications.insert(
                MockModelFactory.createDescriptor(Handles.CONTEXTDESCRIPTOR_0, PatientContextDescriptor.class),
                Arrays.asList(
                        MockModelFactory.createContextState(Handles.CONTEXT_0, Handles.CONTEXTDESCRIPTOR_0, PatientContextState.class),
                        MockModelFactory.createContextState(Handles.CONTEXT_1, Handles.CONTEXTDESCRIPTOR_0, PatientContextState.class)
                ),
                Handles.SYSTEMCONTEXT_0
        );
        descriptionModifications.insert(
                MockModelFactory.createDescriptor(Handles.CONTEXTDESCRIPTOR_1, LocationContextDescriptor.class),
                Handles.SYSTEMCONTEXT_0
        );

        mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), mock(BigInteger.class), descriptionModifications);

        testWithVersion(testedHandles, BigInteger.ZERO);

        MdibStateModifications stateModifications = MdibStateModifications.create(MdibStateModifications.Type.ALERT);
        stateModifications.add(MockModelFactory.createState(Handles.ALERTSYSTEM_0, BigInteger.ONE, AlertSystemState.class));
        stateModifications.add(MockModelFactory.createState(Handles.ALERTCONDITION_0, BigInteger.ONE, AlertConditionState.class));
        stateModifications.add(MockModelFactory.createState(Handles.ALERTCONDITION_1, BigInteger.ONE, AlertConditionState.class));
        try {
            stateModifications.add(MockModelFactory.createState(Handles.MDS_0, BigInteger.ONE, MdsState.class));
            Assertions.fail("Could add MDS to alert state change set");
        } catch (Exception ignored) {
        }

        mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), stateModifications);
        testWithVersion(testedHandles, BigInteger.ZERO, BigInteger.ONE);

        testedHandles = Arrays.asList(Handles.CONTEXTDESCRIPTOR_0);

        stateModifications = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT);

        stateModifications.add(MockModelFactory.createContextState(Handles.CONTEXT_0, Handles.CONTEXTDESCRIPTOR_0, BigInteger.ONE, PatientContextState.class));
        stateModifications.add(MockModelFactory.createContextState(Handles.CONTEXT_1, Handles.CONTEXTDESCRIPTOR_0, BigInteger.ONE, PatientContextState.class));
        mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), stateModifications);

        testWithVersion(testedHandles, BigInteger.ZERO, BigInteger.ONE);

        // add a MultiState to a descriptor without any previous states
        stateModifications = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT);
        stateModifications.add(MockModelFactory.createContextState(Handles.CONTEXT_2, Handles.CONTEXTDESCRIPTOR_1, BigInteger.ONE, LocationContextState.class));
        mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), stateModifications);
        assertThat(mdibStorage.getContextStates(Handles.CONTEXTDESCRIPTOR_1, LocationContextState.class).size(), is(1));

        // add a second MultiState
        stateModifications = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT);
        stateModifications.add(MockModelFactory.createContextState(Handles.CONTEXT_3, Handles.CONTEXTDESCRIPTOR_1, BigInteger.ONE, LocationContextState.class));
        mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), stateModifications);
        assertThat(mdibStorage.getContextStates(Handles.CONTEXTDESCRIPTOR_1, LocationContextState.class).size(), is(2));
    }

    @Test
    void writeNotAssociatedContextStatesThroughDescriptionUpdate() {
        applyDescriptionWithVersion(MdibDescriptionModification.Type.INSERT, BigInteger.ZERO);

        // Make sure there are two states in the MDIB for tested patient context
        var states = mdibStorage.findContextStatesByType(PatientContextState.class);
        assertEquals(2, states.size());

        // Get states and set the first one not-associated
        var entity = mdibStorage.getEntity(Handles.CONTEXTDESCRIPTOR_0);
        assertTrue(entity.isPresent());
        states = entity.get().getStates(PatientContextState.class);
        assertEquals(2, states.size());
        states.get(0).setContextAssociation(ContextAssociation.NO);
        var modifications = MdibDescriptionModifications.create()
                .update(entity.get().getDescriptor(), states);
        var result = mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), mock(BigInteger.class), modifications);

        // After write check that there was one updated entity with two updated states where one state is not associated
        assertEquals(1, result.getUpdatedEntities().size());
        assertEquals(2, result.getUpdatedEntities().get(0).getStates().size());
        states = result.getUpdatedEntities().get(0).getStates(PatientContextState.class);
        assertEquals(Handles.CONTEXT_0, states.get(0).getHandle());
        assertEquals(ContextAssociation.NO, states.get(0).getContextAssociation());

        // Check that the state is missing from the MDIB
        entity = mdibStorage.getEntity(Handles.CONTEXTDESCRIPTOR_0);
        assertTrue(entity.isPresent());
        states = entity.get().getStates(PatientContextState.class);
        assertEquals(1, states.size());
        assertEquals(Handles.CONTEXT_1, states.get(0).getHandle());

        // Whitebox: triggers internal access to contextStates map
        // That one shall be updated, too
        states = mdibStorage.findContextStatesByType(PatientContextState.class);
        assertEquals(1, states.size());
    }

    @Test
    void writeNotAssociatedContextStatesThroughDescriptionInsert() {
        applyDescriptionWithVersion(MdibDescriptionModification.Type.INSERT, BigInteger.ZERO);

        // Make sure there are two states in the MDIB for tested patient context
        var states = mdibStorage.findContextStatesByType(EnsembleContextState.class);
        assertEquals(0, states.size());

        // Get states and add another one as not associated
        var newDescriptor = new EnsembleContextDescriptor();
        newDescriptor.setHandle(Handles.CONTEXTDESCRIPTOR_2);
        var newState = new EnsembleContextState();
        newState.setDescriptorHandle(Handles.CONTEXTDESCRIPTOR_2);
        newState.setHandle(Handles.CONTEXT_3);
        newState.setContextAssociation(ContextAssociation.NO);
        states.add(newState);
        var modifications = MdibDescriptionModifications.create()
                .insert(newDescriptor, states, Handles.SYSTEMCONTEXT_0);
        var result = mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), mock(BigInteger.class), modifications);

        // After write check that there was one inserted entity with one inserted state where that one state is not associated
        assertEquals(1, result.getInsertedEntities().size());
        assertEquals(1, result.getInsertedEntities().get(0).getStates().size());
        states = result.getInsertedEntities().get(0).getStates(EnsembleContextState.class);
        assertEquals(Handles.CONTEXT_3, states.get(0).getHandle());
        assertEquals(ContextAssociation.NO, states.get(0).getContextAssociation());

        // Check that the state is missing from the MDIB as a not-associated context state is never written to the MDIB
        var entity = mdibStorage.getEntity(Handles.CONTEXTDESCRIPTOR_2);
        assertTrue(entity.isPresent());
        states = entity.get().getStates(EnsembleContextState.class);
        assertEquals(0, states.size());

        // Whitebox: triggers internal access to contextStates map
        states = mdibStorage.findContextStatesByType(EnsembleContextState.class);
        assertEquals(0, states.size());
    }

    @Test
    void writeNotAssociatedContextStatesThroughStateUpdate() {
        applyDescriptionWithVersion(MdibDescriptionModification.Type.INSERT, BigInteger.ZERO);

        {
            // Read disassociated context, write to associated
            var state = mdibStorage.getState(Handles.CONTEXT_0, AbstractContextState.class);
            assertTrue(state.isPresent());
            assertEquals(ContextAssociation.DIS, state.get().getContextAssociation());
            state.get().setContextAssociation(ContextAssociation.ASSOC);
            mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class),
                    MdibStateModifications.create(MdibStateModifications.Type.CONTEXT)
                            .add(state.get()));
            state = mdibStorage.getState(Handles.CONTEXT_0, AbstractContextState.class);
            assertTrue(state.isPresent());
            assertEquals(ContextAssociation.ASSOC, state.get().getContextAssociation());
        }
        {
            // Read associated context, write to not-associated (i.e., remove)
            var state = mdibStorage.getState(Handles.CONTEXT_0, AbstractContextState.class);
            assertTrue(state.isPresent());
            state.get().setContextAssociation(ContextAssociation.NO);
            var applyResult = mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class),
                    MdibStateModifications.create(MdibStateModifications.Type.CONTEXT)
                            .add(state.get()));
            // Expect the state to be part of the report
            assertEquals(1, applyResult.getStates().size());
            var statesList = applyResult.getStates().values().stream().findFirst().orElseThrow();
            assertEquals(1, statesList.size());
            var writtenState = statesList.get(0);
            assertTrue(writtenState instanceof AbstractContextState);
            assertEquals(ContextAssociation.NO, ((AbstractContextState) writtenState).getContextAssociation());
            state = mdibStorage.getState(Handles.CONTEXT_0, AbstractContextState.class);
            // Expect the state not to be in the MDIB anymore
            assertTrue(state.isEmpty());
        }
        {
            // Write a new context state as not-associated
            var contextState = new PatientContextState();
            contextState.setHandle(Handles.CONTEXT_3);
            contextState.setDescriptorHandle(Handles.CONTEXTDESCRIPTOR_0);

            var applyResult = mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class),
                    MdibStateModifications.create(MdibStateModifications.Type.CONTEXT)
                            .add(contextState));
            // Expect the state to be part of the report
            assertEquals(1, applyResult.getStates().size());
            var statesList = applyResult.getStates().values().stream().findFirst().orElseThrow();
            assertEquals(1, statesList.size());
            var writtenState = statesList.get(0);
            assertTrue(writtenState instanceof AbstractContextState);
            assertNull(((AbstractContextState) writtenState).getContextAssociation());
            var state = mdibStorage.getState(Handles.CONTEXT_3, AbstractContextState.class);
            // Expect the state not to be in the MDIB
            assertTrue(state.isEmpty());
        }
    }

    @Test
    void testParentOfInsertedEntitiesAppearInUpdatedList() {
        applyDescriptionWithVersion(MdibDescriptionModification.Type.INSERT, BigInteger.ZERO);

        {
            // Check that parent MDS appears in updated list
            var modifications = MdibDescriptionModifications.create()
                    .insert(MockModelFactory.createDescriptor(Handles.VMD_1, BigInteger.ZERO, VmdDescriptor.class),
                            MockModelFactory.createState(Handles.VMD_1, BigInteger.ZERO, VmdState.class),
                            Handles.MDS_0);
            var result = mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), mock(BigInteger.class), modifications);

            assertEquals(1, result.getInsertedEntities().size());
            assertEquals(Handles.VMD_1, result.getInsertedEntities().get(0).getHandle());
            assertEquals(1, result.getUpdatedEntities().size());
            assertEquals(Handles.MDS_0, result.getUpdatedEntities().get(0).getHandle());
        }

        {
            // Check that parent VMD does not appear twice in updated list
            var modifications = MdibDescriptionModifications.create()
                    .insert(MockModelFactory.createDescriptor(Handles.CHANNEL_2, BigInteger.ZERO, ChannelDescriptor.class),
                            MockModelFactory.createState(Handles.CHANNEL_2, BigInteger.ZERO, ChannelState.class),
                            Handles.VMD_0)
                    .update(MockModelFactory.createDescriptor(Handles.VMD_0, BigInteger.ZERO, MdsDescriptor.class),
                            MockModelFactory.createState(Handles.VMD_0, BigInteger.ZERO, MdsState.class));
            var result = mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), mock(BigInteger.class), modifications);

            assertEquals(1, result.getInsertedEntities().size());
            assertEquals(Handles.CHANNEL_2, result.getInsertedEntities().get(0).getHandle());
            assertEquals(1, result.getUpdatedEntities().size());
            assertEquals(Handles.VMD_0, result.getUpdatedEntities().get(0).getHandle());
        }

        {
            // Check that existing parent VMD does not appear twice in updated list
            var modifications = MdibDescriptionModifications.create()
                    .insert(MockModelFactory.createDescriptor(Handles.CHANNEL_2, BigInteger.ZERO, ChannelDescriptor.class),
                            MockModelFactory.createState(Handles.CHANNEL_2, BigInteger.ZERO, ChannelState.class),
                            Handles.VMD_0)
                    .update(MockModelFactory.createDescriptor(Handles.VMD_0, BigInteger.ZERO, MdsDescriptor.class),
                            MockModelFactory.createState(Handles.VMD_0, BigInteger.ZERO, MdsState.class));
            var result = mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), mock(BigInteger.class), modifications);

            assertEquals(1, result.getInsertedEntities().size());
            assertEquals(Handles.CHANNEL_2, result.getInsertedEntities().get(0).getHandle());
            assertEquals(1, result.getUpdatedEntities().size());
            assertEquals(Handles.VMD_0, result.getUpdatedEntities().get(0).getHandle());
        }

        {
            // Check that inserted parent VMD does not appear in updated list (it is sufficient to add it to the
            // insertedEntities list
            var modifications = MdibDescriptionModifications.create()
                    .insert(MockModelFactory.createDescriptor(Handles.VMD_2, BigInteger.ZERO, VmdDescriptor.class),
                            MockModelFactory.createState(Handles.VMD_2, BigInteger.ZERO, VmdState.class),
                            Handles.MDS_0)
                    .insert(MockModelFactory.createDescriptor(Handles.CHANNEL_3, BigInteger.ZERO, ChannelDescriptor.class),
                            MockModelFactory.createState(Handles.CHANNEL_3, BigInteger.ZERO, ChannelState.class),
                            Handles.VMD_2);
            var result = mdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), mock(BigInteger.class), modifications);

            assertEquals(2, result.getInsertedEntities().size());
            assertEquals(Handles.VMD_2, result.getInsertedEntities().get(0).getHandle());
            assertEquals(Handles.CHANNEL_3, result.getInsertedEntities().get(1).getHandle());
            assertEquals(1, result.getUpdatedEntities().size());
            assertEquals(Handles.MDS_0, result.getUpdatedEntities().get(0).getHandle());
        }
    }

    MdibStorage mdibStorageWithKeepContextStates() {
        Injector injector = new UnitTestUtil(new AbstractConfigurationModule() {
            @Override
            protected void defaultConfigure() {
                bind(CommonConfig.COPY_MDIB_INPUT, Boolean.class, true);
                bind(CommonConfig.COPY_MDIB_OUTPUT, Boolean.class, true);
                bind(CommonConfig.STORE_NOT_ASSOCIATED_CONTEXT_STATES, Boolean.class, true);
                bind(CommonConfig.CONSUMER_STATE_PREPROCESSING_SEGMENTS,
                        new TypeLiteral<List<Class<? extends StatePreprocessingSegment>>>() {
                        },
                        List.of(DuplicateContextStateHandleHandler.class, VersionDuplicateHandler.class));

                bind(CommonConfig.CONSUMER_DESCRIPTION_PREPROCESSING_SEGMENTS,
                        new TypeLiteral<List<Class<? extends DescriptionPreprocessingSegment>>>() {
                        },
                        List.of(DescriptorChildRemover.class));

                bind(CommonConfig.PROVIDER_STATE_PREPROCESSING_SEGMENTS,
                        new TypeLiteral<List<Class<? extends StatePreprocessingSegment>>>() {
                        },
                        List.of(DuplicateContextStateHandleHandler.class, VersionHandler.class));

                bind(CommonConfig.PROVIDER_DESCRIPTION_PREPROCESSING_SEGMENTS,
                        new TypeLiteral<List<Class<? extends DescriptionPreprocessingSegment>>>() {
                        },
                        List.of(DuplicateChecker.class, TypeConsistencyChecker.class, VersionHandler.class,
                                HandleReferenceHandler.class, DescriptorChildRemover.class));
            }
        }).getInjector();
        return injector.getInstance(MdibStorageFactory.class).createMdibStorage();
    }

    /*
     Tests intentionally enabling storage of not associated context states.
     */
    @Test
    void keepNotAssociatedContextStatesThroughDescriptionUpdate() {
        var localMdibStorage = mdibStorageWithKeepContextStates();
        applyDescriptionWithVersion(localMdibStorage, MdibDescriptionModification.Type.INSERT, BigInteger.ZERO);

        // Make sure there are two states in the MDIB for tested patient context
        var states = localMdibStorage.findContextStatesByType(PatientContextState.class);
        assertEquals(2, states.size());

        // Get states and set the first one not-associated
        var entity = localMdibStorage.getEntity(Handles.CONTEXTDESCRIPTOR_0);
        assertTrue(entity.isPresent());
        states = entity.get().getStates(PatientContextState.class);
        assertEquals(2, states.size());
        states.get(0).setContextAssociation(ContextAssociation.NO);
        var modifications = MdibDescriptionModifications.create()
                .update(entity.get().getDescriptor(), states);
        var result = localMdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), mock(BigInteger.class), modifications);

        // After write check that there was one updated entity with two updated states where one state is not associated
        assertEquals(1, result.getUpdatedEntities().size());
        assertEquals(2, result.getUpdatedEntities().get(0).getStates().size());
        states = result.getUpdatedEntities().get(0).getStates(PatientContextState.class);
        assertEquals(Handles.CONTEXT_0, states.get(0).getHandle());
        assertEquals(ContextAssociation.NO, states.get(0).getContextAssociation());

        // Check that the state is present from the MDIB
        entity = localMdibStorage.getEntity(Handles.CONTEXTDESCRIPTOR_0);
        assertTrue(entity.isPresent());
        states = entity.get().getStates(PatientContextState.class);
        assertEquals(2, states.size());
        assertEquals(Handles.CONTEXT_0, states.get(0).getHandle());
        assertEquals(Handles.CONTEXT_1, states.get(1).getHandle());

        // Whitebox: triggers internal access to contextStates map
        // That one shall be updated, too
        states = localMdibStorage.findContextStatesByType(PatientContextState.class);
        assertEquals(2, states.size());
    }

    @Test
    void keepNotAssociatedContextStatesThroughDescriptionInsert() {
        var localMdibStorage = mdibStorageWithKeepContextStates();
        applyDescriptionWithVersion(localMdibStorage, MdibDescriptionModification.Type.INSERT, BigInteger.ZERO);

        // Make sure there are no states in the MDIB for tested patient context
        var states = localMdibStorage.findContextStatesByType(EnsembleContextState.class);
        assertEquals(0, states.size());

        // Get states and add another one as not associated
        var newDescriptor = new EnsembleContextDescriptor();
        newDescriptor.setHandle(Handles.CONTEXTDESCRIPTOR_2);
        var newState = new EnsembleContextState();
        newState.setDescriptorHandle(Handles.CONTEXTDESCRIPTOR_2);
        newState.setHandle(Handles.CONTEXT_3);
        newState.setContextAssociation(ContextAssociation.NO);
        states.add(newState);
        var modifications = MdibDescriptionModifications.create()
                .insert(newDescriptor, states, Handles.SYSTEMCONTEXT_0);
        var result = localMdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), mock(BigInteger.class), modifications);

        // After write check that there was one inserted entity with one inserted state where that one state is not associated
        assertEquals(1, result.getInsertedEntities().size());
        assertEquals(1, result.getInsertedEntities().get(0).getStates().size());
        states = result.getInsertedEntities().get(0).getStates(EnsembleContextState.class);
        assertEquals(Handles.CONTEXT_3, states.get(0).getHandle());
        assertEquals(ContextAssociation.NO, states.get(0).getContextAssociation());

        // Check that the state is not missing from the MDIB as a not-associated context state is written to the MDIB
        var entity = localMdibStorage.getEntity(Handles.CONTEXTDESCRIPTOR_2);
        assertTrue(entity.isPresent());
        states = entity.get().getStates(EnsembleContextState.class);
        assertEquals(1, states.size());

        // Whitebox: triggers internal access to contextStates map
        states = localMdibStorage.findContextStatesByType(EnsembleContextState.class);
        assertEquals(1, states.size());
    }

    @Test
    void keepNotAssociatedContextStatesThroughStateUpdate() {
        var localMdibStorage = mdibStorageWithKeepContextStates();
        applyDescriptionWithVersion(localMdibStorage, MdibDescriptionModification.Type.INSERT, BigInteger.ZERO);

        {
            // Read disassociated context, write to associated
            var state = localMdibStorage.getState(Handles.CONTEXT_0, AbstractContextState.class);
            assertTrue(state.isPresent());
            assertEquals(ContextAssociation.DIS, state.get().getContextAssociation());
            state.get().setContextAssociation(ContextAssociation.ASSOC);
            localMdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class),
                    MdibStateModifications.create(MdibStateModifications.Type.CONTEXT)
                            .add(state.get()));
            state = localMdibStorage.getState(Handles.CONTEXT_0, AbstractContextState.class);
            assertTrue(state.isPresent());
            assertEquals(ContextAssociation.ASSOC, state.get().getContextAssociation());
        }
        {
            // Read associated context, write to not-associated (i.e., remove)
            var state = localMdibStorage.getState(Handles.CONTEXT_0, AbstractContextState.class);
            assertTrue(state.isPresent());
            state.get().setContextAssociation(ContextAssociation.NO);
            var applyResult = localMdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class),
                    MdibStateModifications.create(MdibStateModifications.Type.CONTEXT)
                            .add(state.get()));
            // Expect the state to be part of the report
            assertEquals(1, applyResult.getStates().size());
            var statesList = applyResult.getStates().values().stream().findFirst().orElseThrow();
            assertEquals(1, statesList.size());
            var writtenState = statesList.get(0);
            assertTrue(writtenState instanceof AbstractContextState);
            assertEquals(ContextAssociation.NO, ((AbstractContextState) writtenState).getContextAssociation());
            state = localMdibStorage.getState(Handles.CONTEXT_0, AbstractContextState.class);
            // Expect the state not to be in the MDIB anymore
            assertTrue(state.isPresent());
        }
        {
            // Write a new context state as not-associated
            var contextState = new PatientContextState();
            contextState.setHandle(Handles.CONTEXT_3);
            contextState.setDescriptorHandle(Handles.CONTEXTDESCRIPTOR_0);

            var applyResult = localMdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class),
                    MdibStateModifications.create(MdibStateModifications.Type.CONTEXT)
                            .add(contextState));
            // Expect the state to be part of the report
            assertEquals(1, applyResult.getStates().size());
            var statesList = applyResult.getStates().values().stream().findFirst().orElseThrow();
            assertEquals(1, statesList.size());
            var writtenState = statesList.get(0);
            assertTrue(writtenState instanceof AbstractContextState);
            assertNull(((AbstractContextState) writtenState).getContextAssociation());
            var state = localMdibStorage.getState(Handles.CONTEXT_3, AbstractContextState.class);
            // Expect the state not to be in the MDIB
            assertTrue(state.isPresent());
        }
    }

    @Test
    void testStateUpdateWithoutDescriptor() {
        Injector injector = new UnitTestUtil(new AbstractConfigurationModule() {
            @Override
            protected void defaultConfigure() {
                bind(CommonConfig.COPY_MDIB_INPUT, Boolean.class, true);
                bind(CommonConfig.COPY_MDIB_OUTPUT, Boolean.class, true);
                bind(CommonConfig.STORE_NOT_ASSOCIATED_CONTEXT_STATES, Boolean.class, true);
                bind(CommonConfig.CONSUMER_STATE_PREPROCESSING_SEGMENTS,
                        new TypeLiteral<List<Class<? extends StatePreprocessingSegment>>>() {
                        },
                        List.of(DuplicateContextStateHandleHandler.class, VersionDuplicateHandler.class));

                bind(CommonConfig.CONSUMER_DESCRIPTION_PREPROCESSING_SEGMENTS,
                        new TypeLiteral<List<Class<? extends DescriptionPreprocessingSegment>>>() {
                        },
                        List.of(DescriptorChildRemover.class));

                bind(CommonConfig.PROVIDER_STATE_PREPROCESSING_SEGMENTS,
                        new TypeLiteral<List<Class<? extends StatePreprocessingSegment>>>() {
                        },
                        List.of(DuplicateContextStateHandleHandler.class, VersionHandler.class));

                bind(CommonConfig.PROVIDER_DESCRIPTION_PREPROCESSING_SEGMENTS,
                        new TypeLiteral<List<Class<? extends DescriptionPreprocessingSegment>>>() {
                        },
                        List.of(DuplicateChecker.class, TypeConsistencyChecker.class, VersionHandler.class,
                                HandleReferenceHandler.class, DescriptorChildRemover.class));
            }
        }).getInjector();
        var localMdibStorage = injector.getInstance(MdibStorageFactory.class).createMdibStorage();

        {
            // Write a new context state as not-associated
            var contextState = new PatientContextState();
            contextState.setHandle(Handles.CONTEXT_3);
            contextState.setDescriptorHandle(Handles.CONTEXTDESCRIPTOR_0);

            assertThrows(
                    RuntimeException.class,
                    () -> localMdibStorage.apply(
                            mock(MdibVersion.class), mock(BigInteger.class),
                            MdibStateModifications.create(MdibStateModifications.Type.CONTEXT).add(contextState)
                    )
            );
        }
        {
            // Make sure there are no states in the MDIB for tested patient context
            var states = localMdibStorage.findContextStatesByType(EnsembleContextState.class);
            assertEquals(0, states.size());

            // Get states and add another one as not associated
            var newDescriptor = new EnsembleContextDescriptor();
            newDescriptor.setHandle(Handles.CONTEXTDESCRIPTOR_2);
            var newState = new EnsembleContextState();
            newState.setDescriptorHandle(Handles.CONTEXTDESCRIPTOR_2);
            newState.setHandle(Handles.CONTEXT_3);
            newState.setContextAssociation(ContextAssociation.NO);
            states.add(newState);
            var modifications = MdibDescriptionModifications.create()
                    .insert(newDescriptor, states, Handles.SYSTEMCONTEXT_0);

            // throws an exception about system context 0 not being present
            var ex = assertThrows(RuntimeException.class, () -> localMdibStorage.apply(mock(MdibVersion.class), mock(BigInteger.class), mock(BigInteger.class), modifications));
            assertTrue(ex.getMessage().contains(Handles.SYSTEMCONTEXT_0));
        }
    }

}
