package com.medstorm.sdcbridge;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.model.participant.NumericMetricState;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Periodically updates a few NumericMetricState values in the MDIB.
 * Uses reflection to be compatible with different SDCri versions.
 */
public final class MetricsFeeder implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(MetricsFeeder.class);

    private final LocalMdibAccess mdib;
    private final Injector injector; // optional (writer fallback)
    private ScheduledExecutorService ses;

    // <<< Make sure these handles exist in your MDIB >>>
    private final List<String> handles = List.of("nGAS", "TEMP", "HR", "SPO2", "RESP");

    public MetricsFeeder(Object mdib, Injector injector) {
        this.mdib = (LocalMdibAccess) mdib;
        this.injector = injector;
    }

    public void start() {
        if (ses != null) return;
        ses = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "metrics-feeder");
            t.setDaemon(true);
            return t;
        });
        ses.scheduleAtFixedRate(this::tick, 1, 2, TimeUnit.SECONDS);
        log.info("MetricsFeeder started ({} handles, every 2s).", handles.size());
    }

    @Override public void close() { stop(); }
    public void stop() { if (ses != null) { ses.shutdownNow(); ses = null; } }

    private void tick() {
        try {
            for (String h : handles) {
                Optional<NumericMetricState> opt = getStateEitherOrder(h);
                if (opt.isEmpty()) continue;
                NumericMetricState s = cloneState(opt.get());

                // toy value generator; replace with your real sensor reading
                double base = (Math.abs(h.hashCode() % 50) + 10);
                double v = base + ((System.nanoTime() % 1_000_000_000L) / 1e9) * 0.5;

                setMetricValueReflective(s, BigDecimal.valueOf(v));
                trySetActivationReflective(s, "ON");
                writeBackStates(Set.of(s));
            }
        } catch (Throwable t) {
            log.warn("MetricsFeeder tick failed: {}", t.toString());
        }
    }

    /** SDCri has both getState(Class,String) and getState(String,Class) across versions; try both. */
    private Optional<NumericMetricState> getStateEitherOrder(String handle) throws Exception {
        try {
            Method m = mdib.getClass().getMethod("getState", Class.class, String.class);
            @SuppressWarnings("unchecked")
            Optional<NumericMetricState> r = (Optional<NumericMetricState>) m.invoke(mdib, NumericMetricState.class, handle);
            return r;
        } catch (NoSuchMethodException ignored) {}

        Method m2 = mdib.getClass().getMethod("getState", String.class, Class.class);
        @SuppressWarnings("unchecked")
        Optional<NumericMetricState> r2 = (Optional<NumericMetricState>) m2.invoke(mdib, handle, NumericMetricState.class);
        return r2;
    }

    private NumericMetricState cloneState(NumericMetricState s) {
        try {
            Method m = s.getClass().getMethod("copy");
            return (NumericMetricState) m.invoke(s);
        } catch (Throwable ignore) {
            // many versions expose a mutable state; worst case we update in place
            return s;
        }
    }

    /** Prefer LocalMdibAccess#writeStates(Set) if present; else try older writer paths reflectively. */
    private void writeBackStates(Set<NumericMetricState> states) throws Exception {
        try {
            Method m = mdib.getClass().getMethod("writeStates", Set.class);
            m.invoke(mdib, states);
            return;
        } catch (NoSuchMethodException ignored) {}

        // Older SDCri releases used an MdibWriter; try that as a fallback (optional).
        try {
            Class<?> mwfClz = Class.forName("org.somda.sdc.biceps.provider.access.factory.MdibWriterFactory");
            Object factory = (injector != null) ? injector.getInstance(mwfClz) : null;
            if (factory != null) {
                Method create = null;
                for (String name : List.of("createMdibWriter", "createWriter")) {
                    try {
                        create = factory.getClass().getMethod(name, mdib.getClass().getInterfaces()[0]);
                        break;
                    } catch (NoSuchMethodException ignored2) {}
                }
                if (create != null) {
                    Object writer = create.invoke(factory, mdib);
                    try {
                        Method ws = writer.getClass().getMethod("writeStates", Set.class);
                        ws.invoke(writer, states);
                    } finally {
                        if (writer instanceof AutoCloseable ac) ac.close();
                    }
                    return;
                }
            }
        } catch (Throwable ignored) {}

        log.warn("No MDIB write path available (neither LocalMdibAccess#writeStates nor writer factory).");
    }

    private void setMetricValueReflective(Object state, BigDecimal value) {
        try {
            // ensure MetricValue object exists
            Method getMv = state.getClass().getMethod("getMetricValue");
            Object mv = getMv.invoke(state);
            if (mv == null) {
                Class<?> mvClz = Class.forName("org.somda.sdc.biceps.model.participant.MetricValue");
                mv = mvClz.getDeclaredConstructor().newInstance();
                Method setMv = state.getClass().getMethod("setMetricValue", mvClz);
                setMv.invoke(state, mv);
            }
            // mv.setValue(BigDecimal)
            try { mv.getClass().getMethod("setValue", BigDecimal.class).invoke(mv, value); } catch (NoSuchMethodException ignored) {}
            // mv.setTimestamp(Instant) if available
            try { mv.getClass().getMethod("setTimestamp", Instant.class).invoke(mv, Instant.now()); } catch (NoSuchMethodException ignored) {}
            // mv.setValidity(MeasuredValueValidity.VALID) if available
            try {
                @SuppressWarnings("unchecked")
                Class<? extends Enum> vClz = (Class<? extends Enum>) Class.forName("org.somda.sdc.biceps.model.participant.MeasuredValueValidity");
                Object VALID = Enum.valueOf(vClz, "VALID");
                mv.getClass().getMethod("setValidity", vClz).invoke(mv, VALID);
            } catch (Throwable ignored) {}
        } catch (Throwable t) {
            log.debug("setMetricValueReflective failed: {}", t.toString());
        }
    }

    private void trySetActivationReflective(Object state, String name) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Enum> aClz = (Class<? extends Enum>) Class.forName("org.somda.sdc.biceps.model.participant.MetricActivation");
            Object ON = Enum.valueOf(aClz, name);
            Method set = state.getClass().getMethod("setActivationState", aClz);
            set.invoke(state, ON);
        } catch (Throwable ignored) {}
    }
}
