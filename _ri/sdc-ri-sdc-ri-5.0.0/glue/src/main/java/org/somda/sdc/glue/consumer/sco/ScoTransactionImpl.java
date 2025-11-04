package org.somda.sdc.glue.consumer.sco;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.somda.sdc.biceps.model.message.AbstractSetResponse;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.common.util.AutoLock;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Default implementation of {@linkplain ScoTransaction}
 * <p>
 * The implementation supports an internal function to trigger reception of incoming reports,
 * see {@link #receiveIncomingReport(Pair)}.
 *
 * @param <T> the response type.
 */
public class ScoTransactionImpl<T extends AbstractSetResponse> implements ScoTransaction<T> {
    private final T response;
    private final Consumer<Pair<OperationInvokedReport.ReportPart, MdibVersion>> reportListener;
    private final ArrayList<Pair<OperationInvokedReport.ReportPart, MdibVersion>> collectedReports;
    private final ReentrantLock reportsLock;
    private final Condition reportsCondition;
    private final ScoUtil scoUtil;

    public ScoTransactionImpl(T response,
                              @Nullable Consumer<Pair<OperationInvokedReport.ReportPart, MdibVersion>> reportListener,
                              ScoUtil scoUtil) {
        this.response = response;
        this.reportListener = reportListener;
        this.reportsLock = new ReentrantLock();
        this.reportsCondition = reportsLock.newCondition();
        this.scoUtil = scoUtil;
        this.collectedReports = new ArrayList<>(3);
    }

    @Override
    public long getTransactionId() {
        return response.getInvocationInfo().getTransactionId();
    }

    @Override
    public List<Pair<OperationInvokedReport.ReportPart, MdibVersion>> getReports() {
        try (var ignored = AutoLock.lock(reportsLock)) {
            return deepCopyCollectedReports();
        }
    }

    @Override
    public T getResponse() {
        return (T) response.createCopy();
    }

    @Override
    public List<Pair<OperationInvokedReport.ReportPart, MdibVersion>> waitForFinalReport(Duration waitTime) {
        var copyWaitTime = waitTime;
        try (var ignored = AutoLock.lock(reportsLock)) {
            if (scoUtil.hasFinalReport(collectedReports)) {
                return deepCopyCollectedReports();
            }

            do {
                Instant start = Instant.now();
                try {
                    if (reportsCondition.await(waitTime.toMillis(), TimeUnit.MILLISECONDS)) {
                        if (scoUtil.hasFinalReport(collectedReports)) {
                            return deepCopyCollectedReports();
                        }
                    } else {
                        return Collections.emptyList();
                    }
                } catch (InterruptedException e) {
                    if (scoUtil.hasFinalReport(collectedReports)) {
                        return deepCopyCollectedReports();
                    } else {
                        return Collections.emptyList();
                    }
                }

                Instant finish = Instant.now();
                copyWaitTime = copyWaitTime.minus(Duration.between(start, finish));
            } while (copyWaitTime.toMillis() > 0);
        }
        return Collections.emptyList();
    }

    /**
     * Internal function to trigger reception of an incoming report.
     * <p>
     * Notifies waiting threads.
     *
     * @param report the report to receive.
     */
    public void receiveIncomingReport(Pair<OperationInvokedReport.ReportPart, MdibVersion> report) {
        try (var ignored = AutoLock.lock(reportsLock)) {
            collectedReports.add(report);
            reportsCondition.signalAll();
        }

        if (reportListener != null) {
            reportListener.accept(report);
        }
    }

    private List<Pair<OperationInvokedReport.ReportPart, MdibVersion>> deepCopyCollectedReports() {
        return collectedReports.stream()
                .map(it -> new ImmutablePair<>(it.getLeft().createCopy(), it.getRight()))
                .collect(Collectors.toList());
    }
}
