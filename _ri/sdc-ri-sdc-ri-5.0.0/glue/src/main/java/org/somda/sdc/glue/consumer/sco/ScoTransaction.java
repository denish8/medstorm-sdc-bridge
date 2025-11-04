package org.somda.sdc.glue.consumer.sco;

import org.apache.commons.lang3.tuple.Pair;
import org.somda.sdc.biceps.model.message.AbstractSetResponse;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.biceps.model.participant.MdibVersion;

import java.time.Duration;
import java.util.List;

/**
 * Definition of an SDC transaction to track incoming operation invoked report parts.
 *
 * @param <T> Type of the invocation response message.
 */
public interface ScoTransaction<T extends AbstractSetResponse> {
    /**
     * Gets the transaction id.
     * <p>
     * Shortcut of accessing {@code getTransactionId()} of {@link #getResponse()}.
     *
     * @return the transaction id of this {@linkplain ScoTransactionImpl}.
     */
    long getTransactionId();

    /**
     * Gets all reports received so far.
     *
     * @return Snapshot of received reports and their respective {#{@link MdibVersion}} as a copy.
     */
    List<Pair<OperationInvokedReport.ReportPart, MdibVersion>> getReports();

    /**
     * Gets set response message.
     *
     * @return a copy of the set response message of this {@linkplain ScoTransactionImpl}.
     */
    T getResponse();

    /**
     * Starts waiting for a final report.
     * <p>
     * A report is final if {@link ScoUtil#isFinalReport(OperationInvokedReport.ReportPart)} holds true.
     *
     * @param waitTime maximum wait time until this function returns.
     * @return a list that holds all reports and their respective {#{@link MdibVersion}}, including the final one or an empty list if no final report
     * has been received.
     */
    List<Pair<OperationInvokedReport.ReportPart, MdibVersion>> waitForFinalReport(Duration waitTime);
}
