package it.org.somda.glue;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import it.org.somda.glue.consumer.TestSdcClient;
import it.org.somda.glue.provider.TestSdcDevice;
import it.org.somda.glue.provider.VentilatorMdibRunner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.common.access.MdibAccessObserver;
import org.somda.sdc.biceps.common.event.DescriptionModificationMessage;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.model.message.InvocationError;
import org.somda.sdc.biceps.model.message.InvocationState;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.CommunicationLogImpl;
import org.somda.sdc.dpws.CommunicationLogSink;
import org.somda.sdc.dpws.factory.CommunicationLogFactory;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.MarshallingService;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.dpws.soap.wseventing.WsEventingConstants;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.glue.common.MdibXmlIo;
import org.somda.sdc.glue.common.factory.ModificationsBuilderFactory;
import org.somda.sdc.glue.consumer.ConnectConfiguration;
import org.somda.sdc.glue.consumer.PrerequisitesException;
import org.somda.sdc.glue.consumer.SdcRemoteDevice;
import org.somda.sdc.glue.provider.plugin.SdcRequiredTypesAndScopes;
import org.somda.sdc.glue.provider.sco.Context;
import org.somda.sdc.glue.provider.sco.IncomingSetServiceRequest;
import org.somda.sdc.glue.provider.sco.InvocationResponse;
import org.somda.sdc.glue.provider.sco.OperationInvocationReceiver;
import test.org.somda.common.CIDetector;
import test.org.somda.common.LoggingTestWatcher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(LoggingTestWatcher.class)
class SdcRemoteDevicesConnectorImplIT {

    private static final Logger LOG = LogManager.getLogger(CommunicationIT.class);
    private static final IntegrationTestUtil IT = new IntegrationTestUtil();

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
    private TestSdcDevice testDevice;
    private TestSdcClient testClient;
    private TestCommLogSink clientCommlog;
    private MarshallingService soapMarshalling;

    @BeforeEach
    void beforeEach() {
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

                context.sendSuccessfulReport(InvocationState.WAIT);
                context.sendSuccessfulReport(InvocationState.START);
                ventilatorMdibRunner.changeMetrics(match.get(), null);
                context.sendSuccessfulReport(InvocationState.FIN);
                return context.createSuccessfulResponse(InvocationState.FIN);
            }
        }), Arrays.asList(ventilatorMdibRunner, IT.getInjector().getInstance(SdcRequiredTypesAndScopes.class)));

        var override = new AbstractModule() {
            @Override
            protected void configure() {
                bind(CommunicationLogSink.class).to(TestCommLogSink.class).asEagerSingleton();
                install(new FactoryModuleBuilder()
                        .implement(CommunicationLog.class, CommunicationLogImpl.class)
                        .build(CommunicationLogFactory.class));
            }
        };
        testClient = new TestSdcClient(override);

        clientCommlog = (TestCommLogSink) testClient.getInjector().getInstance(CommunicationLogSink.class);

        soapMarshalling = testClient.getInjector().getInstance(MarshallingService.class);
    }

    @AfterEach
    void afterEach() throws TimeoutException {
        testDevice.stopAsync().awaitTerminated(WAIT_DURATION);
        testClient.stopAsync().awaitTerminated(WAIT_DURATION);
    }

    @Test
    @DisplayName("Ensure GetMdib is send after a SubscribeResponse message")
    void testGetMdibAfterSubscribe()
            throws TimeoutException, InterceptorException, ExecutionException, InterruptedException,
            MarshallingException, PrerequisitesException {
        testDevice.startAsync().awaitRunning(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
        testClient.startAsync().awaitRunning(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        final ListenableFuture<HostingServiceProxy> hostingServiceFuture = testClient.getClient()
                .connect(testDevice.getSdcDevice().getEprAddress());
        final HostingServiceProxy hostingServiceProxy = hostingServiceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        final ListenableFuture<SdcRemoteDevice> remoteDeviceFuture = testClient.getConnector().connect(hostingServiceProxy,
                ConnectConfiguration.create(ConnectConfiguration.ALL_EPISODIC_AND_WAVEFORM_REPORTS));

        final SdcRemoteDevice sdcRemoteDevice = remoteDeviceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        sdcRemoteDevice.stopAsync().awaitTerminated(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        // collect actions in order
        var actions = new ArrayList<String>();
        for (ByteArrayOutputStream byteArrayOutputStream : clientCommlog.getTraffic()) {
            var msg = soapMarshalling.unmarshal(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
            msg.getWsAddressingHeader().getAction().ifPresent(action -> actions.add(action.getValue()));
        }

        assertFalse(actions.isEmpty(), "No outbound actions found");
        var hadSubscribe = false;
        var hadGetMdib = false;
        for (String action : actions) {
            if (ActionConstants.ACTION_GET_MDIB.equals(action)) {
                assertTrue(hadSubscribe, "Subscribe did not occur before GetMdib");
                hadGetMdib = true;
            } else if (WsEventingConstants.WSA_ACTION_SUBSCRIBE_RESPONSE.equals(action)) {
                hadSubscribe = true;
            }
        }

        assertTrue(hadGetMdib, "Never saw any outgoing GetMdib");
        assertTrue(hadSubscribe, "Never saw any incoming SubscribeResponse");
    }

    @Test
    @DisplayName("Ensure MdibAccessObserver receives DescriptionModificationMessage after GetMdibResponse")
    void testMdibAccessObserverReceivesDescriptionModificationMessage()
            throws TimeoutException, InterceptorException, ExecutionException, InterruptedException,
            PrerequisitesException {
        // Given test device and test client...
        testDevice.startAsync().awaitRunning(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
        testClient.startAsync().awaitRunning(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
        final ListenableFuture<HostingServiceProxy> hostingServiceFuture = testClient.getClient()
                .connect(testDevice.getSdcDevice().getEprAddress());
        final HostingServiceProxy hostingServiceProxy = hostingServiceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        // ...and an MdibAccessObserver
        final AtomicInteger callCounter = new AtomicInteger();
        final MdibAccessObserver mdibAccessObserver = new MdibAccessObserver() {
            @Subscribe
            void onDescriptionModification(DescriptionModificationMessage message) {
                LOG.info("DescriptionModificationMessage received");
                callCounter.incrementAndGet();
            }
        };

        // When the client connects to the device, passing the MdibAccessObserver
        final ListenableFuture<SdcRemoteDevice> remoteDeviceFuture = testClient.getConnector().connect(hostingServiceProxy,
                ConnectConfiguration.create(ConnectConfiguration.ALL_EPISODIC_AND_WAVEFORM_REPORTS), mdibAccessObserver);
        final SdcRemoteDevice sdcRemoteDevice = remoteDeviceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
        sdcRemoteDevice.stopAsync().awaitTerminated(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        // Then a DescriptionModificationMessage is received by the MdibAccessObserver
        assertEquals(1, callCounter.get(), "MdibAccessObserver should receive exactly one DescriptionModificationMessage on device connect");
    }

    static class TestCommLogSink implements CommunicationLogSink {

        private final List<ByteArrayOutputStream> traffic;

        TestCommLogSink() {
            this.traffic = new ArrayList<>();
        }

        @Override
        public OutputStream createTargetStream(CommunicationLog.TransportType path,
                                               CommunicationLog.Direction direction,
                                               CommunicationLog.MessageType messageType,
                                               CommunicationContext communicationContext) {
            var os = new ByteArrayOutputStream();
            traffic.add(os);
            return os;
        }

        public List<ByteArrayOutputStream> getTraffic() {
            return traffic;
        }

        public void clear() {
            traffic.clear();
        }
    }

}
