package org.somda.sdc.biceps.consumer.access;

import com.google.inject.Injector;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.UnitTestUtil;
import org.somda.sdc.biceps.common.*;
import org.somda.sdc.biceps.common.access.WriteDescriptionResult;
import org.somda.sdc.biceps.common.event.DescriptionModificationMessage;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.consumer.access.factory.RemoteMdibAccessFactory;
import org.somda.sdc.biceps.guice.DefaultBicepsConfigModule;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.testutil.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.org.somda.common.LoggingTestWatcher;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(LoggingTestWatcher.class)
class RemoteMdibAccessImplTest {
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
    private RemoteMdibAccess mdibAccess;
    private MockEntryFactory mockEntryFactory;

    @BeforeEach
    void beforeEach() {
        injector = UT.getInjector();
        mdibTypeValidator = injector.getInstance(MdibTypeValidator.class);
        mdibAccess = injector.getInstance(RemoteMdibAccessFactory.class).createRemoteMdibAccess();
        mockEntryFactory = new MockEntryFactory(mdibTypeValidator);
    }

    @Test
    void initialInsertAndRead() throws Exception {
        // Given a base tree to be inserted into a remote mdib access
        MdibDescriptionModifications descriptionModifications = setupBaseTree();

        // When the modifications are written to the local mdib access
        MdibAccessObserverSpy updatesSpy = new MdibAccessObserverSpy();
        mdibAccess.registerObserver(updatesSpy);
        final MdibVersion expectedInitialMdibVersion = MdibVersion.setVersionCounter(MdibVersion.create(), BigInteger.TEN);
        final BigInteger expectedInitialMdDescriptionVersion = BigInteger.TWO;
        final BigInteger expectedInitialMdStateVersion = BigInteger.TWO;
        WriteDescriptionResult writeDescriptionResult = mdibAccess.writeDescription(
                expectedInitialMdibVersion,
                expectedInitialMdDescriptionVersion,
                expectedInitialMdStateVersion,
                descriptionModifications);

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
        assertEquals(expectedInitialMdibVersion, castMessage.getMdibAccess().getMdibVersion());
        assertEquals(expectedInitialMdDescriptionVersion, castMessage.getMdibAccess().getMdDescriptionVersion());
        assertEquals(expectedInitialMdStateVersion, castMessage.getMdibAccess().getMdStateVersion());

        List<PatientContextState> actualPatientContextStates = castMessage.getMdibAccess().getContextStates(Handles.CONTEXTDESCRIPTOR_0, PatientContextState.class);
        assertEquals(1, actualPatientContextStates.size());
        assertEquals(Handles.CONTEXTDESCRIPTOR_0, actualPatientContextStates.get(0).getDescriptorHandle());
        assertEquals(Handles.CONTEXT_0, actualPatientContextStates.get(0).getHandle());
        assertEquals(BigInteger.ZERO, actualPatientContextStates.get(0).getDescriptorVersion());
        assertEquals(BigInteger.ZERO, actualPatientContextStates.get(0).getStateVersion());

        List<AbstractContextState> untypedContextStates = castMessage.getMdibAccess().getContextStates(Handles.CONTEXTDESCRIPTOR_0);
        assertEquals(1, untypedContextStates.size());
        assertEquals(Handles.CONTEXTDESCRIPTOR_0, untypedContextStates.get(0).getDescriptorHandle());
        assertEquals(Handles.CONTEXT_0, untypedContextStates.get(0).getHandle());
        assertEquals(BigInteger.ZERO, untypedContextStates.get(0).getDescriptorVersion());
        assertEquals(BigInteger.ZERO, untypedContextStates.get(0).getStateVersion());

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
    void stateUpdateWithoutDescriptorFails() throws PreprocessingException {
        final BigInteger expectedStateVersion = BigInteger.TEN;
        final BigInteger expectedDescriptorVersion = BigInteger.ONE;

        {
            // Given state updates with no corresponding descriptor to be inserted into a remote mdib access
            VmdState vmdState = MockModelFactory.createState(Handles.VMD_0, expectedStateVersion, expectedDescriptorVersion, VmdState.class);
            MdibStateModifications componentStateModifications = MdibStateModifications.create(MdibStateModifications.Type.COMPONENT)
                    .add(vmdState);

            MdibAccessObserverSpy updatesSpy = new MdibAccessObserverSpy();
            mdibAccess.registerObserver(updatesSpy);

            // When the state is written, no parent mds can be determined, write exits
            assertThrows(NullPointerException.class, () -> mdibAccess.writeStates(
                MdibVersion.create(),
                componentStateModifications));
        }

        {
            // Given multi-state updates with no corresponding descriptor to be inserted into a remote mdib access
            LocationContextState contextState = MockModelFactory.createContextState(Handles.CONTEXT_0,
                    Handles.CONTEXTDESCRIPTOR_0, expectedStateVersion, expectedDescriptorVersion, LocationContextState.class);
            MdibStateModifications contextStateModifications = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT)
                    .add(contextState);

            MdibAccessObserverSpy updatesSpy = new MdibAccessObserverSpy();
            mdibAccess.registerObserver(updatesSpy);

            // When the state is written, no parent mds can be determined, write exits
            assertThrows(NullPointerException.class, () -> mdibAccess.writeStates(
                MdibVersion.create(),
                contextStateModifications));
        }
    }

    private MdibDescriptionModifications setupBaseTree() {
        return new BaseTreeModificationsSet(mockEntryFactory).createBaseTree();
    }
}