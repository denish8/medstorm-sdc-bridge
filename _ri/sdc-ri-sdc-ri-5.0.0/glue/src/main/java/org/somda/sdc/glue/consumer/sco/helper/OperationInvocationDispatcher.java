package org.somda.sdc.glue.consumer.sco.helper;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.message.AbstractSetResponse;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.glue.consumer.ConsumerConfig;
import org.somda.sdc.glue.consumer.helper.HostingServiceLogger;
import org.somda.sdc.glue.consumer.sco.ScoTransaction;
import org.somda.sdc.glue.consumer.sco.ScoTransactionImpl;
import org.somda.sdc.glue.consumer.sco.ScoUtil;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * Helper class to dispatch incoming operation invoked report parts to {@linkplain ScoTransaction} objects.
 */
public class OperationInvocationDispatcher {
    private static final Logger LOG = LogManager.getLogger(OperationInvocationDispatcher.class);
    private final ScoUtil scoUtil;
    private final Duration awaitingTransactionTimeout;

    private final Map<Long, BlockingQueue<Pair<OperationInvokedReport.ReportPart, MdibVersion>>> pendingReports;
    private final Map<Long, ScoTransactionImpl<? extends AbstractSetResponse>> runningTransactions;
    private final Map<Long, Instant> awaitingTransactions;
    private final Logger instanceLogger;

    @Inject
    OperationInvocationDispatcher(@Assisted HostingServiceProxy hostingServiceProxy,
                                  ScoUtil scoUtil,
                                  @Named(ConsumerConfig.AWAITING_TRANSACTION_TIMEOUT)
                                          Duration awaitingTransactionTimeout,
                                  @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = HostingServiceLogger.getLogger(LOG, hostingServiceProxy, frameworkIdentifier);
        this.scoUtil = scoUtil;
        this.awaitingTransactionTimeout = awaitingTransactionTimeout;
        this.pendingReports = new HashMap<>();
        this.runningTransactions = new HashMap<>();
        this.awaitingTransactions = new HashMap<>();
    }

    /**
     * Accepts a report and dispatches its report parts to registered SCO transactions.
     * <p>
     * By using {@link #registerTransaction(ScoTransactionImpl)}, SCO transactions can be registered to get notified
     * on incoming operation invoked report parts.
     * If an SCO transaction is not registered by the time the first report pops up, reports are going to be buffered.
     * Buffered "dead" reports are sanitized on each incoming report (see
     * {@link org.somda.sdc.glue.consumer.ConsumerConfig#AWAITING_TRANSACTION_TIMEOUT}).
     *
     * @param report the report to process.
     */
    public synchronized void dispatchReport(OperationInvokedReport report) {
        report.getReportPart()
            .forEach(part -> this.dispatchReport(
                new ImmutablePair<>(
                    part,
                    new MdibVersion(
                        report.getSequenceId(),
                        report.getMdibVersion() != null ? report.getMdibVersion() : BigInteger.ZERO,
                        report.getInstanceId() != null ? report.getInstanceId() : BigInteger.ZERO
                    )
                )
            ));
    }

    /**
     * Registers an SCO transaction and delivers buffered reports immediately.
     * <p>
     * Once a final report is registered, the allocated heap used for the transaction is erased.
     *
     * @param transaction the transaction to
     */
    public synchronized void registerTransaction(ScoTransactionImpl<? extends AbstractSetResponse> transaction) {
        long transactionId = transaction.getTransactionId();
        final ScoTransaction<? extends AbstractSetResponse> runningTransaction = runningTransactions.get(transactionId);
        if (runningTransaction != null) {
            instanceLogger.warn("Try to add transaction {} twice, which is not permitted", transactionId);
            return;
        }

        awaitingTransactions.remove(transactionId);
        runningTransactions.put(transaction.getTransactionId(), transaction);
        BlockingQueue<Pair<OperationInvokedReport.ReportPart, MdibVersion>> reportPartsQueue = pendingReports.get(transactionId);

        if (reportPartsQueue != null) {
            applyReportsOnTransaction(reportPartsQueue, transaction);
        }
    }

    private void dispatchReport(Pair<OperationInvokedReport.ReportPart, MdibVersion> reportPart) {

        final long transactionId = reportPart.getLeft().getInvocationInfo().getTransactionId();

        sanitizeAwaitingTransactions();

        final BlockingQueue<Pair<OperationInvokedReport.ReportPart, MdibVersion>> guardedQueue = pendingReports.get(transactionId);
        BlockingQueue<Pair<OperationInvokedReport.ReportPart, MdibVersion>> reportPartsQueue;
        if (guardedQueue == null) {
            reportPartsQueue = new LinkedBlockingQueue<>(3); // 3 bc 1 wait, one started, one finished
            pendingReports.put(transactionId, reportPartsQueue);
            awaitingTransactions.put(transactionId, Instant.now());
        } else {
            reportPartsQueue = guardedQueue;
        }

        ScoTransactionImpl<? extends AbstractSetResponse> transaction = runningTransactions.get(transactionId);
        if (scoUtil.isFinalReport(reportPart.getLeft())) {
            runningTransactions.remove(transactionId);
        }
        if (!reportPartsQueue.offer(reportPart)) {
            instanceLogger.warn("Too many reports received for transaction {}", transactionId);
            return;
        }

        if (transaction != null) {
            applyReportsOnTransaction(reportPartsQueue, transaction);
        }
    }

    private void applyReportsOnTransaction(BlockingQueue<Pair<OperationInvokedReport.ReportPart, MdibVersion>> queue,
                                           ScoTransactionImpl<? extends AbstractSetResponse> transaction) {
        while (!queue.isEmpty()) {
            try {
                final Pair<OperationInvokedReport.ReportPart, MdibVersion> reportFromQueue = queue.take();
                transaction.receiveIncomingReport(reportFromQueue);
            } catch (InterruptedException e) {
                instanceLogger.error("Could not take expected report from queue for transaction {}",
                        transaction.getTransactionId());
                return;
            }
        }
    }

    private void sanitizeAwaitingTransactions() {
        final Instant finish = Instant.now();
        // find transactions that timed out
        final var toRemove = awaitingTransactions.entrySet()
            .stream()
            .filter(entry -> Duration.between(entry.getValue(), finish).compareTo(awaitingTransactionTimeout) > 0)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // remove timed out operations
        toRemove.forEach(awaitingTransactions.keySet()::remove);
        toRemove.forEach(runningTransactions.keySet()::remove);
    }
}
