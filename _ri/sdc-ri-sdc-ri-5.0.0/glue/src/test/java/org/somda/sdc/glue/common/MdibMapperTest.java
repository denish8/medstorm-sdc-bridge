package org.somda.sdc.glue.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.MdibTypeValidator;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.provider.access.factory.LocalMdibAccessFactory;
import org.somda.sdc.biceps.testutil.BaseTreeModificationsSet;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.biceps.testutil.MockEntryFactory;
import org.somda.sdc.glue.UnitTestUtil;
import org.somda.sdc.glue.common.factory.MdibMapperFactory;
import test.org.somda.common.LoggingTestWatcher;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(LoggingTestWatcher.class)
class MdibMapperTest {
    private static final UnitTestUtil UT = new UnitTestUtil();

    LocalMdibAccess mdibAccess;
    BaseTreeModificationsSet baseTreeModificationsSet;
    private MdibMapper mdibMapper;

    @BeforeEach
    void beforeEach() {
        baseTreeModificationsSet = new BaseTreeModificationsSet(new MockEntryFactory(
                UT.getInjector().getInstance(MdibTypeValidator.class)));
        mdibAccess = UT.getInjector().getInstance(LocalMdibAccessFactory.class).createLocalMdibAccess();
        mdibMapper = UT.getInjector().getInstance(MdibMapperFactory.class).createMdibMapper(mdibAccess);
    }

    @Test
    void mapMdib() throws PreprocessingException {
        final MdibDescriptionModifications baseTree = baseTreeModificationsSet.createBaseTree();

        Mdib mdib = mdibMapper.mapMdib();
        assertNotNull(mdib.getMdDescription());
        assertNotNull(mdib.getMdState());
        assertEquals(0, mdib.getMdState().getState().size());
        assertEquals(BigInteger.ZERO, mdib.getMdState().getStateVersion());
        assertEquals(BigInteger.ZERO, mdib.getMdDescription().getDescriptionVersion());
        assertEquals(mdibAccess.getMdibVersion().getSequenceId().toString(), mdib.getSequenceId());
        assertEquals(mdibAccess.getMdibVersion().getInstanceId(), mdib.getInstanceId());
        assertEquals(mdibAccess.getMdibVersion().getVersion(), mdib.getMdibVersion());

        mdibAccess.writeDescription(baseTree);

        mdib = mdibMapper.mapMdib();
        assertNotNull(mdib.getMdDescription());
        assertNotNull(mdib.getMdState());
        assertEquals(baseTree.getModifications().size(), mdib.getMdState().getState().size());
        assertEquals(BigInteger.ONE, mdib.getMdState().getStateVersion());
        assertEquals(BigInteger.ONE, mdib.getMdDescription().getDescriptionVersion());

        assertEquals(BigInteger.ONE, mdib.getMdState().getStateVersion());
        assertEquals(BigInteger.ONE, mdib.getMdDescription().getDescriptionVersion());
        assertEquals(mdibAccess.getMdibVersion().getSequenceId().toString(), mdib.getSequenceId());
        assertEquals(mdibAccess.getMdibVersion().getInstanceId(), mdib.getInstanceId());
        assertEquals(mdibAccess.getMdibVersion().getVersion(), mdib.getMdibVersion());

        for (AbstractState abstractState : mdib.getMdState().getState()) {
            final Optional<MdibEntity> entity = mdibAccess.getEntity(abstractState.getDescriptorHandle());
            assertTrue(entity.isPresent());
            entity.get().doIfMultiState(multiStates -> {
                assertTrue(abstractState instanceof AbstractMultiState);
                boolean found = false;
                for (AbstractMultiState multiState : multiStates) {
                    if (multiState.getHandle().equals(((AbstractMultiState) abstractState).getHandle())) {
                        found = true;
                        break;
                    }
                }
                assertTrue(found);
            });
        }

        assertEquals(Handles.MDS_0, mdib.getMdDescription().getMds().get(0).getHandle());
        assertEquals(Handles.MDS_1, mdib.getMdDescription().getMds().get(1).getHandle());

        assertEquals(Handles.VMD_0, mdib.getMdDescription().getMds().get(0).getVmd().get(0).getHandle());
        assertEquals(Handles.VMD_1, mdib.getMdDescription().getMds().get(0).getVmd().get(1).getHandle());
        assertEquals(Handles.VMD_2, mdib.getMdDescription().getMds().get(0).getVmd().get(2).getHandle());

        assertEquals(Handles.BATTERY_0, mdib.getMdDescription().getMds().get(0).getBattery().get(0).getHandle());
        assertEquals(Handles.CLOCK_0, mdib.getMdDescription().getMds().get(0).getClock().getHandle());
        assertEquals(Handles.ALERTSYSTEM_0, mdib.getMdDescription().getMds().get(0).getAlertSystem().getHandle());
        assertEquals(Handles.SCO_0, mdib.getMdDescription().getMds().get(0).getSco().getHandle());
        assertEquals(Handles.SYSTEMCONTEXT_0, mdib.getMdDescription().getMds().get(0).getSystemContext().getHandle());

        assertEquals(Handles.CHANNEL_0, mdib.getMdDescription().getMds().get(0).getVmd().get(0).getChannel().get(0).getHandle());
        assertEquals(Handles.CHANNEL_1, mdib.getMdDescription().getMds().get(0).getVmd().get(0).getChannel().get(1).getHandle());

        assertEquals(Handles.METRIC_0, mdib.getMdDescription().getMds().get(0).getVmd().get(0).getChannel().get(0).getMetric().get(0).getHandle());
        assertEquals(Handles.METRIC_1, mdib.getMdDescription().getMds().get(0).getVmd().get(0).getChannel().get(0).getMetric().get(1).getHandle());
        assertEquals(Handles.METRIC_2, mdib.getMdDescription().getMds().get(0).getVmd().get(0).getChannel().get(0).getMetric().get(2).getHandle());
        assertEquals(Handles.METRIC_3, mdib.getMdDescription().getMds().get(0).getVmd().get(0).getChannel().get(0).getMetric().get(3).getHandle());
        assertEquals(Handles.METRIC_4, mdib.getMdDescription().getMds().get(0).getVmd().get(0).getChannel().get(0).getMetric().get(4).getHandle());

        assertEquals(Handles.OPERATION_0, mdib.getMdDescription().getMds().get(0).getSco().getOperation().get(0).getHandle());
        assertEquals(Handles.OPERATION_1, mdib.getMdDescription().getMds().get(0).getSco().getOperation().get(1).getHandle());
        assertEquals(Handles.OPERATION_2, mdib.getMdDescription().getMds().get(0).getSco().getOperation().get(2).getHandle());
        assertEquals(Handles.OPERATION_3, mdib.getMdDescription().getMds().get(0).getSco().getOperation().get(3).getHandle());
        assertEquals(Handles.OPERATION_4, mdib.getMdDescription().getMds().get(0).getSco().getOperation().get(4).getHandle());
        assertEquals(Handles.OPERATION_5, mdib.getMdDescription().getMds().get(0).getSco().getOperation().get(5).getHandle());
        assertEquals(Handles.OPERATION_6, mdib.getMdDescription().getMds().get(0).getSco().getOperation().get(6).getHandle());
        assertEquals(Handles.OPERATION_7, mdib.getMdDescription().getMds().get(1).getSco().getOperation().get(0).getHandle());

        assertEquals(Handles.CONTEXTDESCRIPTOR_0, mdib.getMdDescription().getMds().get(0).getSystemContext().getPatientContext().getHandle());
        assertEquals(Handles.CONTEXTDESCRIPTOR_1, mdib.getMdDescription().getMds().get(0).getSystemContext().getLocationContext().getHandle());
        assertEquals(Handles.CONTEXTDESCRIPTOR_2, mdib.getMdDescription().getMds().get(0).getSystemContext().getEnsembleContext().get(0).getHandle());
        assertEquals(Handles.CONTEXTDESCRIPTOR_3, mdib.getMdDescription().getMds().get(0).getSystemContext().getWorkflowContext().get(0).getHandle());
        assertEquals(Handles.CONTEXTDESCRIPTOR_4, mdib.getMdDescription().getMds().get(0).getSystemContext().getOperatorContext().get(0).getHandle());
        assertEquals(Handles.CONTEXTDESCRIPTOR_5, mdib.getMdDescription().getMds().get(0).getSystemContext().getMeansContext().get(0).getHandle());
    }

    @Test
    void mapMdState() throws PreprocessingException {
        final MdibDescriptionModifications baseTree = baseTreeModificationsSet.createBaseTree();
        mdibAccess.writeDescription(baseTree);

        {
            final MdState mdState = mdibMapper.mapMdState(Arrays.asList(Handles.MDS_0, Handles.CHANNEL_1,
                    Handles.CONTEXTDESCRIPTOR_0, Handles.CONTEXT_0));

            assertEquals(3, mdState.getState().size());
        }

        {
            final MdState mdState = mdibMapper.mapMdState(Arrays.asList(Handles.MDS_0, Handles.CHANNEL_1,
                    Handles.CONTEXTDESCRIPTOR_0, Handles.CONTEXT_1));

            assertEquals(4, mdState.getState().size());
        }
    }

    @Test
    void mapMdDescription() throws PreprocessingException {
        final MdibDescriptionModifications baseTree = baseTreeModificationsSet.createBaseTree();
        mdibAccess.writeDescription(baseTree);

        {
            final MdDescription mdDescription = mdibMapper.mapMdDescription(Arrays.asList(Handles.MDS_1,
                    Handles.CONTEXTDESCRIPTOR_0));

            assertEquals(2, mdDescription.getMds().size());
        }

        {
            final MdDescription mdDescription = mdibMapper.mapMdDescription(Arrays.asList(Handles.MDS_0,
                    Handles.CONTEXTDESCRIPTOR_0));

            assertEquals(1, mdDescription.getMds().size());
            assertEquals(Handles.MDS_0, mdDescription.getMds().get(0).getHandle());
        }

        {
            final MdDescription mdDescription = mdibMapper.mapMdDescription(Arrays.asList(Handles.METRIC_0));

            assertEquals(1, mdDescription.getMds().size());
            assertEquals(Handles.MDS_0, mdDescription.getMds().get(0).getHandle());
        }
    }
}