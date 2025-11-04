package it.org.somda.glue;

import com.google.common.util.concurrent.ListenableFuture;
import it.org.somda.glue.consumer.ReportListenerSpy;
import it.org.somda.glue.consumer.TestSdcClient;
import it.org.somda.glue.provider.TestSdcDevice;
import it.org.somda.glue.provider.VentilatorMdibRunner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.common.CommonConstants;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.event.AbstractMdibAccessMessage;
import org.somda.sdc.biceps.common.event.AlertStateModificationMessage;
import org.somda.sdc.biceps.common.event.MetricStateModificationMessage;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.model.message.GetContainmentTree;
import org.somda.sdc.biceps.model.message.GetContainmentTreeResponse;
import org.somda.sdc.biceps.model.message.GetDescriptor;
import org.somda.sdc.biceps.model.message.GetDescriptorResponse;
import org.somda.sdc.biceps.model.message.InvocationError;
import org.somda.sdc.biceps.model.message.InvocationState;
import org.somda.sdc.biceps.model.message.SetString;
import org.somda.sdc.biceps.model.message.SetStringResponse;
import org.somda.sdc.biceps.model.participant.AbstractAlertState;
import org.somda.sdc.biceps.model.participant.AbstractMetricState;
import org.somda.sdc.biceps.model.participant.AlertConditionState;
import org.somda.sdc.biceps.model.participant.AlertSignalPresence;
import org.somda.sdc.biceps.model.participant.AlertSignalState;
import org.somda.sdc.biceps.model.participant.ContainmentTree;
import org.somda.sdc.biceps.model.participant.EnumStringMetricState;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.biceps.model.participant.NumericMetricState;
import org.somda.sdc.biceps.model.participant.factory.CodedValueFactory;
import org.somda.sdc.biceps.testutil.MdibAccessObserverSpy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.glue.common.MdibXmlIo;
import org.somda.sdc.glue.common.factory.ModificationsBuilderFactory;
import org.somda.sdc.glue.consumer.ConnectConfiguration;
import org.somda.sdc.glue.consumer.SdcRemoteDevice;
import org.somda.sdc.glue.consumer.sco.ScoTransaction;
import org.somda.sdc.glue.provider.plugin.SdcRequiredTypesAndScopes;
import org.somda.sdc.glue.provider.sco.Context;
import org.somda.sdc.glue.provider.sco.IncomingSetServiceRequest;
import org.somda.sdc.glue.provider.sco.InvocationResponse;
import org.somda.sdc.glue.provider.sco.OperationInvocationReceiver;
import test.org.somda.common.CIDetector;
import test.org.somda.common.LoggingTestWatcher;

import javax.xml.namespace.QName;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(LoggingTestWatcher.class)
class CommunicationIT {
    private static final Logger LOG = LogManager.getLogger(CommunicationIT.class);
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

    private static final TimeUnit WAIT_TIME_UNIT = TimeUnit.SECONDS;
    private static final Duration WAIT_DURATION = Duration.ofSeconds(WAIT_IN_SECONDS);
    private VentilatorMdibRunner ventilatorMdibRunner;

    private Optional<MdibVersion> lastInvocationMdibVersion = Optional.empty();

    @BeforeEach
    void beforeEach(TestInfo testInfo) {
        LOG.info("Running test case {}", testInfo.getDisplayName());
        ventilatorMdibRunner = new VentilatorMdibRunner(
                IT.getInjector().getInstance(MdibXmlIo.class),
                IT.getInjector().getInstance(ModificationsBuilderFactory.class));

        testDevice = new TestSdcDevice(Collections.singletonList(new OperationInvocationReceiver() {
            @IncomingSetServiceRequest(operationHandle = VentilatorMdibRunner.HANDLE_SET_MDC_DEV_SYS_PT_VENT_VMD)
            public InvocationResponse onSet(Context context, String requestedValue) throws PreprocessingException {
                Optional<VentilatorMdibRunner.VentilatorMode> match = Optional.empty();
                for (VentilatorMdibRunner.VentilatorMode value : VentilatorMdibRunner.VentilatorMode.values()) {
                    if (requestedValue.equals(value.getModeValue())) {
                        match = Optional.of(value);
                        break;
                    }
                }

                if (match.isEmpty()) {
                    context.sendUnsuccessfulReport(InvocationState.FAIL, InvocationError.OTH, Collections.emptyList());
                    return context.createUnsuccessfulResponse(InvocationState.FAIL, InvocationError.OTH, Collections.emptyList());
                }

                var mdibVersion = context.getMdibAccess().getMdibVersion();

                lastInvocationMdibVersion = Optional.of(mdibVersion);

                context.sendSuccessfulReport(mdibVersion, InvocationState.WAIT);
                context.sendSuccessfulReport(mdibVersion, InvocationState.START);
                ventilatorMdibRunner.changeMetrics(match.get(), null);
                context.sendSuccessfulReport(mdibVersion, InvocationState.FIN);
                return context.createSuccessfulResponse(mdibVersion, InvocationState.FIN);
            }
        }), Arrays.asList(ventilatorMdibRunner, IT.getInjector().getInstance(SdcRequiredTypesAndScopes.class)));
        testClient = new TestSdcClient();
    }

    @AfterEach
    void afterEach(TestInfo testInfo) {
        LOG.info("Done with test case {}", testInfo.getDisplayName());
        testDevice.stopAsync().awaitTerminated();
        testClient.stopAsync().awaitTerminated();
    }

    @Test
    void connectOneClientAndTransferData() throws Exception {
        testDevice.startAsync().awaitRunning(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
        testClient.startAsync().awaitRunning(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        final ListenableFuture<HostingServiceProxy> hostingServiceFuture = testClient.getClient()
                .connect(testDevice.getSdcDevice().getEprAddress());
        final HostingServiceProxy hostingServiceProxy = hostingServiceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        final ListenableFuture<SdcRemoteDevice> remoteDeviceFuture = testClient.getConnector().connect(hostingServiceProxy,
                ConnectConfiguration.create(ConnectConfiguration.ALL_EPISODIC_AND_WAVEFORM_REPORTS));

        final SdcRemoteDevice sdcRemoteDevice = remoteDeviceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
        final MdibAccessObserverSpy mdibSpy = new MdibAccessObserverSpy();

        sdcRemoteDevice.getMdibAccessObservable().registerObserver(mdibSpy);

        {
            mdibSpy.reset();
            ventilatorMdibRunner.changeMetrics(VentilatorMdibRunner.VentilatorMode.VENT_MODE_INSPASSIST, null);

            assertTrue(mdibSpy.waitForNumberOfRecordedMessages(1, WAIT_DURATION));
            final AbstractMdibAccessMessage recordedMessage = mdibSpy.getRecordedMessages().get(0);
            assertTrue(recordedMessage instanceof MetricStateModificationMessage);
            MetricStateModificationMessage metricStateMessage = (MetricStateModificationMessage) recordedMessage;
            assertEquals(1, metricStateMessage.getStates().size());
            var metricStateChangeList = Optional.ofNullable(metricStateMessage.getStates().get(VentilatorMdibRunner.HANDLE_MDC_DEV_SYS_PT_VENT_MDS)).orElseThrow();
            assertEquals(1, metricStateChangeList.size());
            final AbstractMetricState abstractMetricState = metricStateChangeList.get(0);
            assertTrue(abstractMetricState instanceof EnumStringMetricState);
            EnumStringMetricState stringMetricState = (EnumStringMetricState) abstractMetricState;
            assertEquals(VentilatorMdibRunner.VentilatorMode.VENT_MODE_INSPASSIST.getModeValue(),
                    stringMetricState.getMetricValue().getValue());
        }

        {
            mdibSpy.reset();
            ventilatorMdibRunner.changeAlertsPresence(true);

            assertTrue(mdibSpy.waitForNumberOfRecordedMessages(1, WAIT_DURATION));
            final AbstractMdibAccessMessage recordedMessage = mdibSpy.getRecordedMessages().get(0);
            assertTrue(recordedMessage instanceof AlertStateModificationMessage);
            AlertStateModificationMessage alertStateMessage = (AlertStateModificationMessage) recordedMessage;
            assertEquals(1, alertStateMessage.getStates().size());
            var alertStateChangeList = alertStateMessage.getStates().values().stream().findFirst().orElseThrow();
            assertEquals(2, alertStateChangeList.size());
            final AbstractAlertState abstractConditionState = alertStateChangeList.get(0);
            assertTrue(abstractConditionState instanceof AlertConditionState);
            AlertConditionState conditionState = (AlertConditionState) abstractConditionState;
            assertEquals(true, conditionState.isPresence());

            final AbstractAlertState abstractSignalState = alertStateChangeList.get(1);
            assertTrue(abstractSignalState instanceof AlertSignalState);
            AlertSignalState signalState = (AlertSignalState) abstractSignalState;
            assertEquals(AlertSignalPresence.ON, signalState.getPresence());
        }

        {
            mdibSpy.reset();

            ReportListenerSpy reportListenerSpy = new ReportListenerSpy();

            SetString setString = new SetString();
            setString.setOperationHandleRef(VentilatorMdibRunner.HANDLE_SET_MDC_DEV_SYS_PT_VENT_VMD);
            setString.setRequestedStringValue(VentilatorMdibRunner.VentilatorMode.VENT_MODE_SIMV.getModeValue());
            final ListenableFuture<ScoTransaction<SetStringResponse>> scoFuture = sdcRemoteDevice.getSetServiceAccess()
                    .invoke(setString, reportListenerSpy, SetStringResponse.class);
            final ScoTransaction<SetStringResponse> scoTransaction = scoFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
            final var reportParts = scoTransaction.waitForFinalReport(WAIT_DURATION);
            assertFalse(reportParts.isEmpty());

            var expectedMdibVersion = lastInvocationMdibVersion.orElseThrow();
            assertEquals(expectedMdibVersion, reportParts.get(0).getValue());
            assertEquals(expectedMdibVersion, reportParts.get(1).getValue());
            assertEquals(expectedMdibVersion, reportParts.get(2).getValue());
            assertEquals(expectedMdibVersion.getVersion(), scoTransaction.getResponse().getMdibVersion());

            assertTrue(reportListenerSpy.waitForReports(3, WAIT_DURATION));
            assertEquals(InvocationState.WAIT, reportListenerSpy.getReports().get(0).getLeft().getInvocationInfo().getInvocationState());
            assertEquals(VentilatorMdibRunner.HANDLE_MDC_DEV_SYS_PT_VENT_MDS, reportListenerSpy.getReports().get(0).getLeft().getSourceMds());
            assertEquals(InvocationState.START, reportListenerSpy.getReports().get(1).getLeft().getInvocationInfo().getInvocationState());
            assertEquals(VentilatorMdibRunner.HANDLE_MDC_DEV_SYS_PT_VENT_MDS, reportListenerSpy.getReports().get(1).getLeft().getSourceMds());
            assertEquals(InvocationState.FIN, reportListenerSpy.getReports().get(2).getLeft().getInvocationInfo().getInvocationState());
            assertEquals(VentilatorMdibRunner.HANDLE_MDC_DEV_SYS_PT_VENT_MDS, reportListenerSpy.getReports().get(2).getLeft().getSourceMds());

            assertTrue(mdibSpy.waitForNumberOfRecordedMessages(1, WAIT_DURATION));
            final AbstractMdibAccessMessage recordedMessage = mdibSpy.getRecordedMessages().get(0);
            assertTrue(recordedMessage instanceof MetricStateModificationMessage);
            MetricStateModificationMessage metricStateMessage = (MetricStateModificationMessage) recordedMessage;
            assertEquals(1, metricStateMessage.getStates().size());
            var metricStateChangeList = metricStateMessage
                .getStates()
                .values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
            assertEquals(1, metricStateChangeList.size());
            final AbstractMetricState abstractMetricState = metricStateChangeList.get(0);
            assertTrue(abstractMetricState instanceof EnumStringMetricState);
            EnumStringMetricState stringMetricState = (EnumStringMetricState) abstractMetricState;
            assertEquals(VentilatorMdibRunner.VentilatorMode.VENT_MODE_SIMV.getModeValue(),
                    stringMetricState.getMetricValue().getValue());
        }

        {
            for (int i = 0; i < 100; ++i) {
                mdibSpy.reset();
                ventilatorMdibRunner.changeMetrics(null, BigDecimal.valueOf(i));

                assertTrue(mdibSpy.waitForNumberOfRecordedMessages(1, WAIT_DURATION));
                final AbstractMdibAccessMessage recordedMessage = mdibSpy.getRecordedMessages().get(0);
                assertTrue(recordedMessage instanceof MetricStateModificationMessage);
                MetricStateModificationMessage metricStateMessage = (MetricStateModificationMessage) recordedMessage;
                assertEquals(1, metricStateMessage.getStates().size());
                var metricStateChangeList = metricStateMessage.getStates().values().stream().findFirst().orElseThrow();
                assertEquals(1, metricStateChangeList.size());
                final AbstractMetricState abstractMetricState = metricStateChangeList.get(0);
                assertTrue(abstractMetricState instanceof NumericMetricState);
                NumericMetricState numericMetricState = (NumericMetricState) abstractMetricState;
                assertEquals(BigDecimal.valueOf(i), numericMetricState.getMetricValue().getValue());
            }
        }
    }

    @Test
    void connectMultipleClientsAndTransferData() throws Exception {
        int numberMessages = 100;
        int numberClients = 10;

        testDevice.startAsync().awaitRunning(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        List<TestSdcClient> testSdcClients = new ArrayList<>(numberClients);
        List<MdibAccessObserverSpy> mdibSpies = new ArrayList<>(numberClients);
        try {
            for (int i = 0; i < numberClients; ++i) {
                final TestSdcClient testSdcClient = new TestSdcClient();
                testSdcClients.add(testSdcClient);
                testSdcClient.startAsync();
                mdibSpies.add(new MdibAccessObserverSpy());
            }

            for (TestSdcClient testSdcClient : testSdcClients) {
                testSdcClient.awaitRunning(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
            }

            List<ListenableFuture<SdcRemoteDevice>> futures = new ArrayList<>(numberClients);
            testSdcClients.forEach(client -> {
                try {
                    final ListenableFuture<HostingServiceProxy> hostingServiceFuture = client.getClient()
                            .connect(testDevice.getSdcDevice().getEprAddress());
                    final HostingServiceProxy hostingServiceProxy = hostingServiceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

                    futures.add(client.getConnector().connect(hostingServiceProxy,
                            ConnectConfiguration.create(ConnectConfiguration.ALL_EPISODIC_AND_WAVEFORM_REPORTS)));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            for (int i = 0; i < numberClients; ++i) {
                final SdcRemoteDevice sdcRemoteDevice = futures.get(i).get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
                sdcRemoteDevice.getMdibAccessObservable().registerObserver(mdibSpies.get(i));
            }

            for (int j = 0; j < numberMessages; ++j) {
                ventilatorMdibRunner.changeMetrics(null, BigDecimal.valueOf(j));
            }

            mdibSpies.forEach(spy -> {
                assertTrue(spy.waitForNumberOfRecordedMessages(numberMessages, WAIT_DURATION));

                final List<AbstractMdibAccessMessage> recordedMessages = spy.getRecordedMessages();
                for (int j = 0; j < numberMessages; ++j) {
                    final AbstractMdibAccessMessage recordedMessage = recordedMessages.get(j);
                    assertTrue(recordedMessage instanceof MetricStateModificationMessage);
                    MetricStateModificationMessage metricStateMessage = (MetricStateModificationMessage) recordedMessage;
                    assertEquals(1, metricStateMessage.getStates().size());
                    var metricStateChangeList = metricStateMessage.getStates().values().stream().findFirst().orElseThrow();
                    assertEquals(1, metricStateChangeList.size());
                    final AbstractMetricState abstractMetricState = metricStateChangeList.get(0);
                    assertTrue(abstractMetricState instanceof NumericMetricState);
                    NumericMetricState numericMetricState = (NumericMetricState) abstractMetricState;
                    assertEquals(BigDecimal.valueOf(j), numericMetricState.getMetricValue().getValue());
                }
            });
        } finally {
            for (TestSdcClient client : testSdcClients) {
                client.stopAsync().awaitTerminated(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
            }
        }
    }

    @Test
    void connectMultipleDevicesAndTransferData() throws Exception {
        int numberMessagesPerDevice = 100;
        int numberDevices = 10;

        testClient.startAsync().awaitRunning(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        List<TestSdcDevice> testSdcDevices = new ArrayList<>(numberDevices);
        List<MdibAccessObserverSpy> mdibSpies = new ArrayList<>(numberDevices);
        try {
            List<VentilatorMdibRunner> ventilatorMdibRunners = new ArrayList<>(numberDevices);
            for (int i = 0; i < numberDevices; ++i) {
                final VentilatorMdibRunner ventilatorMdibRunner = new VentilatorMdibRunner(
                        IT.getInjector().getInstance(MdibXmlIo.class),
                        IT.getInjector().getInstance(ModificationsBuilderFactory.class));
                ventilatorMdibRunners.add(ventilatorMdibRunner);
            }

            for (int i = 0; i < numberDevices; ++i) {
                TestSdcDevice testSdcDevice = new TestSdcDevice(Collections.emptyList(),
                        Arrays.asList(ventilatorMdibRunners.get(i), IT.getInjector().getInstance(SdcRequiredTypesAndScopes.class)));
                testSdcDevices.add(testSdcDevice);
                testSdcDevice.startAsync();
                mdibSpies.add(new MdibAccessObserverSpy());
            }

            for (TestSdcDevice device : testSdcDevices) {
                device.awaitRunning(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
            }

            List<HostingServiceProxy> hostingServiceProxies = new ArrayList<>(numberDevices);
            testSdcDevices.forEach(testSdcDevice -> {
                var eprAddress = testSdcDevice.getSdcDevice().getEprAddress();
                try {
                    final ListenableFuture<HostingServiceProxy> hostingServiceFuture = testClient.getClient()
                            .connect(eprAddress);
                    hostingServiceProxies.add(hostingServiceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            List<ListenableFuture<SdcRemoteDevice>> futures = new ArrayList<>(numberDevices);
            hostingServiceProxies.forEach(hostingServiceProxy -> {
                try {
                    futures.add(testClient.getConnector().connect(hostingServiceProxy,
                            ConnectConfiguration.create(ConnectConfiguration.ALL_EPISODIC_AND_WAVEFORM_REPORTS)));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            for (int i = 0; i < numberDevices; ++i) {
                final SdcRemoteDevice sdcRemoteDevice = futures.get(i).get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
                sdcRemoteDevice.getMdibAccessObservable().registerObserver(mdibSpies.get(i));
            }

            ventilatorMdibRunners.forEach(ventilatorMdibRunner -> {
                try {
                    for (int j = 0; j < numberMessagesPerDevice; ++j) {
                        ventilatorMdibRunner.changeMetrics(null, BigDecimal.valueOf(j));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            mdibSpies.forEach(spy -> {
                assertTrue(spy.waitForNumberOfRecordedMessages(numberMessagesPerDevice, WAIT_DURATION));

                final List<AbstractMdibAccessMessage> recordedMessages = spy.getRecordedMessages();
                for (int j = 0; j < numberMessagesPerDevice; ++j) {
                    final AbstractMdibAccessMessage recordedMessage = recordedMessages.get(j);
                    assertTrue(recordedMessage instanceof MetricStateModificationMessage);
                    MetricStateModificationMessage metricStateMessage = (MetricStateModificationMessage) recordedMessage;
                    assertEquals(1, metricStateMessage.getStates().size());
                    var metricStateChangeList = metricStateMessage.getStates().values().stream().findFirst().orElseThrow();
                    assertEquals(1, metricStateChangeList.size());
                    final AbstractMetricState abstractMetricState = metricStateChangeList.get(0);
                    assertTrue(abstractMetricState instanceof NumericMetricState);
                    NumericMetricState numericMetricState = (NumericMetricState) abstractMetricState;
                    assertEquals(BigDecimal.valueOf(j), numericMetricState.getMetricValue().getValue());
                }
            });
        } finally {
            for (TestSdcDevice device : testSdcDevices) {
                device.stopAsync().awaitTerminated(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
            }
        }
    }

    @Test
    void connectOneClientAndTransferPeriodicEvents() throws Exception {
        testDevice.startAsync().awaitRunning(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
        testClient.startAsync().awaitRunning(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        var hostingServiceFuture = testClient.getClient()
                .connect(testDevice.getSdcDevice().getEprAddress());
        var hostingServiceProxy = hostingServiceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        var remoteDeviceFuture = testClient.getConnector().connect(hostingServiceProxy,
                ConnectConfiguration.create(ConnectConfiguration.ALL_PERIODIC_AND_WAVEFORM_REPORTS));

        var sdcRemoteDevice = remoteDeviceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
        var mdibSpy = new MdibAccessObserverSpy();
        sdcRemoteDevice.getMdibAccessObservable().registerObserver(mdibSpy);

        int periodicChangeCount = 5;
        new Thread(() -> {
            for (int i = 0; i < periodicChangeCount; ++i) {
                var modifications = MdibStateModifications.create(MdibStateModifications.Type.METRIC);
                try (var readTransaction = testDevice.getSdcDevice().getLocalMdibAccess().startTransaction()) {
                    VentilatorMdibRunner.changePeepValue(readTransaction, modifications, BigDecimal.valueOf(i));
                }
                assertDoesNotThrow(() -> {
                    var result = testDevice.getSdcDevice().getLocalMdibAccess().writeStates(modifications);
                    testDevice.getSdcDevice().sendPeriodicStateReport(result.getStates(), result.getMdibVersion());
                    Thread.sleep(100);
                });
            }
        }).start();

        assertTrue(mdibSpy.waitForNumberOfRecordedMessages(periodicChangeCount, WAIT_DURATION),
                String.format("Expected %s message(s), received %s", periodicChangeCount, mdibSpy.getRecordedMessages().size()));

        for (int i = 0; i < periodicChangeCount; ++i) {
            var recordedMessage = mdibSpy.getRecordedMessages().get(i);
            assertTrue(recordedMessage instanceof MetricStateModificationMessage);
            var metricStateMessage = (MetricStateModificationMessage) recordedMessage;
            assertEquals(1, metricStateMessage.getStates().size());
            var statesList = metricStateMessage
                .getStates()
                .values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
            var abstractMetricState = statesList.get(0);
            assertTrue(abstractMetricState instanceof NumericMetricState);
            var numericMetricState = (NumericMetricState) abstractMetricState;
            assertEquals(BigDecimal.valueOf(i), numericMetricState.getMetricValue().getValue());
        }
    }

    @Test
    void containmentTreeService() throws Exception {
        testDevice.startAsync().awaitRunning(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
        testClient.startAsync().awaitRunning(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        var hostingServiceFuture = testClient.getClient()
                .connect(testDevice.getSdcDevice().getEprAddress());
        var hostingServiceProxy = hostingServiceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        var remoteDeviceFuture = testClient.getConnector().connect(hostingServiceProxy,
                ConnectConfiguration.create(ConnectConfiguration.ALL_PERIODIC_AND_WAVEFORM_REPORTS));

        var sdcRemoteDevice = remoteDeviceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);


        var hostedServiceProxy = sdcRemoteDevice.getHostingServiceProxy().getHostedServices().get("HighPriorityServices");
        assertNotNull(hostedServiceProxy);
        var client = hostedServiceProxy.getRequestResponseClient();
        Function<String, QName> qName = name -> new QName(CommonConstants.NAMESPACE_PARTICIPANT, name);

        // Navigate down to metrics and check if containment tree responses are in accordance with Ventilator MDIB
        {
            var tree = getContainmentTree(client, Collections.emptyList());
            assertEquals(1, tree.getEntry().size());
            {
                var entry = tree.getEntry().get(0);
                assertEquals(VentilatorMdibRunner.HANDLE_MDC_DEV_SYS_PT_VENT_MDS, entry.getHandleRef());
                assertEquals(5, entry.getChildrenCount());
                assertEquals(CodedValueFactory.createIeeeCodedValue("70001", "MDC_DEV_SYS_PT_VENT_MDS"),
                        entry.getType());
                assertEquals(qName.apply("MdsDescriptor"), entry.getEntryType());
            }
        }

        {
            var tree = getContainmentTree(client, List.of(VentilatorMdibRunner.HANDLE_MDC_DEV_SYS_PT_VENT_MDS));
            assertEquals(5, tree.getEntry().size());
            {
                var entry = tree.getEntry().get(0);
                assertEquals(VentilatorMdibRunner.HANDLE_ALERT_SYSTEM, entry.getHandleRef());
                assertEquals(2, entry.getChildrenCount());
                assertNull(entry.getType());
                assertEquals(qName.apply("AlertSystemDescriptor"), entry.getEntryType());
            }
            {
                var entry = tree.getEntry().get(1);
                assertEquals(VentilatorMdibRunner.HANDLE_SCO, entry.getHandleRef());
                assertEquals(1, entry.getChildrenCount());
                assertNull(entry.getType());
                assertEquals(qName.apply("ScoDescriptor"), entry.getEntryType());
            }
            {
                var entry = tree.getEntry().get(2);
                assertEquals(VentilatorMdibRunner.HANDLE_SYSTEMCONTEXT, entry.getHandleRef());
                assertEquals(1, entry.getChildrenCount());
                assertNull(entry.getType());
                assertEquals(qName.apply("SystemContextDescriptor"), entry.getEntryType());
            }
            {
                var entry = tree.getEntry().get(3);
                assertEquals(VentilatorMdibRunner.HANDLE_CLOCK, entry.getHandleRef());
                assertEquals(0, entry.getChildrenCount());
                assertNull(entry.getType());
                assertEquals(qName.apply("ClockDescriptor"), entry.getEntryType());
            }
            {
                var entry = tree.getEntry().get(4);
                assertEquals(VentilatorMdibRunner.HANDLE_MDC_DEV_SYS_PT_VENT_VMD, entry.getHandleRef());
                assertEquals(1, entry.getChildrenCount());
                assertEquals(CodedValueFactory.createIeeeCodedValue("70002", "MDC_DEV_SYS_PT_VENT_VMD"),
                        entry.getType());
                assertEquals(qName.apply("VmdDescriptor"), entry.getEntryType());
            }
        }

        {
            var tree = getContainmentTree(client, List.of(
                    VentilatorMdibRunner.HANDLE_MDC_DEV_SYS_PT_VENT_VMD, "non-result-handle"));
            assertEquals(VentilatorMdibRunner.HANDLE_MDC_DEV_SYS_PT_VENT_VMD, tree.getHandleRef());
            assertEquals(VentilatorMdibRunner.HANDLE_MDC_DEV_SYS_PT_VENT_MDS, tree.getParentHandleRef());
            assertEquals(1, tree.getChildrenCount());
            assertEquals(qName.apply("VmdDescriptor"), tree.getEntryType());
            assertEquals(1, tree.getEntry().size());
            {
                var entry = tree.getEntry().get(0);
                assertEquals(VentilatorMdibRunner.HANDLE_MDC_DEV_SYS_PT_VENT_CHAN, entry.getHandleRef());
                assertEquals(VentilatorMdibRunner.HANDLE_MDC_DEV_SYS_PT_VENT_VMD, entry.getParentHandleRef());
                assertEquals(2, entry.getChildrenCount());
                assertEquals(CodedValueFactory.createIeeeCodedValue("70003", "MDC_DEV_SYS_PT_VENT_CHAN"),
                        entry.getType());
                assertEquals(qName.apply("ChannelDescriptor"), entry.getEntryType());
            }
        }

        {
            var tree = getContainmentTree(client, List.of(VentilatorMdibRunner.HANDLE_MDC_DEV_SYS_PT_VENT_CHAN));
            assertEquals(VentilatorMdibRunner.HANDLE_MDC_DEV_SYS_PT_VENT_CHAN, tree.getHandleRef());
            assertEquals(VentilatorMdibRunner.HANDLE_MDC_DEV_SYS_PT_VENT_VMD, tree.getParentHandleRef());
            assertEquals(2, tree.getChildrenCount());
            assertEquals(qName.apply("ChannelDescriptor"), tree.getEntryType());
            assertEquals(2, tree.getEntry().size());
            {
                var entry = tree.getEntry().get(0);
                assertEquals(VentilatorMdibRunner.HANDLE_MDC_VENT_MODE, entry.getHandleRef());
                assertEquals(VentilatorMdibRunner.HANDLE_MDC_DEV_SYS_PT_VENT_CHAN, entry.getParentHandleRef());
                assertEquals(VentilatorMdibRunner.HANDLE_MDC_DEV_SYS_PT_VENT_CHAN, entry.getParentHandleRef());
                assertEquals(0, entry.getChildrenCount());
                assertEquals(CodedValueFactory.createIeeeCodedValue("184352", "MDC_VENT_MODE"),
                        entry.getType());
                assertEquals(qName.apply("EnumStringMetricDescriptor"), entry.getEntryType());
            }
            {
                var entry = tree.getEntry().get(1);
                assertEquals(VentilatorMdibRunner.HANDLE_MDC_PRESS_AWAY_END_EXP_POS, entry.getHandleRef());
                assertEquals(VentilatorMdibRunner.HANDLE_MDC_DEV_SYS_PT_VENT_CHAN, entry.getParentHandleRef());
                assertEquals(0, entry.getChildrenCount());
                assertEquals(CodedValueFactory.createIeeeCodedValue("151804", "MDC_PRESS_AWAY_END_EXP_POS"),
                        entry.getType());
                assertEquals(qName.apply("NumericMetricDescriptor"), entry.getEntryType());
            }
        }

        var soapUtil = IT.getInjector().getInstance(SoapUtil.class);
        var getDescriptor = new GetDescriptor();
        var request = soapUtil.createMessage(ActionConstants.ACTION_GET_DESCRIPTOR, getDescriptor);
        getDescriptor.getHandleRef().addAll(List.of(
                VentilatorMdibRunner.HANDLE_MDC_DEV_SYS_PT_VENT_MDS,
                VentilatorMdibRunner.HANDLE_MDC_VENT_MODE,
                VentilatorMdibRunner.HANDLE_MDC_PRESS_AWAY_END_EXP_POS));

        var response = client.sendRequestResponse(request);
        soapUtil.getBody(response, GetDescriptorResponse.class).orElseThrow(() ->
                new RuntimeException("Containment tree response was empty")).getDescriptor();
    }

    private ContainmentTree getContainmentTree(RequestResponseClient client, List<String> handles)
            throws InterceptorException, SoapFaultException, MarshallingException, TransportException {
        var soapUtil = IT.getInjector().getInstance(SoapUtil.class);
        var getContainmentTree = new GetContainmentTree();
        var request = soapUtil.createMessage(ActionConstants.ACTION_GET_CONTAINMENT_TREE, getContainmentTree);
        getContainmentTree.getHandleRef().addAll(handles);
        var response = client.sendRequestResponse(request);
        return soapUtil.getBody(response, GetContainmentTreeResponse.class).orElseThrow(() ->
                new RuntimeException("Containment tree response was empty")).getContainmentTree();
    }
}
