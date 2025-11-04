package org.somda.sdc.biceps.provider.access;


import com.google.inject.Injector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.UnitTestUtil;
import org.somda.sdc.biceps.common.CommonConfig;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.MdibTypeValidator;
import org.somda.sdc.biceps.common.access.WriteDescriptionResult;
import org.somda.sdc.biceps.common.access.WriteStateResult;
import org.somda.sdc.biceps.common.event.DescriptionModificationMessage;
import org.somda.sdc.biceps.guice.DefaultBicepsConfigModule;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractMetricDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.BatteryDescriptor;
import org.somda.sdc.biceps.model.participant.ChannelDescriptor;
import org.somda.sdc.biceps.model.participant.ChannelState;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.biceps.model.participant.MdsDescriptor;
import org.somda.sdc.biceps.model.participant.MdsState;
import org.somda.sdc.biceps.model.participant.PatientContextState;
import org.somda.sdc.biceps.model.participant.ScoDescriptor;
import org.somda.sdc.biceps.model.participant.VmdDescriptor;
import org.somda.sdc.biceps.provider.access.factory.LocalMdibAccessFactory;
import org.somda.sdc.biceps.testutil.BaseTreeModificationsSet;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.biceps.testutil.MdibAccessObserverSpy;
import org.somda.sdc.biceps.testutil.MockEntryFactory;
import org.somda.sdc.biceps.testutil.MockModelFactory;
import test.org.somda.common.LoggingTestWatcher;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(LoggingTestWatcher.class)
class LocalMdibAccessImplTest {
    private static final UnitTestUtil UT = new UnitTestUtil(new DefaultBicepsConfigModule() {
        @Override
        protected void customConfigure() {
            // Configure to avoid copying and make comparison easier
            bind(CommonConfig.COPY_MDIB_INPUT,
                    Boolean.class,
                    false);
            bind(CommonConfig.COPY_MDIB_OUTPUT,
                    Boolean.class,
                    false);
        }
    });

    private Injector injector;
    private MdibTypeValidator mdibTypeValidator;
    private LocalMdibAccess mdibAccess;
    private MockEntryFactory mockEntryFactory;

    @BeforeEach
    void beforeEach() {
        injector = UT.getInjector();
        mdibTypeValidator = injector.getInstance(MdibTypeValidator.class);
        mdibAccess = injector.getInstance(LocalMdibAccessFactory.class)
                .createLocalMdibAccess();
        mockEntryFactory = new MockEntryFactory(mdibTypeValidator);
    }

    @Test
    void initialInsertAndRead() throws Exception {
        // Given a base tree to be inserted into a local mdib access
        MdibDescriptionModifications descriptionModifications = setupBaseTree();

        // When the modifications are written to the local mdib access
        MdibAccessObserverSpy updatesSpy = new MdibAccessObserverSpy();
        mdibAccess.registerObserver(updatesSpy);
        WriteDescriptionResult writeDescriptionResult = mdibAccess.writeDescription(descriptionModifications);

        // Then expect the changes to be reflected by the return value as well as the notified event
        assertEquals(descriptionModifications.getModifications().size(), writeDescriptionResult.getInsertedEntities().size());
        assertEquals(0, writeDescriptionResult.getUpdatedEntities().size());
        assertEquals(0, writeDescriptionResult.getDeletedEntities().size());
        assertEquals(1, updatesSpy.getRecordedMessages().size());
        assertEquals(DescriptionModificationMessage.class, updatesSpy.getRecordedMessages().get(0).getClass());
        DescriptionModificationMessage castMessage = (DescriptionModificationMessage) updatesSpy.getRecordedMessages().get(0);
        assertEquals(writeDescriptionResult.getInsertedEntities(), castMessage.getInsertedEntities());
        assertEquals(writeDescriptionResult.getUpdatedEntities(), castMessage.getUpdatedEntities());
        assertEquals(writeDescriptionResult.getDeletedEntities(), castMessage.getDeletedEntities());

        // Further plausibility checks (incl. more read access)
        assertEquals(writeDescriptionResult.getMdibVersion(), castMessage.getMdibAccess().getMdibVersion());
        assertEquals(BigInteger.ONE, castMessage.getMdibAccess().getMdDescriptionVersion());
        assertEquals(BigInteger.ONE, castMessage.getMdibAccess().getMdStateVersion());

        List<PatientContextState> actualPatientContextStates = castMessage.getMdibAccess().getContextStates(Handles.CONTEXTDESCRIPTOR_0, PatientContextState.class);
        assertEquals(1, actualPatientContextStates.size());
        assertEquals(Handles.CONTEXTDESCRIPTOR_0, actualPatientContextStates.get(0).getDescriptorHandle());
        assertEquals(Handles.CONTEXT_0, actualPatientContextStates.get(0).getHandle());
        assertEquals(BigInteger.ZERO, actualPatientContextStates.get(0).getDescriptorVersion());
        assertEquals(BigInteger.ZERO, actualPatientContextStates.get(0).getStateVersion());

        Optional<MdsDescriptor> mdsDescr = castMessage.getMdibAccess().getDescriptor(Handles.MDS_0, MdsDescriptor.class);
        assertTrue(mdsDescr.isPresent());
        assertEquals(Handles.MDS_0, mdsDescr.get().getHandle());
        assertEquals(BigInteger.ZERO, mdsDescr.get().getDescriptorVersion());

        Optional<MdsState> mdsState = castMessage.getMdibAccess().getState(Handles.MDS_0, MdsState.class);
        assertTrue(mdsState.isPresent());
        assertEquals(Handles.MDS_0, mdsState.get().getDescriptorHandle());
        assertEquals(BigInteger.ZERO, mdsState.get().getDescriptorVersion());
        assertEquals(BigInteger.ZERO, mdsState.get().getStateVersion());

        Optional<MdibEntity> mdsEntity = castMessage.getMdibAccess().getEntity(Handles.MDS_0);
        assertTrue(mdsEntity.isPresent());
        assertEquals(Handles.MDS_0, mdsEntity.get().getHandle());
        assertEquals(writeDescriptionResult.getMdibVersion(), mdsEntity.get().getLastChanged());
        assertEquals(mdsDescr.get(), mdsEntity.get().getDescriptor());
        assertEquals(1, mdsEntity.get().getStates().size());
        assertEquals(mdsState.get(), mdsEntity.get().getStates().get(0));

        assertEquals(2, castMessage.getMdibAccess().getRootEntities().size());
        assertEquals(Handles.MDS_0, castMessage.getMdibAccess().getRootEntities().get(0).getHandle());

        assertEquals(8, mdsEntity.get().getChildren().size());
        List<MdibEntity> childrenByType = castMessage.getMdibAccess().getChildrenByType(Handles.MDS_0, VmdDescriptor.class);
        assertEquals(3, childrenByType.size());
        assertEquals(Handles.VMD_0, childrenByType.get(0).getHandle());
        assertEquals(Handles.VMD_1, childrenByType.get(1).getHandle());
        assertEquals(Handles.VMD_2, childrenByType.get(2).getHandle());
        assertEquals(1, castMessage.getMdibAccess().getChildrenByType(Handles.MDS_0, ScoDescriptor.class).size());
        assertEquals(0, castMessage.getMdibAccess().getChildrenByType(Handles.MDS_0, ChannelDescriptor.class).size());

        assertEquals(2, castMessage.getMdibAccess().findEntitiesByType(ChannelDescriptor.class).size());
        assertEquals(5, castMessage.getMdibAccess().findEntitiesByType(AbstractMetricDescriptor.class).size());

        Optional<MdibEntity> channelEntity = castMessage.getMdibAccess().getEntity(Handles.CHANNEL_0);
        assertTrue(channelEntity.isPresent());
        assertTrue(channelEntity.get().getParent().isPresent());
        assertEquals(Handles.VMD_0, channelEntity.get().getParent().get());
    }

    @Test
    void mdibVersioning() throws Exception {
        // Given a local mdib access with an initialized base tree
        MdibDescriptionModifications modifications = setupBaseTree();
        WriteDescriptionResult writeResult = mdibAccess.writeDescription(modifications);

        final MdibVersion expectedInitialMdibVersion = writeResult.getMdibVersion();
        final BigInteger expectedInitialVersion = BigInteger.ONE;

        MdibVersion expectedMdibVersion = expectedInitialMdibVersion;
        BigInteger expectedMdDescrVersion = expectedInitialVersion;
        BigInteger expectedMdStateVersion = expectedInitialVersion;
        assertEquals(expectedInitialVersion, expectedMdibVersion.getVersion());
        assertEquals(expectedMdDescrVersion, mdibAccess.getMdDescriptionVersion());
        assertEquals(expectedMdStateVersion, mdibAccess.getMdStateVersion());

        {
            // When an entity is inserted
            modifications = MdibDescriptionModifications.create()
                    .insert(entry(Handles.BATTERY_1, BatteryDescriptor.class, Handles.MDS_0));

            writeResult = mdibAccess.writeDescription(modifications);

            // Then expect the version changes to be reflected in the local mdib access
            expectedMdibVersion = MdibVersion.increment(expectedMdibVersion);
            expectedMdDescrVersion = expectedMdDescrVersion.add(BigInteger.ONE);
            expectedMdStateVersion = expectedMdStateVersion.add(BigInteger.ONE);
            assertEquals(mdibAccess.getMdibVersion(), writeResult.getMdibVersion());
            assertEquals(expectedMdibVersion, mdibAccess.getMdibVersion());
            assertEquals(expectedMdDescrVersion, mdibAccess.getMdDescriptionVersion());
            assertEquals(expectedMdStateVersion, mdibAccess.getMdStateVersion());
        }

        {
            // When an entity is updated
            modifications = MdibDescriptionModifications.create()
                    .update(entry(Handles.BATTERY_1, BatteryDescriptor.class));

            writeResult = mdibAccess.writeDescription(modifications);

            // Then expect the version changes to be reflected in the local mdib access
            expectedMdibVersion = MdibVersion.increment(expectedMdibVersion);
            expectedMdDescrVersion = expectedMdDescrVersion.add(BigInteger.ONE);
            expectedMdStateVersion = expectedMdStateVersion.add(BigInteger.ONE);
            assertEquals(mdibAccess.getMdibVersion(), writeResult.getMdibVersion());
            assertEquals(expectedMdibVersion, mdibAccess.getMdibVersion());
            assertEquals(expectedMdDescrVersion, mdibAccess.getMdDescriptionVersion());
            assertEquals(expectedMdStateVersion, mdibAccess.getMdStateVersion());
        }

        {
            // When an entity is deleted
            modifications = MdibDescriptionModifications.create()
                    .delete(entry(Handles.BATTERY_1, BatteryDescriptor.class));

            writeResult = mdibAccess.writeDescription(modifications);

            // Then expect the version changes to be reflected in the local mdib access
            expectedMdibVersion = MdibVersion.increment(expectedMdibVersion);
            expectedMdDescrVersion = expectedMdDescrVersion.add(BigInteger.ONE);
            expectedMdStateVersion = expectedMdStateVersion.add(BigInteger.ONE);
            assertEquals(mdibAccess.getMdibVersion(), writeResult.getMdibVersion());
            assertEquals(expectedMdibVersion, mdibAccess.getMdibVersion());
            assertEquals(expectedMdDescrVersion, mdibAccess.getMdDescriptionVersion());
            assertEquals(expectedMdStateVersion, mdibAccess.getMdStateVersion());
        }

        {
            // When an only a state is updated
            MdibStateModifications stateModifications = MdibStateModifications.create(MdibStateModifications.Type.COMPONENT)
                    .add(state(Handles.MDS_0, MdsState.class));

            WriteStateResult writeStateResult = mdibAccess.writeStates(stateModifications);

            // Then expect the version changes to be reflected in the local mdib access
            expectedMdibVersion = MdibVersion.increment(expectedMdibVersion);
            // expectedMdDescrVersion = expectedMdDescrVersion; -> state was written, hence no increment
            expectedMdStateVersion = expectedMdStateVersion.add(BigInteger.ONE);
            assertEquals(mdibAccess.getMdibVersion(), writeStateResult.getMdibVersion());
            assertEquals(expectedMdibVersion, mdibAccess.getMdibVersion());
            assertEquals(expectedMdDescrVersion, mdibAccess.getMdDescriptionVersion());
            assertEquals(expectedMdStateVersion, mdibAccess.getMdStateVersion());
        }
    }

    @Test
    void insertUpdateDelete() throws Exception {
        // Given a local mdib access with an initialized base tree
        MdibDescriptionModifications modifications = setupBaseTree();
        WriteDescriptionResult writeResult = mdibAccess.writeDescription(modifications);

        {
            // When a description is inserted
            modifications = MdibDescriptionModifications.create()
                    .insert(entry(Handles.CHANNEL_2, ChannelDescriptor.class, Handles.VMD_1));

            WriteDescriptionResult writeDescriptionResult = mdibAccess.writeDescription(modifications);

            // Then expect the element to be requestable and versioned
            assertEquals(1, writeDescriptionResult.getInsertedEntities().size());
            MdibEntity entity = writeDescriptionResult.getInsertedEntities().get(0);
            assertEquals(ChannelDescriptor.class, entity.getDescriptor().getClass());
            assertEquals(BigInteger.ZERO, entity.getDescriptor().getDescriptorVersion());
            assertEquals(1, entity.getStates().size());
            assertEquals(BigInteger.ZERO, entity.getStates().get(0).getDescriptorVersion());
            assertEquals(BigInteger.ZERO, entity.getStates().get(0).getStateVersion());

            assertTrue(mdibAccess.getEntity(Handles.VMD_1).isPresent());
            assertEquals(BigInteger.ONE, mdibAccess.getEntity(Handles.VMD_1).get().getDescriptor().getDescriptorVersion());
        }

        {
            // When the description is updated
            modifications = MdibDescriptionModifications.create()
                    .update(entry(Handles.CHANNEL_2, ChannelDescriptor.class));

            WriteDescriptionResult writeDescriptionResult = mdibAccess.writeDescription(modifications);

            // Then expect the element to be updated
            assertEquals(1, writeDescriptionResult.getUpdatedEntities().size());
            MdibEntity entity = writeDescriptionResult.getUpdatedEntities().get(0);
            assertEquals(ChannelDescriptor.class, entity.getDescriptor().getClass());
            assertEquals(BigInteger.ONE, entity.getDescriptor().getDescriptorVersion());
            assertEquals(1, entity.getStates().size());
            assertEquals(BigInteger.ONE, entity.getStates().get(0).getDescriptorVersion());
            assertEquals(BigInteger.ONE, entity.getStates().get(0).getStateVersion());

            assertTrue(mdibAccess.getEntity(Handles.VMD_1).isPresent());
            assertEquals(BigInteger.ONE, mdibAccess.getEntity(Handles.VMD_1).get().getDescriptor().getDescriptorVersion());
        }

        {
            // When the description is deleted
            modifications = MdibDescriptionModifications.create()
                    .delete(entry(Handles.CHANNEL_2, ChannelDescriptor.class));

            WriteDescriptionResult writeDescriptionResult = mdibAccess.writeDescription(modifications);

            // Then expect the element to be deleted
            assertEquals(1, writeDescriptionResult.getDeletedEntities().size());
            assertEquals(Handles.CHANNEL_2, writeDescriptionResult.getDeletedEntities().get(0).getHandle());
            assertFalse(mdibAccess.getEntity(Handles.CHANNEL_2).isPresent());

            assertTrue(mdibAccess.getEntity(Handles.VMD_1).isPresent());
            assertEquals(BigInteger.TWO, mdibAccess.getEntity(Handles.VMD_1).get().getDescriptor().getDescriptorVersion());
        }

        {
            // When the description is re-inserted
            modifications = MdibDescriptionModifications.create()
                    .insert(entry(Handles.CHANNEL_2, ChannelDescriptor.class, Handles.VMD_1));

            WriteDescriptionResult writeDescriptionResult = mdibAccess.writeDescription(modifications);

            // Then expect the element to be requestable and versioned according to the last seen version
            assertEquals(1, writeDescriptionResult.getInsertedEntities().size());
            MdibEntity entity = writeDescriptionResult.getInsertedEntities().get(0);
            assertEquals(ChannelDescriptor.class, entity.getDescriptor().getClass());
            assertEquals(BigInteger.TWO, entity.getDescriptor().getDescriptorVersion());
            assertEquals(1, entity.getStates().size());
            assertEquals(BigInteger.TWO, entity.getStates().get(0).getDescriptorVersion());
            assertEquals(BigInteger.TWO, entity.getStates().get(0).getStateVersion());

            assertTrue(mdibAccess.getEntity(Handles.VMD_1).isPresent());
            assertEquals(BigInteger.valueOf(3), mdibAccess.getEntity(Handles.VMD_1).get().getDescriptor().getDescriptorVersion());
        }

        {
            // When only the state is updated
            MdibStateModifications stateModifications = MdibStateModifications.create(MdibStateModifications.Type.COMPONENT)
                    .add(state(Handles.CHANNEL_2, ChannelState.class));

            WriteStateResult writeStateResult = mdibAccess.writeStates(stateModifications);

            // Then expect the element to be requestable and versioned according to the last seen version
            assertEquals(1, writeStateResult.getStates().size());
            var statesMap = writeStateResult.getStates();
            assertEquals(1, statesMap.size());
            AbstractState state = statesMap.values().stream().flatMap(Collection::stream).findFirst().orElseThrow();
            assertEquals(ChannelState.class, state.getClass());
            assertEquals(BigInteger.TWO, state.getDescriptorVersion());
            assertEquals(BigInteger.valueOf(3), state.getStateVersion());

            assertTrue(mdibAccess.getEntity(Handles.CHANNEL_2).isPresent());
            assertEquals(BigInteger.TWO, mdibAccess.getEntity(Handles.CHANNEL_2).get().getDescriptor().getDescriptorVersion());

            assertTrue(mdibAccess.getEntity(Handles.VMD_1).isPresent());
            assertEquals(BigInteger.valueOf(3), mdibAccess.getEntity(Handles.VMD_1).get().getDescriptor().getDescriptorVersion());
        }
    }

    private MdibDescriptionModifications setupBaseTree() {
        return new BaseTreeModificationsSet(mockEntryFactory).createBaseTree();
    }

    private <T extends AbstractDescriptor, V extends AbstractState> MdibDescriptionModifications.Entry entry(String handle, Class<T> descrClass) throws Exception {
        return entry(handle, descrClass, null);
    }

    private <T extends AbstractDescriptor, V extends AbstractState> MdibDescriptionModifications.Entry entry(String handle, Class<T> descrClass, @Nullable String parentHandle) throws Exception {
        return mockEntryFactory.entry(handle, descrClass, parentHandle);
    }

    private <T extends AbstractDescriptor, V extends AbstractContextState> MdibDescriptionModifications.MultiStateEntry contextEntry(String handle, String stateHandle, Class<T> descrClass, String parentHandle) throws Exception {
        return mockEntryFactory.contextEntry(handle, stateHandle, descrClass, parentHandle);
    }

    private <T extends AbstractDescriptor> T descriptor(String handle, Class<T> theClass) {
        return MockModelFactory.createDescriptor(handle, theClass);
    }

    private <T extends AbstractState> T state(String handle, Class<T> theClass) {
        return MockModelFactory.createState(handle, theClass);
    }
}