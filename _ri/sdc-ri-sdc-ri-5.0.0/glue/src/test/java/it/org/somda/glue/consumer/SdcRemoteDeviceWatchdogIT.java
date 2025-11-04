package it.org.somda.glue.consumer;

import com.google.common.util.concurrent.ListenableFuture;
import it.org.somda.glue.IntegrationTestUtil;
import it.org.somda.glue.provider.TestSdcDevice;
import it.org.somda.glue.provider.VentilatorMdibRunner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.common.event.AbstractMdibAccessMessage;
import org.somda.sdc.biceps.common.event.MetricStateModificationMessage;
import org.somda.sdc.biceps.model.participant.AbstractMetricState;
import org.somda.sdc.biceps.model.participant.NumericMetricState;
import org.somda.sdc.biceps.testutil.MdibAccessObserverSpy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.glue.common.MdibXmlIo;
import org.somda.sdc.glue.common.factory.ModificationsBuilderFactory;
import org.somda.sdc.glue.consumer.ConnectConfiguration;
import org.somda.sdc.glue.consumer.SdcRemoteDevice;
import org.somda.sdc.glue.provider.plugin.SdcRequiredTypesAndScopes;
import test.org.somda.common.CIDetector;
import test.org.somda.common.LoggingTestWatcher;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(LoggingTestWatcher.class)
class SdcRemoteDeviceWatchdogIT {
    private static final Logger LOG = LogManager.getLogger(SdcRemoteDeviceWatchdogIT.class);
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

    @BeforeEach
    void beforeEach(TestInfo testInfo) {
        LOG.info("Running test case {}", testInfo.getDisplayName());
        ventilatorMdibRunner = new VentilatorMdibRunner(
                IT.getInjector().getInstance(MdibXmlIo.class),
                IT.getInjector().getInstance(ModificationsBuilderFactory.class));

        testDevice = new TestSdcDevice(
                Collections.emptyList(),
                Arrays.asList(
                        ventilatorMdibRunner,
                        IT.getInjector().getInstance(SdcRequiredTypesAndScopes.class)
                )
        );

        testClient = new TestSdcClient();
    }

    @AfterEach
    void afterEach(TestInfo testInfo) {
        LOG.info("Done with test case {}", testInfo.getDisplayName());
        testDevice.stopAsync().awaitTerminated();
        testClient.stopAsync().awaitTerminated();
    }

    @Test
    void testRenew() throws Exception {

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

        var subscriptions = testDevice.getSdcDevice().getActiveSubscriptions();
        // there should only be one subscription, get it
        assertEquals(1, subscriptions.size());

        var subscription = subscriptions.entrySet().iterator().next().getValue();

        assertEquals(TestSdcClient.REQUESTED_EXPIRES, subscription.getExpires());

        var expiresTime = subscription.getExpiresTimeout();

        // wait enough time to ensure a renew must've happened
        Thread.sleep(TestSdcClient.REQUESTED_EXPIRES.toMillis());

        // we must be AFTER the expires moment now
        var now = Instant.now();
        assertTrue(now.isAfter(expiresTime));

        // and reports must still arrive after the timeout has expired
        for (int i = 0; i < 10; ++i) {
            mdibSpy.reset();
            ventilatorMdibRunner.changeMetrics(null, BigDecimal.valueOf(i));

            assertTrue(mdibSpy.waitForNumberOfRecordedMessages(1, WAIT_DURATION));
            final AbstractMdibAccessMessage recordedMessage = mdibSpy.getRecordedMessages().get(0);
            assertTrue(recordedMessage instanceof MetricStateModificationMessage);
            MetricStateModificationMessage metricStateMessage = (MetricStateModificationMessage) recordedMessage;
            assertEquals(1, metricStateMessage.getStates().size());
            var metricStateList = metricStateMessage
                .getStates()
                .values()
                .stream()
                .flatMap(it -> it.stream())
                .collect(Collectors.toList());
            final AbstractMetricState abstractMetricState = metricStateList.get(0);
            assertTrue(abstractMetricState instanceof NumericMetricState);
            NumericMetricState numericMetricState = (NumericMetricState) abstractMetricState;
            assertEquals(BigDecimal.valueOf(i), numericMetricState.getMetricValue().getValue());
        }

    }

}
