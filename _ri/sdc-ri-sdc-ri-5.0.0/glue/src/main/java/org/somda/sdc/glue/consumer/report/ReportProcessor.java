package org.somda.sdc.glue.consumer.report;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.message.AbstractReport;
import org.somda.sdc.biceps.model.message.GetContextStatesResponse;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.AutoLock;
import org.somda.sdc.glue.common.MdibVersionUtil;
import org.somda.sdc.glue.consumer.ConsumerConfig;
import org.somda.sdc.glue.consumer.report.helper.ReportWriter;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.net.URI;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Class that is responsible for buffering and processing of incoming reports.
 * <p>
 * As long as there is no MDIB (plus optional context states) available, the report processor buffers incoming reports
 * and only applies them once an MDIB (plus optional context states) was set.
 * In case every report type is received through one subscription, the {@linkplain ReportProcessor} ensures data
 * coherency.
 */
public class ReportProcessor extends AbstractIdleService {
    private static final Logger LOG = LogManager.getLogger(ReportProcessor.class);

    private final ReentrantLock mdibReadyLock;
    private final Condition mdibReadyCondition;
    private final MdibVersionUtil mdibVersionUtil;
    private final ReportWriter reportWriter;
    private final BlockingQueue<AbstractReport> bufferedReports;
    private final Logger instanceLogger;
    private final Boolean applyReportsSameMdibVersion;

    private AtomicBoolean bufferingRequested;
    private RemoteMdibAccess mdibAccess;
    private Consumer<AbstractReport> writeReport;

    @Inject
    ReportProcessor(ReentrantLock mdibReadyLock,
                    MdibVersionUtil mdibVersionUtil,
                    ReportWriter reportWriter,
                    @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                    @Named(ConsumerConfig.APPLY_REPORTS_SAME_MDIB_VERSION) Boolean applyReportsSameMdibVersion) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.mdibReadyLock = mdibReadyLock;
        this.mdibReadyCondition = mdibReadyLock.newCondition();
        this.mdibVersionUtil = mdibVersionUtil;
        this.reportWriter = reportWriter;
        this.bufferedReports = new ArrayBlockingQueue<>(500); // todo make queue size configurable
        this.mdibAccess = null;
        this.bufferingRequested = new AtomicBoolean(true);
        this.applyReportsSameMdibVersion = applyReportsSameMdibVersion;

        startAsync().awaitRunning();
        initMdibAccessWait();
    }

    /**
     * Queues or processes a report.
     * <p>
     * In case no MDIB was set via {@link #startApplyingReportsOnMdib(RemoteMdibAccess, GetContextStatesResponse)} yet,
     * this function queues incoming reports.
     * Once {@link #startApplyingReportsOnMdib(RemoteMdibAccess, GetContextStatesResponse)} was called, reports are
     * directly applied on the injected {@link RemoteMdibAccess} instance.
     * <p>
     * As soon as the {@linkplain ReportProcessor} is shut down, no reports will be processed henceforth.
     *
     * @param report the report to process.
     * @param <T>    any report type in terms of the SDC protocol.
     */
    public <T extends AbstractReport> void processReport(T report) {
        try (var ignored = AutoLock.lock(mdibReadyLock)) {
            this.writeReport.accept(report);
        }
    }

    /**
     * Accepts an MDIB and starts applying reports on it.
     *
     * @param mdibAccess            the MDIB access to use for application of incoming reports.
     * @param contextStatesResponse optionally a get-context-states response message in case
     *                              a get-mdib request did not returned some.
     * @throws PreprocessingException    is thrown in case writing to the MDIB fails.
     * @throws ReportProcessingException is thrown in case there is any error during processing of a report or
     *                                   accessing data from the queue.
     */
    public void startApplyingReportsOnMdib(RemoteMdibAccess mdibAccess,
                                           @Nullable GetContextStatesResponse contextStatesResponse)
            throws PreprocessingException, ReportProcessingException {
        try (var ignored = AutoLock.lock(mdibReadyLock)) {
            if (this.mdibAccess != null) {
                instanceLogger.warn("Tried to invoke startApplyingReportsOnMdib() multiple times. " +
                        "Make sure to call it only once. " +
                        "Invocation ignored.");
                return;
            }
        }

        applyOptionalContextStates(mdibAccess, contextStatesResponse);

        applyReportsFromBuffer(mdibAccess);
        bufferingRequested.set(false);
        applyReportsFromBuffer(mdibAccess);

        try (var ignored = AutoLock.lock(mdibReadyLock)) {
            this.mdibAccess = mdibAccess;
            mdibReadyCondition.signalAll();
        }
    }

    private void applyReportsFromBuffer(RemoteMdibAccess mdibAccess)
            throws PreprocessingException, ReportProcessingException {
        while (!bufferedReports.isEmpty()) {
            try {
                applyReportOnMdib(bufferedReports.take(), mdibAccess);
            } catch (InterruptedException e) {
                throw new ReportProcessingException("Could not take an element from the queue though expected");
            }
        }
    }

    private void applyReportOnMdib(AbstractReport report) throws PreprocessingException, ReportProcessingException {
        applyReportOnMdib(report, mdibAccess);
    }

    private void applyReportOnMdib(AbstractReport report, RemoteMdibAccess mdibAccess)
            throws PreprocessingException, ReportProcessingException {
        final MdibVersion mdibAccessMdibVersion = mdibAccess.getMdibVersion();
        final MdibVersion reportMdibVersion = mdibVersionUtil.getMdibVersion(report);
        if (!mdibAccessMdibVersion.getSequenceId().equals(reportMdibVersion.getSequenceId()) ||
                !equalsInstanceIds(mdibAccessMdibVersion.getInstanceId(), reportMdibVersion.getInstanceId())) {
            throw new ReportProcessingException(String.format("MDIB version from MDIB (%s) and " +
                            "MDIB version from report (%s) do not match",
                    mdibAccessMdibVersion, reportMdibVersion));
        }

        if (mdibAccessMdibVersion.getVersion().compareTo(reportMdibVersion.getVersion()) == 0) {
            LOG.debug("Received a second report with Mdib Version {}", reportMdibVersion.getVersion());
            if (!applyReportsSameMdibVersion) {
                return;
            }
        } else if (mdibAccessMdibVersion.getVersion().compareTo(reportMdibVersion.getVersion()) > 0) {
            LOG.debug(
                    "Received a report older than current mdib. Mdib {}, report {}",
                    mdibAccessMdibVersion.getVersion(), reportMdibVersion.getVersion()
            );
            return;
        }

        reportWriter.write(report, mdibAccess);
    }

    private void initMdibAccessWait() {
        this.writeReport = report -> {
            if (bufferingRequested.get()) {
                bufferedReports.add(report);
                return;
            }

            try (var ignored = AutoLock.lock(mdibReadyLock)) {
                if (mdibAccess == null) {
                    if (!mdibReadyCondition.await(10000, TimeUnit.MILLISECONDS)) {
                        throw new TimeoutException("No MDIB access set within 10s");
                    }
                    if (mdibAccess == null) {
                        throw new NullPointerException("Expected MDIB access to be set, but was null");
                    }
                }

                applyReportOnMdib(report);
                writeReport = abstractReport -> {
                    try {
                        applyReportOnMdib(abstractReport);
                    } catch (PreprocessingException | ReportProcessingException e) {
                        throw new RuntimeException(e);
                    }
                };
            } catch (InterruptedException | PreprocessingException | TimeoutException | ReportProcessingException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private void applyOptionalContextStates(RemoteMdibAccess mdibAccess,
                                            @Nullable GetContextStatesResponse contextStatesResponse)
            throws PreprocessingException, ReportProcessingException {
        if (contextStatesResponse == null) {
            return;
        }

        final MdibVersion mdibVersion = mdibAccess.getMdibVersion();
        if (!URI.create(mdibVersion.getSequenceId()).equals(URI.create(contextStatesResponse.getSequenceId())) ||
                !equalsInstanceIds(mdibVersion.getInstanceId(), contextStatesResponse.getInstanceId())) {
            throw new ReportProcessingException(String.format("Received context state report belongs to " +
                            "different MDIB sequence/instance. Expected: %s, actual: %s",
                    mdibVersionUtil.getMdibVersion(contextStatesResponse), mdibVersion));
        }

        if (mdibVersion.getVersion().compareTo(contextStatesResponse.getMdibVersion()) > 0) {
            instanceLogger.warn("Found a context state response whose MDIB version ({}) is older " +
                            "than the MDIB's MDIB version ({})",
                    contextStatesResponse.getMdibVersion(), mdibVersion.getVersion());
            return;
        }

        final MdibStateModifications modifications = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT)
                .addAll(contextStatesResponse.getContextState());
        mdibAccess.writeStates(mdibVersion, modifications);
    }

    /**
     * Compares two InstanceIds for equality.
     *
     * This equals check is necessary, since InstanceId has a implied value of 0 and the report might actually have the
     * field set to null.
     *
     * @param mdibVersionInstanceId the mdibVersion instance id
     * @param reportInstanceId the report instance id, implied value always set to 0
     * @return true if equal, false otherwise
     */
    private boolean equalsInstanceIds(BigInteger mdibVersionInstanceId, @Nullable BigInteger reportInstanceId) {
        return reportInstanceId == null ? mdibVersionInstanceId.equals(BigInteger.ZERO) : mdibVersionInstanceId.equals(reportInstanceId);
    }

    @Override
    protected void startUp() {
        // nothing to be done
    }

    @Override
    protected void shutDown() {
        try (var ignored = AutoLock.lock(mdibReadyLock)) {
            writeReport = abstractReport -> {
                // stop processing reports by doing nothing
            };
        }
    }
}
