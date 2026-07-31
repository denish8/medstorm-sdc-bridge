package com.medstorm.sdc.core.mdib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.access.WriteStateResult;
import org.somda.sdc.biceps.model.participant.AbstractMetricState;
import org.somda.sdc.biceps.model.participant.AbstractMetricValue;
import org.somda.sdc.biceps.model.participant.ComponentActivation;
import org.somda.sdc.biceps.model.participant.GenerationMode;
import org.somda.sdc.biceps.model.participant.MeasurementValidity;
import org.somda.sdc.biceps.model.participant.NumericMetricState;
import org.somda.sdc.biceps.model.participant.NumericMetricValue;
import org.somda.sdc.biceps.model.participant.StringMetricState;
import org.somda.sdc.biceps.model.participant.StringMetricValue;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Writes metric values into a provider's MDIB, which is what causes SDC-ri to
 * emit an {@code EpisodicMetricReport} to every subscribed consumer.
 *
 * <p>Values written together in one {@link Batch} are committed as a single MDIB
 * transaction and therefore produce <em>one</em> report rather than one per
 * metric. For a device publishing several correlated values at a fixed rate this
 * matters: five separate writes at 1&nbsp;Hz means five reports per second per
 * subscriber, and consumers see the values arrive at slightly different MDIB
 * versions as if they were independent observations.
 *
 * <pre>{@code
 * MetricWriter writer = new MetricWriter(localMdibAccess);
 * writer.batch()
 *       .numeric("numeric.pain", 4)
 *       .numeric("numeric.awk",  62)
 *       .numeric("numeric.nbv",  2)
 *       .commit();                     // -> one EpisodicMetricReport
 * }</pre>
 *
 * <p>Thread-safe: each {@link Batch} is independent, and the underlying
 * {@code LocalMdibAccess} serialises writes. A {@code Batch} itself is not
 * intended to be shared across threads.
 */
public final class MetricWriter {
    private static final Logger LOG = LoggerFactory.getLogger(MetricWriter.class);

    private final LocalMdibAccess mdib;
    private final GenerationMode generationMode;

    /**
     * Creates a writer that marks values as real measurements
     * ({@link GenerationMode#REAL}).
     */
    public MetricWriter(LocalMdibAccess mdib) {
        this(mdib, GenerationMode.REAL);
    }

    /**
     * @param generationMode reported to consumers in each value's metric quality.
     *                       Use {@link GenerationMode#DEMO} for simulated data so
     *                       a consumer can tell it apart from patient data.
     */
    public MetricWriter(LocalMdibAccess mdib, GenerationMode generationMode) {
        this.mdib = Objects.requireNonNull(mdib, "mdib");
        this.generationMode = Objects.requireNonNull(generationMode, "generationMode");
    }

    /**
     * Starts a batch stamped with the current time.
     */
    public Batch batch() {
        return new Batch(Instant.now());
    }

    /**
     * Starts a batch stamped with the time the measurement was actually taken.
     *
     * <p>Prefer this when the reading carries its own timestamp: the determination
     * time consumers see should be when the device measured, not when the bridge
     * got round to writing.
     */
    public Batch batch(Instant determinationTime) {
        return new Batch(Objects.requireNonNull(determinationTime, "determinationTime"));
    }

    /** Convenience for a single numeric write. */
    public WriteStateResult numeric(String handle, BigDecimal value) throws MetricWriteException {
        return batch().numeric(handle, value).commit();
    }

    /** Convenience for a single numeric write. */
    public WriteStateResult numeric(String handle, double value) throws MetricWriteException {
        return batch().numeric(handle, value).commit();
    }

    /** Convenience for a single string write. */
    public WriteStateResult string(String handle, String value) throws MetricWriteException {
        return batch().string(handle, value).commit();
    }

    /**
     * A set of metric values committed together as one MDIB transaction.
     */
    public final class Batch {
        private final Instant determinationTime;
        private final List<PendingWrite> pending = new ArrayList<>();

        private Batch(Instant determinationTime) {
            this.determinationTime = determinationTime;
        }

        public Batch numeric(String handle, double value) {
            return numeric(handle, BigDecimal.valueOf(value));
        }

        public Batch numeric(String handle, BigDecimal value) {
            return numeric(handle, value, MeasurementValidity.VLD);
        }

        /**
         * Writes a numeric value with explicit validity.
         *
         * <p>Use this to publish a reading the device knows is untrustworthy &mdash;
         * {@link MeasurementValidity#INV} for a bad-signal condition, for example
         * &mdash; rather than suppressing the write. A consumer that receives no
         * update cannot distinguish "sensor detached" from "network down"; one that
         * receives an invalid-flagged value can.
         */
        public Batch numeric(String handle, BigDecimal value, MeasurementValidity validity) {
            Objects.requireNonNull(handle, "handle");
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(validity, "validity");
            pending.add(new PendingWrite(handle, Kind.NUMERIC, value, null, validity));
            return this;
        }

        public Batch string(String handle, String value) {
            return string(handle, value, MeasurementValidity.VLD);
        }

        public Batch string(String handle, String value, MeasurementValidity validity) {
            Objects.requireNonNull(handle, "handle");
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(validity, "validity");
            pending.add(new PendingWrite(handle, Kind.STRING, null, value, validity));
            return this;
        }

        /**
         * Commits every value in this batch as one MDIB transaction.
         *
         * @return the write result, or {@code null} if the batch was empty.
         * @throws MetricWriteException.UnknownHandleException if any handle is absent
         *         from the MDIB. Nothing is written in that case &mdash; the batch is
         *         all-or-nothing.
         * @throws MetricWriteException if the MDIB rejected the transaction.
         */
        public WriteStateResult commit() throws MetricWriteException {
            Set<String> unknown = new LinkedHashSet<>();
            List<AbstractMetricState> resolved = resolve(unknown);
            if (!unknown.isEmpty()) {
                throw new MetricWriteException.UnknownHandleException(unknown);
            }
            return write(resolved);
        }

        /**
         * Commits the values whose handles exist, skipping the rest.
         *
         * <p>For callers that publish a superset of metrics across device variants and
         * genuinely expect some handles to be absent. The skipped handles are returned
         * so they can be logged or counted &mdash; do not discard them silently.
         *
         * @return the handles that were not present in the MDIB, empty if all resolved.
         */
        public Set<String> commitIgnoringUnknownHandles() throws MetricWriteException {
            Set<String> unknown = new LinkedHashSet<>();
            List<AbstractMetricState> resolved = resolve(unknown);
            write(resolved);
            if (!unknown.isEmpty()) {
                LOG.debug("Skipped {} unknown metric handle(s): {}", unknown.size(), unknown);
            }
            return unknown;
        }

        private List<AbstractMetricState> resolve(Set<String> unknownSink) {
            List<AbstractMetricState> resolved = new ArrayList<>(pending.size());
            for (PendingWrite w : pending) {
                // CopyMdibOutput is enabled, so getState hands back a detached copy that
                // is safe to mutate before writing it back.
                if (w.kind == Kind.NUMERIC) {
                    Optional<NumericMetricState> state = mdib.getState(w.handle, NumericMetricState.class);
                    if (state.isEmpty()) {
                        unknownSink.add(w.handle);
                        continue;
                    }
                    resolved.add(applyNumeric(state.get(), w));
                } else {
                    Optional<StringMetricState> state = mdib.getState(w.handle, StringMetricState.class);
                    if (state.isEmpty()) {
                        unknownSink.add(w.handle);
                        continue;
                    }
                    resolved.add(applyString(state.get(), w));
                }
            }
            return resolved;
        }

        private WriteStateResult write(List<AbstractMetricState> states) throws MetricWriteException {
            if (states.isEmpty()) {
                return null;
            }
            // 6.0.0 models this as a Kotlin sealed type: one subtype per report
            // category, constructed from the full list, rather than 5.x's
            // create(Type)/add() builder.
            MdibStateModifications modifications = new MdibStateModifications.Metric(states);
            try {
                return mdib.writeStates(modifications);
            } catch (Exception e) {
                throw new MetricWriteException(
                        "MDIB rejected a metric transaction of " + states.size() + " state(s): " + e, e);
            }
        }

        private NumericMetricState applyNumeric(NumericMetricState state, PendingWrite w) {
            NumericMetricValue value = state.getMetricValue();
            if (value == null) {
                value = new NumericMetricValue();
                state.setMetricValue(value);
            }
            value.setValue(w.numericValue);
            stamp(value, w.validity);
            state.setActivationState(ComponentActivation.ON);
            return state;
        }

        private StringMetricState applyString(StringMetricState state, PendingWrite w) {
            StringMetricValue value = state.getMetricValue();
            if (value == null) {
                value = new StringMetricValue();
                state.setMetricValue(value);
            }
            value.setValue(w.stringValue);
            stamp(value, w.validity);
            state.setActivationState(ComponentActivation.ON);
            return state;
        }

        private void stamp(AbstractMetricValue value, MeasurementValidity validity) {
            value.setDeterminationTime(determinationTime);
            AbstractMetricValue.MetricQuality quality = value.getMetricQuality();
            if (quality == null) {
                quality = new AbstractMetricValue.MetricQuality();
                value.setMetricQuality(quality);
            }
            // Set on every write, not just when absent: validity is per-reading, and a
            // stale VLD left over from the previous value would misreport a bad sample.
            quality.setValidity(validity);
            quality.setMode(generationMode);
        }
    }

    private enum Kind { NUMERIC, STRING }

    private static final class PendingWrite {
        final String handle;
        final Kind kind;
        final BigDecimal numericValue;
        final String stringValue;
        final MeasurementValidity validity;

        PendingWrite(String handle, Kind kind, BigDecimal numericValue, String stringValue,
                     MeasurementValidity validity) {
            this.handle = handle;
            this.kind = kind;
            this.numericValue = numericValue;
            this.stringValue = stringValue;
            this.validity = validity;
        }
    }
}
