package it.org.somda.glue;

import it.org.somda.glue.consumer.TestSdcClient;
import it.org.somda.glue.provider.TestSdcDevice;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.MdibTypeValidator;
import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.common.event.ContextStateModificationMessage;
import org.somda.sdc.biceps.common.event.DescriptionModificationMessage;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.model.participant.ContextAssociation;
import org.somda.sdc.biceps.model.participant.LocationContextDescriptor;
import org.somda.sdc.biceps.model.participant.LocationContextState;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.BaseTreeModificationsSet;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.biceps.testutil.MdibAccessObserverSpy;
import org.somda.sdc.biceps.testutil.MockEntryFactory;
import org.somda.sdc.glue.consumer.ConnectConfiguration;
import test.org.somda.common.CIDetector;
import test.org.somda.common.LoggingTestWatcher;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(LoggingTestWatcher.class)
class MdibTransferIT {
    private static final Logger LOG = LogManager.getLogger(MdibTransferIT.class);
    private static final IntegrationTestUtil IT = new IntegrationTestUtil();

    private TestSdcDevice testDevice;
    private TestSdcClient testClient;

    private static int WAIT_IN_SECONDS = 120;

    static {
        if (CIDetector.isRunningInCi()) {
            WAIT_IN_SECONDS = 180;
            LOG.info("CI detected, setting WAIT_IN_SECONDS to {}", WAIT_IN_SECONDS);
        }
    }

    private static final TimeUnit WAIT_TIME_UNIT = TimeUnit.MINUTES;
    private static final Duration WAIT_DURATION = Duration.ofMinutes(WAIT_IN_SECONDS);
    private LocalMdibAccess localMdibAccess;
    private MdibAccess remoteMdibAccess;
    private MdibAccessObserverSpy mdibSpy;

    @BeforeEach
    void beforeEach(TestInfo testInfo) throws Exception {
        LOG.info("Running test case {}", testInfo.getDisplayName());

        testDevice = new TestSdcDevice();
        testClient = new TestSdcClient();

        localMdibAccess = testDevice.getSdcDevice().getMdibAccess();

        setupBaseMdib(localMdibAccess);
        testDevice.startAsync().awaitRunning(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
        testClient.startAsync().awaitRunning(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        var hostingServiceFuture = testClient.getClient()
                .connect(testDevice.getSdcDevice().getEprAddress());
        var hostingServiceProxy = hostingServiceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
        var remoteDeviceFuture = testClient.getConnector().connect(hostingServiceProxy,
                ConnectConfiguration.create(ConnectConfiguration.ALL_EPISODIC_AND_WAVEFORM_REPORTS));
        var sdcRemoteDevice = remoteDeviceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        remoteMdibAccess = sdcRemoteDevice.getMdibAccess();
        mdibSpy = new MdibAccessObserverSpy();
        sdcRemoteDevice.getMdibAccessObservable().registerObserver(mdibSpy);
    }

    @AfterEach
    void afterEach(TestInfo testInfo) {
        LOG.info("Done with test case {}", testInfo.getDisplayName());
        testDevice.stopAsync().awaitTerminated();
        testClient.stopAsync().awaitTerminated();
    }

    @Test
    void checkContextStateDeletionOnStateUpdate() throws Exception {
        assertTrue(localMdibAccess.getState(Handles.CONTEXT_1, LocationContextState.class).isPresent());
        assertTrue(remoteMdibAccess.getState(Handles.CONTEXT_1, LocationContextState.class).isPresent());

        var state = createLocationContextState(Handles.CONTEXT_1, ContextAssociation.NO);
        var modifications = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT)
                .add(state);
        var writeStateResult = localMdibAccess.writeStates(modifications);
        var writtenStates = writeStateResult.getStates();
        assertEquals(1, writtenStates.size());
        var writtenStatesList = writtenStates.values().stream().flatMap(it -> it.stream()).collect(Collectors.toList());
        assertTrue(writtenStatesList.get(0) instanceof LocationContextState);
        assertEquals(Handles.CONTEXT_1, ((LocationContextState) writtenStatesList.get(0)).getHandle());
        assertTrue(localMdibAccess.getState(Handles.CONTEXT_0, LocationContextState.class).isEmpty());

        assertTrue(mdibSpy.waitForNumberOfRecordedMessages(1, WAIT_DURATION));
        assertTrue(mdibSpy.getRecordedMessages().get(0) instanceof ContextStateModificationMessage);
        var recordedMessage = (ContextStateModificationMessage) mdibSpy.getRecordedMessages().get(0);
        assertEquals(1, recordedMessage.getStates().size());
        var contextStatesList = recordedMessage.getStates().get(Handles.MDS_0);
        assertEquals(ContextAssociation.NO, contextStatesList.get(0).getContextAssociation());
        assertEquals(Handles.CONTEXT_1, contextStatesList.get(0).getHandle());
        assertTrue(remoteMdibAccess.getState(Handles.CONTEXT_1, LocationContextState.class).isEmpty());
    }

    @Test
    void checkContextStateDeletionOnDescriptionUpdate() throws Exception {
        assertTrue(localMdibAccess.getState(Handles.CONTEXT_1, LocationContextState.class).isPresent());
        assertTrue(remoteMdibAccess.getState(Handles.CONTEXT_1, LocationContextState.class).isPresent());

        var locationEntity = localMdibAccess.getEntity(Handles.CONTEXTDESCRIPTOR_1);
        assertTrue(locationEntity.isPresent());

        var state = createLocationContextState(Handles.CONTEXT_1, ContextAssociation.NO);
        var modifications = MdibDescriptionModifications.create()
                .update(locationEntity.get().getDescriptor(), Collections.singletonList(state));
        var writeDescrResult = localMdibAccess.writeDescription(modifications);
        var writtenItems = writeDescrResult.getUpdatedEntities();
        assertEquals(1, writtenItems.size());
        assertTrue(writtenItems.get(0).getDescriptor() instanceof LocationContextDescriptor);
        assertEquals(1, writtenItems.get(0).getStates().size());
        assertEquals(Handles.CONTEXT_1, ((LocationContextState) writtenItems.get(0).getStates().get(0)).getHandle());
        assertTrue(localMdibAccess.getState(Handles.CONTEXT_0, LocationContextState.class).isEmpty());

        assertTrue(mdibSpy.waitForNumberOfRecordedMessages(1, WAIT_DURATION));
        assertTrue(mdibSpy.getRecordedMessages().get(0) instanceof DescriptionModificationMessage);
        var recordedMessage = (DescriptionModificationMessage) mdibSpy.getRecordedMessages().get(0);
        assertEquals(1, recordedMessage.getUpdatedEntities().size());
        assertEquals(1, recordedMessage.getUpdatedEntities().get(0).getStates().size());
        assertEquals(ContextAssociation.NO, ((LocationContextState) recordedMessage.getUpdatedEntities().get(0).getStates().get(0)).getContextAssociation());
        assertEquals(Handles.CONTEXT_1, ((LocationContextState) recordedMessage.getUpdatedEntities().get(0).getStates().get(0)).getHandle());
        assertTrue(remoteMdibAccess.getState(Handles.CONTEXT_1, LocationContextState.class).isEmpty());
    }

    private void setupBaseMdib(LocalMdibAccess mdibAccess) throws PreprocessingException {
        var baseTree = new BaseTreeModificationsSet(new MockEntryFactory(IT.getInjector().getInstance(MdibTypeValidator.class)));
        mdibAccess.writeDescription(baseTree.createBaseTree());
    }

    private LocationContextState createLocationContextState(String handle, ContextAssociation contextAssociation) {
        var state = new LocationContextState();
        state.setHandle(handle);
        state.setDescriptorHandle(Handles.CONTEXTDESCRIPTOR_1);
        state.setContextAssociation(contextAssociation);
        return state;
    }
}
