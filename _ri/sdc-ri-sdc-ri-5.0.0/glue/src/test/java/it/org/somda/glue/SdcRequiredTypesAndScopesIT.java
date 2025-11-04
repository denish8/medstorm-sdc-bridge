package it.org.somda.glue;

import com.google.common.base.Joiner;
import com.google.common.eventbus.Subscribe;
import it.org.somda.glue.consumer.TestSdcClient;
import it.org.somda.glue.provider.TestSdcDevice;
import it.org.somda.glue.provider.VentilatorMdibRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.access.MdibAccessObserver;
import org.somda.sdc.biceps.common.event.ContextStateModificationMessage;
import org.somda.sdc.biceps.common.event.DescriptionModificationMessage;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.model.message.InvocationError;
import org.somda.sdc.biceps.model.message.InvocationState;
import org.somda.sdc.biceps.model.participant.LocationDetail;
import org.somda.sdc.biceps.model.participant.MdsDescriptor;
import org.somda.sdc.biceps.model.participant.factory.CodedValueFactory;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.client.DiscoveredDevice;
import org.somda.sdc.dpws.client.DiscoveryObserver;
import org.somda.sdc.dpws.client.event.DeviceEnteredMessage;
import org.somda.sdc.glue.GlueConstants;
import org.somda.sdc.glue.common.CommonConstants;
import org.somda.sdc.glue.common.uri.ComplexDeviceComponentMapper;
import org.somda.sdc.glue.common.FallbackInstanceIdentifier;
import org.somda.sdc.glue.common.MdibXmlIo;
import org.somda.sdc.glue.common.factory.ModificationsBuilderFactory;
import org.somda.sdc.glue.common.uri.LocationDetailQueryMapper;
import org.somda.sdc.glue.common.uri.UriMapperGenerationArgumentException;
import org.somda.sdc.glue.provider.SdcDeviceContext;
import org.somda.sdc.glue.provider.SdcDevicePlugin;
import org.somda.sdc.glue.provider.plugin.ScopesDecorator;
import org.somda.sdc.glue.provider.plugin.SdcRequiredTypesAndScopes;
import org.somda.sdc.glue.provider.sco.Context;
import org.somda.sdc.glue.provider.sco.IncomingSetServiceRequest;
import org.somda.sdc.glue.provider.sco.InvocationResponse;
import org.somda.sdc.glue.provider.sco.OperationInvocationReceiver;
import test.org.somda.common.CIDetector;
import test.org.somda.common.LoggingTestWatcher;
import test.org.somda.common.TimedWait;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(LoggingTestWatcher.class)
class SdcRequiredTypesAndScopesIT {
    private static final Logger LOG = LogManager.getLogger(SdcRequiredTypesAndScopesIT.class);
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
    void beforeEach() {
        ventilatorMdibRunner = new VentilatorMdibRunner(
                IT.getInjector().getInstance(MdibXmlIo.class),
                IT.getInjector().getInstance(ModificationsBuilderFactory.class));
        setupDevice();
        testClient = new TestSdcClient();
    }

    @Test
    void checkRequiredTypesAndScopesOnUpdate() throws Exception {
        testDevice.startAsync().awaitRunning();
        testClient.startAsync().awaitRunning();

        var discoveryObserverSpy = new DiscoveryObserverSpy();
        testClient.getClient().registerDiscoveryObserver(discoveryObserverSpy);
        LocationDetail locationDetail = new LocationDetail();
        locationDetail.setFacility("facility");
        var instanceIdentifier = FallbackInstanceIdentifier.create(locationDetail);
        assertTrue(instanceIdentifier.isPresent());
        var expectedLocationScope = LocationDetailQueryMapper.createWithLocationDetailQuery(instanceIdentifier.get(),
                locationDetail);

        var mdibAccess = testDevice.getSdcDevice().getMdibAccess();
        var mds = mdibAccess.getDescriptor(VentilatorMdibRunner.HANDLE_MDC_DEV_SYS_PT_VENT_MDS, MdsDescriptor.class);
        assertTrue(mds.isPresent());
        var expectedMdsScope = ComplexDeviceComponentMapper.fromComplexDeviceComponent(mds.get());

        {
            // Test initial types and scopes
            var resolveFuture = testClient.getClient().resolve(testDevice.getSdcDevice().getEprAddress());
            var discoveredDevice = resolveFuture.get(WAIT_IN_SECONDS, TimeUnit.SECONDS);
            var actualTypes = discoveredDevice.getTypes();
            var actualScopes = discoveredDevice.getScopes();

            var expectedTypes = Arrays.asList(DpwsConstants.DEVICE_TYPE, CommonConstants.MEDICAL_DEVICE_TYPE);
            var expectedScopes = Arrays.asList(GlueConstants.SCOPE_SDC_PROVIDER.toString(),
                    expectedMdsScope.toString());

            verifyCollection(expectedTypes, actualTypes);
            verifyCollection(expectedScopes, actualScopes);
        }

        {
            ventilatorMdibRunner.changeLocation(locationDetail);

            assertTrue(discoveryObserverSpy.waitForEnteredDevices(1));

            var expectedScopes = Arrays.asList(GlueConstants.SCOPE_SDC_PROVIDER.toString(),
                    expectedLocationScope.toString(), expectedMdsScope.toString());
            var actualScopes = discoveryObserverSpy.getEnteredDevices().get(0).getScopes();
            verifyCollection(expectedScopes, actualScopes);
        }

        {

            var modifications = MdibDescriptionModifications.create();

            mds.get().setType(CodedValueFactory.createIeeeCodedValue("70002"));
            modifications.update(mds.get());
            mdibAccess.writeDescription(modifications);

            assertTrue(discoveryObserverSpy.waitForEnteredDevices(2));

            expectedMdsScope = ComplexDeviceComponentMapper.fromComplexDeviceComponent(mds.get());
            var expectedScopes = Arrays.asList(GlueConstants.SCOPE_SDC_PROVIDER.toString(),
                    expectedLocationScope.toString(), expectedMdsScope.toString());
            var actualScopes = discoveryObserverSpy.getEnteredDevices().get(1).getScopes();
            verifyCollection(expectedScopes, actualScopes);
        }
    }

    @Test
    void checkAdditionalScopesOnUpdate() throws PreprocessingException, UriMapperGenerationArgumentException {
        // Overwrite device and ventilator runner
        ventilatorMdibRunner = new VentilatorMdibRunner(
                IT.getInjector().getInstance(MdibXmlIo.class),
                IT.getInjector().getInstance(ModificationsBuilderFactory.class));
        setupDevice(Collections.singleton(new MyScopesUpdater(IT.getInjector()
                .getInstance(SdcRequiredTypesAndScopes.class))));

        testDevice.startAsync().awaitRunning();
        testClient.startAsync().awaitRunning();

        var discoveryObserverSpy = new DiscoveryObserverSpy();
        testClient.getClient().registerDiscoveryObserver(discoveryObserverSpy);
        LocationDetail locationDetail = new LocationDetail();
        locationDetail.setFacility("facility");
        var instanceIdentifier = FallbackInstanceIdentifier.create(locationDetail);
        assertTrue(instanceIdentifier.isPresent());
        var expectedLocationScope = LocationDetailQueryMapper.createWithLocationDetailQuery(instanceIdentifier.get(),
                locationDetail);

        var mdibAccess = testDevice.getSdcDevice().getMdibAccess();
        var mds = mdibAccess.getDescriptor(VentilatorMdibRunner.HANDLE_MDC_DEV_SYS_PT_VENT_MDS, MdsDescriptor.class);
        assertTrue(mds.isPresent());
        var expectedMdsScope = ComplexDeviceComponentMapper.fromComplexDeviceComponent(mds.get());

        {
            ventilatorMdibRunner.changeLocation(locationDetail);

            assertTrue(discoveryObserverSpy.waitForEnteredDevices(1),
                    () -> "Count for entered devices was " + discoveryObserverSpy.getEnteredDevices().size());

            var expectedScopes = Arrays.asList(GlueConstants.SCOPE_SDC_PROVIDER.toString(),
                    expectedLocationScope.toString(), expectedMdsScope.toString(),
                    MyScopesUpdater.SCOPE_ON_CONTEXT_UPDATE.toString());
            var actualScopes = discoveryObserverSpy.getEnteredDevices().get(0).getScopes();
            verifyCollection(expectedScopes, actualScopes);
        }

        {

            var modifications = MdibDescriptionModifications.create();

            mds.get().setType(CodedValueFactory.createIeeeCodedValue("70002"));
            modifications.update(mds.get());
            mdibAccess.writeDescription(modifications);

            assertTrue(discoveryObserverSpy.waitForEnteredDevices(2));

            expectedMdsScope = ComplexDeviceComponentMapper.fromComplexDeviceComponent(mds.get());
            var expectedScopes = Arrays.asList(GlueConstants.SCOPE_SDC_PROVIDER.toString(),
                    expectedLocationScope.toString(), expectedMdsScope.toString(),
                    MyScopesUpdater.SCOPE_ON_DESCRIPTION_UPDATE.toString());
            var actualScopes = discoveryObserverSpy.getEnteredDevices().get(1).getScopes();
            verifyCollection(expectedScopes, actualScopes);
        }
    }

    private void setupDevice() {
        setupDevice(null);
    }

    private void setupDevice(@Nullable Collection<SdcDevicePlugin> sdcDevicePlugins) {
        var customizedPlugins = new ArrayList<SdcDevicePlugin>();
        customizedPlugins.add(ventilatorMdibRunner);
        if (sdcDevicePlugins == null) {
            customizedPlugins.add(IT.getInjector().getInstance(SdcRequiredTypesAndScopes.class));
        } else {
            customizedPlugins.addAll(sdcDevicePlugins);
        }

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
        }), customizedPlugins);
    }

    private <T> void verifyCollection(Collection<T> expectedCollection, Collection<T> actualCollection) {
        Supplier<String> derivationTextSupplier = () ->
                String.format("Expected != Actual items: [%s] != [%s]",
                        Joiner.on(",").join(expectedCollection),
                        Joiner.on(",").join(actualCollection));

        assertEquals(expectedCollection.size(), actualCollection.size(), derivationTextSupplier);
        int matchCount = 0;
        for (var expected : expectedCollection) {
            for (var actual : actualCollection) {
                matchCount += expected.equals(actual) ? 1 : 0;
            }
        }
        assertEquals(expectedCollection.size(), matchCount, derivationTextSupplier);
    }


    private static class DiscoveryObserverSpy implements DiscoveryObserver {
        private final List<DiscoveredDevice> enteredDevices = new ArrayList<>();
        private final TimedWait<List<DiscoveredDevice>> timedWait;

        DiscoveryObserverSpy() {
            timedWait = new TimedWait<>(ArrayList::new);
        }

        List<DiscoveredDevice> getEnteredDevices() {
            return timedWait.getData();
        }

        boolean waitForEnteredDevices(int enteredDevicesCount) {
            return timedWait.waitForData(enteredDevices -> enteredDevices.size() == enteredDevicesCount,
                    SdcRequiredTypesAndScopesIT.WAIT_DURATION);
        }

        @Subscribe
        void onHello(DeviceEnteredMessage deviceEnteredMessage) {
            timedWait.modifyData(discoveredDevices -> discoveredDevices.add(deviceEnteredMessage.getPayload()));
        }
    }

    private static class MyScopesUpdater implements SdcDevicePlugin, MdibAccessObserver {
        private final ScopesDecorator scopesDecorator;

        static final String SCOPE_ON_CONTEXT_UPDATE = "http://context-update";
        static final String SCOPE_ON_DESCRIPTION_UPDATE = "http://description-update";

        MyScopesUpdater(ScopesDecorator scopesDecorator) {
            this.scopesDecorator = scopesDecorator;
        }

        @Override
        public void beforeStartUp(SdcDeviceContext context) throws Exception {
            scopesDecorator.init(context, Collections.emptySet());
            context.getLocalMdibAccess().registerObserver(this);
        }

        @Subscribe
        private void onContextChange(ContextStateModificationMessage message) {
            scopesDecorator.appendScopesAndSendHello(Collections.singleton(SCOPE_ON_CONTEXT_UPDATE));
        }

        @Subscribe
        private void onDescriptionChange(DescriptionModificationMessage message) {
            scopesDecorator.appendScopesAndSendHello(Collections.singleton(SCOPE_ON_DESCRIPTION_UPDATE));
        }
    }
}
