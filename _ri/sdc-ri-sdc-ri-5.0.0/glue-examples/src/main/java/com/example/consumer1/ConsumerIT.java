package com.example.consumer1;

import com.example.Constants;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.ContextAssociation;
import org.somda.sdc.biceps.model.participant.LocationContextState;
import org.somda.sdc.biceps.model.participant.LocationDetail;
import org.somda.sdc.biceps.model.participant.PatientContextState;
import org.somda.sdc.dpws.client.DiscoveredDevice;
import org.somda.sdc.dpws.client.DiscoveryObserver;
import org.somda.sdc.dpws.client.event.ProbedDeviceFoundMessage;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.dpws.soap.wsdiscovery.MatchBy;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryUtil;
import org.somda.sdc.glue.common.FallbackInstanceIdentifier;
import org.somda.sdc.glue.common.uri.LocationDetailQueryMapper;
import org.somda.sdc.glue.consumer.ConnectConfiguration;
import org.somda.sdc.glue.consumer.PrerequisitesException;
import org.somda.sdc.glue.consumer.SdcDiscoveryFilterBuilder;
import org.somda.sdc.glue.consumer.SdcRemoteDevice;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.example.consumer1.Consumer.invokeActivate;
import static com.example.consumer1.Consumer.invokeSetString;
import static com.example.consumer1.Consumer.invokeSetValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * This consumer is meant to be used for integration tests with other SDC libraries.
 */
public class ConsumerIT {
    private static final Logger LOG = LogManager.getLogger(ConsumerIT.class);
    private static final Duration MAX_WAIT = Duration.ofSeconds(11);
    private static String[] args;

    private static Duration reportTimeout;
    private static String targetFacility;
    private static String targetBed;
    private static String targetPoC;
    private ConsumerReportProcessor reportObs;

    @BeforeAll
    static void setUp() {
        targetFacility = System.getenv().getOrDefault("ref_fac", Constants.DEFAULT_FACILITY);
        targetBed = System.getenv().getOrDefault("ref_bed", Constants.DEFAULT_BED);
        targetPoC = System.getenv().getOrDefault("ref_poc", Constants.DEFAULT_POC);
        reportTimeout = Duration.ofSeconds(
                Long.parseLong(
                        System.getProperty("reportTimeout", Constants.DEFAULT_REPORT_TIMEOUT)
                )
        );
    }

    @Test
    void runIT() throws Exception {
        var settings = new ConsumerUtil(args);
        var consumer = new Consumer(settings);
        consumer.startUp();

        var targetEpr = discoverDevice(consumer);
        assert targetEpr != null;
        var hostingService = connectDevice(consumer, targetEpr);
        var remoteDevice = connectMdibAndSubscribe(consumer, hostingService);
        verifyContexts(remoteDevice);
        verifyReports();
        verifyOperationInvocation(remoteDevice);

        // shut down
        remoteDevice.getMdibAccessObservable().unregisterObserver(reportObs);
        remoteDevice.stopAsync().awaitTerminated();

        consumer.shutDown();
    }

    private String discoverDevice(Consumer consumer) throws Exception {

        var wsdUtil = consumer.getInjector().getInstance(WsDiscoveryUtil.class);

        var location = new LocationDetail();
        location.setFacility(targetFacility);
        location.setBed(targetBed);
        location.setPoC(targetPoC);

        var instanceIdentifierOpt = FallbackInstanceIdentifier.create(location);
        assertTrue(instanceIdentifierOpt.isPresent());

        var query = LocationDetailQueryMapper.createWithLocationDetailQuery(
                instanceIdentifierOpt.get(),
                location
        );

        LOG.info("Starting discovery for location {}", query);
        final SettableFuture<String> targetEpr = SettableFuture.create();
        DiscoveryObserver obs = new DiscoveryObserver() {
            @Subscribe
            void deviceFound(ProbedDeviceFoundMessage message) {
                DiscoveredDevice payload = message.getPayload();
                if (wsdUtil.isScopesMatching(payload.getScopes(), List.of(query), MatchBy.RFC3986)) {
                    LOG.info("Found device with epr {}", payload.getEprAddress());
                    targetEpr.set(payload.getEprAddress());
                } else {
                    LOG.info("Found non-matching device with epr {}", payload.getEprAddress());
                }
            }
        };
        consumer.getClient().registerDiscoveryObserver(obs);

        // filter discovery for SDC devices only
        SdcDiscoveryFilterBuilder discoveryFilterBuilder = SdcDiscoveryFilterBuilder.create();
        discoveryFilterBuilder.addScope(query);
        consumer.getClient().probe(discoveryFilterBuilder.get());

        try {
            return targetEpr.get(MAX_WAIT.toSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            targetEpr.cancel(true);
            LOG.error("Couldn't find target with location {} after {}s", location, MAX_WAIT.toSeconds(), e);
            fail(String.format("Couldn't find target with location %s after %s", location, MAX_WAIT.toSeconds()), e);
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldn't find target with location {}", location, e);
            fail("Couldn't find target with location " + location, e);
        } finally {
            consumer.getClient().unregisterDiscoveryObserver(obs);
        }
        return null;
    }

    HostingServiceProxy connectDevice(Consumer consumer, String targetEpr) throws InterceptorException {
        LOG.info("Connecting to {}", targetEpr);

        var hostingServiceFuture = consumer.getClient().connect(targetEpr);

        HostingServiceProxy hostingServiceProxy = null;
        try {
            hostingServiceProxy = hostingServiceFuture.get(MAX_WAIT.toSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            LOG.error("Couldn't connect to EPR {} after {}s", targetEpr, MAX_WAIT.toSeconds(), e);
            fail(String.format("Couldn't connect to EPR %s after %ss", targetEpr, MAX_WAIT.toSeconds()), e);
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldn't connect to EPR {}", targetEpr, e);
            fail("Couldn't connect to EPR " + targetEpr, e);
        }
        return hostingServiceProxy;
    }

    SdcRemoteDevice connectMdibAndSubscribe(Consumer consumer, HostingServiceProxy hostingServiceProxy) {
        LOG.info("Attaching to remote mdib and subscriptions");
        ListenableFuture<SdcRemoteDevice> remoteDeviceFuture = null;
        SdcRemoteDevice sdcRemoteDevice = null;
        try {
            remoteDeviceFuture = consumer.getConnector()
                .connect(
                    hostingServiceProxy,
                    ConnectConfiguration.create(ConnectConfiguration.ALL_EPISODIC_AND_WAVEFORM_REPORTS)
                );
            sdcRemoteDevice = remoteDeviceFuture.get(MAX_WAIT.toSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            remoteDeviceFuture.cancel(true);
            LOG.error("Couldn't attach to remote mdib and subscriptions after {}s", MAX_WAIT.toSeconds(), e);
            fail(String.format("Couldn't attach to remote mdib and subscriptions after %ss", MAX_WAIT.toSeconds()), e);
        } catch (PrerequisitesException | InterruptedException | ExecutionException e) {
            LOG.error("Couldn't attach to remote mdib and subscriptions", e);
            fail("Couldn't attach to remote mdib and subscriptions", e);
        }

        // attach report listener
        reportObs = new ConsumerReportProcessor();
        sdcRemoteDevice.getMdibAccessObservable().registerObserver(reportObs);
        return sdcRemoteDevice;
    }

    void verifyContexts(SdcRemoteDevice sdcRemoteDevice) {
        // verify that provider has at least one patient and location context attached
        List<AbstractContextState> contextStates = sdcRemoteDevice.getMdibAccess().getContextStates();

        // has patient
        long numPatientContexts = contextStates.stream()
                .filter(x -> PatientContextState.class.isAssignableFrom(x.getClass()))
                .filter(x -> ContextAssociation.ASSOC.equals(x.getContextAssociation()))
                .count();
        assertTrue(numPatientContexts >= 1, "No associated patient context found");
        // has location context
        long numLocationContexts = contextStates.stream()
                .filter(x -> LocationContextState.class.isAssignableFrom(x.getClass()))
                .filter(x -> ContextAssociation.ASSOC.equals(x.getContextAssociation()))
                .filter(x -> targetFacility.equals(((LocationContextState) x).getLocationDetail().getFacility()))
                .count();
        assertTrue(numLocationContexts >= 1, "No associated location context matching discovery data found");
    }

    void verifyReports() throws InterruptedException {

        // wait for incoming reports
        Thread.sleep(reportTimeout.toMillis());

        // expected number of reports given 5 second interval
        int minNumberReports = (int) (reportTimeout.dividedBy(Duration.ofSeconds(5))) - 1;

        // verify the number of reports for the expected metrics is at least five during the timeout
        assertTrue(
                reportObs.getMetricChanges().values().stream().anyMatch(changes -> changes >= minNumberReports),
                "Did not receive metric reports, expected at least "
                        + minNumberReports
                        + " but received "
                        + reportObs.getMetricChanges()
                        + " instead."
        );
        assertTrue(
                reportObs.getConditionChanges().values().stream().anyMatch(changes -> changes >= minNumberReports),
                "Did not receive alert condition reports, expected at least "
                        + minNumberReports
                        + " but received "
                        + reportObs.getConditionChanges()
                        + " instead."
        );
    }

    void verifyOperationInvocation(SdcRemoteDevice sdcRemoteDevice) {

        // invoke all target operations
        var setServiceAccess = sdcRemoteDevice.getSetServiceAccess();

        boolean operationFailed = false;
        try {
            invokeSetString(setServiceAccess, Constants.HANDLE_SET_STRING, "SDCri was here");
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            operationFailed = true;
            LOG.error("Could not invoke {}", Constants.HANDLE_SET_STRING, e);
        }
        try {
            invokeSetString(setServiceAccess, Constants.HANDLE_SET_STRING_ENUM, "OFF");
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            operationFailed = true;
            LOG.error("Could not invoke {}", Constants.HANDLE_SET_STRING_ENUM, e);
        }
        try {
            invokeSetValue(setServiceAccess, Constants.HANDLE_SET_VALUE, BigDecimal.valueOf(20));
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            operationFailed = true;
            LOG.error("Could not invoke {}", Constants.HANDLE_SET_VALUE, e);
        }
        try {
            invokeActivate(setServiceAccess, Constants.HANDLE_ACTIVATE, Collections.emptyList());
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            operationFailed = true;
            LOG.error("Could not invoke {}", Constants.HANDLE_ACTIVATE, e);
        }
        assertFalse(operationFailed, "Operation invocation failed unexpectedly, check the log");

        LOG.info("Done, quitting");
    }

    public static void main(String[] args) {

        ConsumerIT.args = args;

        final LauncherDiscoveryRequest request =
                LauncherDiscoveryRequestBuilder.request()
                        .selectors(selectClass(ConsumerIT.class))
                        .build();

        final Launcher launcher = LauncherFactory.create();
        final SummaryGeneratingListener listener = new SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        TestExecutionSummary summary = listener.getSummary();
        List<TestExecutionSummary.Failure> failures = summary.getFailures();
        LOG.info("getTestsSucceededCount() - {}", summary.getTestsSucceededCount());
        failures.forEach(failure -> LOG.error("failure", failure.getException()));

        System.exit(!failures.isEmpty() ? 1 : 0);
    }
}
