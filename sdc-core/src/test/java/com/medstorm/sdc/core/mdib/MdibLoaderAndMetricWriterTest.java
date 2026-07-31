package com.medstorm.sdc.core.mdib;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import com.medstorm.sdcbridge.MedstormProviderOnlyGlueModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.somda.sdc.biceps.guice.DefaultBicepsConfigModule;
import org.somda.sdc.biceps.guice.DefaultBicepsModule;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.guice.DefaultDpwsModule;
import org.somda.sdc.biceps.model.participant.MeasurementValidity;
import org.somda.sdc.biceps.model.participant.NumericMetricState;
import org.somda.sdc.biceps.model.participant.StringMetricState;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.provider.access.factory.LocalMdibAccessFactory;
import org.somda.sdc.common.guice.DefaultCommonConfigModule;
import org.somda.sdc.common.guice.DefaultCommonModule;
import org.somda.sdc.glue.guice.DefaultGlueConfigModule;
import org.somda.sdc.glue.guice.DefaultGlueModule;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the MDIB load -> write -> read-back path against a real BICEPS
 * object graph. No network, no DPWS: just the parts that decide whether a
 * consumer would see anything.
 */
class MdibLoaderAndMetricWriterTest {

    private static final String MDIB_RESOURCE = "mdib/example-vitals-mdib.xml";

    private static Injector injector;

    private LocalMdibAccess mdib;
    private MetricWriter writer;

    @BeforeAll
    static void createInjector() {
        // The smallest composition that can read an MDIB and hold it provider-side.
        // Three things are non-obvious and cost time to discover:
        //   - DefaultCommonModule does NOT install DefaultCommonConfigModule, so
        //     Common.InstanceIdentifier is unbound and MdibXmlIo cannot be created.
        //   - CommonConfig.NAMESPACE_MAPPINGS is bound by DefaultGlueConfigModule,
        //     not by the common module its name suggests.
        //   - DefaultGlueModule.configureConsumer eagerly wires the consumer graph,
        //     which drags in DpwsFramework and SoapMessageFactory. Overriding
        //     SdcRemoteDevicesConnector with a no-op is what keeps a provider-only
        //     deployment from needing the whole DPWS stack.
        // Every Default*Module has a separate Default*ConfigModule that it does not
        // install itself; all of them must be listed or the graph fails at creation time.
        // DefaultDpwsModule is required even though this test never opens a socket:
        // Glue's provider bindings reference DeviceFactory, HostedServiceFactory and
        // SoapMarshalling. It only binds - nothing starts until DpwsFramework does.
        injector = Guice.createInjector(
                new DefaultCommonModule(),
                new DefaultCommonConfigModule(),
                new DefaultBicepsModule(),
                new DefaultBicepsConfigModule(),
                new DefaultDpwsConfigModule(),
                new DefaultDpwsModule(),
                new DefaultGlueConfigModule(),
                Modules.override(new DefaultGlueModule()).with(new MedstormProviderOnlyGlueModule()));
    }

    @BeforeEach
    void loadMdib() throws Exception {
        mdib = injector.getInstance(LocalMdibAccessFactory.class).createLocalMdibAccess();
        MdibLoader.from(injector).loadFromClasspath(mdib, MDIB_RESOURCE);
        writer = new MetricWriter(mdib);
    }

    @Test
    @DisplayName("an empty MDIB has no metric states until one is loaded")
    void emptyMdibHasNoStates() {
        LocalMdibAccess empty = injector.getInstance(LocalMdibAccessFactory.class).createLocalMdibAccess();
        assertTrue(empty.getState("numeric.hr", NumericMetricState.class).isEmpty(),
                "a freshly created LocalMdibAccess must be empty - this is the state the provider "
                        + "served before MdibLoader existed");
    }

    @Test
    @DisplayName("loading inserts descriptors and single states, including ones absent from MdState")
    void loadPopulatesStates() {
        assertTrue(mdib.getState("numeric.hr", NumericMetricState.class).isPresent());
        assertTrue(mdib.getState("numeric.spo2", NumericMetricState.class).isPresent());
        assertTrue(mdib.getState("numeric.temp", NumericMetricState.class).isPresent());
        assertTrue(mdib.getState("string.status", StringMetricState.class).isPresent());

        // The fixture declares no <pm:State> for the MDS, VMD, channel or contexts;
        // createSingleStateIfMissing must have generated them.
        assertFalse(mdib.getEntity("mds0").isEmpty(), "MDS entity should exist after load");
        assertFalse(mdib.getEntity("ch.vitals").isEmpty(), "channel entity should exist after load");
    }

    @Test
    @DisplayName("a numeric write is readable back with value, time, validity and activation set")
    void numericWriteRoundTrips() throws Exception {
        Instant when = Instant.parse("2026-01-02T03:04:05Z");

        writer.batch(when).numeric("numeric.hr", 72).commit();

        NumericMetricState state = mdib.getState("numeric.hr", NumericMetricState.class).orElseThrow();
        assertNotNull(state.getMetricValue(), "metric value must be created if absent");
        assertEquals(0, BigDecimal.valueOf(72).compareTo(state.getMetricValue().getValue()));
        assertEquals(when, state.getMetricValue().getDeterminationTime(),
                "determination time must be the measurement time, not the write time");
        assertNotNull(state.getMetricValue().getMetricQuality());
        assertEquals(MeasurementValidity.VLD, state.getMetricValue().getMetricQuality().getValidity());
        assertNotNull(state.getActivationState(), "activation state must be set so consumers treat the metric as live");
    }

    @Test
    @DisplayName("a string write round-trips")
    void stringWriteRoundTrips() throws Exception {
        writer.string("string.status", "READY");

        StringMetricState state = mdib.getState("string.status", StringMetricState.class).orElseThrow();
        assertNotNull(state.getMetricValue());
        assertEquals("READY", state.getMetricValue().getValue());
    }

    @Test
    @DisplayName("several metrics commit as one transaction and all land")
    void batchWritesAllValues() throws Exception {
        writer.batch()
                .numeric("numeric.hr", 61)
                .numeric("numeric.spo2", 98)
                .numeric("numeric.temp", new BigDecimal("36.7"))
                .string("string.status", "MEASURING")
                .commit();

        assertEquals(0, BigDecimal.valueOf(61).compareTo(
                mdib.getState("numeric.hr", NumericMetricState.class).orElseThrow().getMetricValue().getValue()));
        assertEquals(0, BigDecimal.valueOf(98).compareTo(
                mdib.getState("numeric.spo2", NumericMetricState.class).orElseThrow().getMetricValue().getValue()));
        assertEquals(0, new BigDecimal("36.7").compareTo(
                mdib.getState("numeric.temp", NumericMetricState.class).orElseThrow().getMetricValue().getValue()));
        assertEquals("MEASURING",
                mdib.getState("string.status", StringMetricState.class).orElseThrow().getMetricValue().getValue());
    }

    @Test
    @DisplayName("writing to a handle the MDIB does not describe fails loudly")
    void unknownHandleThrows() {
        var ex = assertThrows(MetricWriteException.UnknownHandleException.class,
                () -> writer.numeric("numeric.does-not-exist", 1));
        assertEquals(Set.of("numeric.does-not-exist"), ex.getHandles());
    }

    @Test
    @DisplayName("a batch containing an unknown handle writes nothing at all")
    void batchIsAllOrNothing() {
        assertThrows(MetricWriteException.UnknownHandleException.class, () -> writer.batch()
                .numeric("numeric.hr", 123)
                .numeric("numeric.nope", 1)
                .commit());

        var hr = mdib.getState("numeric.hr", NumericMetricState.class).orElseThrow();
        assertTrue(hr.getMetricValue() == null || hr.getMetricValue().getValue() == null,
                "the valid value in a rejected batch must not have been written");
    }

    @Test
    @DisplayName("lenient commit writes what it can and reports what it skipped")
    void lenientCommitReportsSkippedHandles() throws Exception {
        Set<String> skipped = writer.batch()
                .numeric("numeric.hr", 55)
                .numeric("numeric.nope", 1)
                .commitIgnoringUnknownHandles();

        assertEquals(Set.of("numeric.nope"), skipped);
        assertEquals(0, BigDecimal.valueOf(55).compareTo(
                mdib.getState("numeric.hr", NumericMetricState.class).orElseThrow().getMetricValue().getValue()));
    }

    @Test
    @DisplayName("validity is set per reading, so a bad sample does not inherit VLD")
    void validityIsPerReading() throws Exception {
        writer.batch().numeric("numeric.hr", 70).commit();
        writer.batch().numeric("numeric.hr", new BigDecimal("0"), MeasurementValidity.INV).commit();

        var quality = mdib.getState("numeric.hr", NumericMetricState.class)
                .orElseThrow().getMetricValue().getMetricQuality();
        assertEquals(MeasurementValidity.INV, quality.getValidity(),
                "validity must be overwritten on every write, not only when absent");
    }

    @Test
    @DisplayName("a missing MDIB resource is reported rather than silently ignored")
    void missingResourceThrows() {
        LocalMdibAccess fresh = injector.getInstance(LocalMdibAccessFactory.class).createLocalMdibAccess();
        assertThrows(MdibLoadException.class,
                () -> MdibLoader.from(injector).loadFromClasspath(fresh, "mdib/no-such-file.xml"));
    }
}
