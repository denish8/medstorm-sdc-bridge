package org.somda.sdc.glue.consumer.sco;

import com.google.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.biceps.model.participant.MdibVersion;

import java.util.Collection;
import java.util.Optional;

/**
 * Utility class for SCO processing.
 */
public class ScoUtil {
    @Inject
    ScoUtil() {
    }

    /**
     * Checks if a collection of invocation report parts includes a final report.
     *
     * @param reportParts the collection to skim.
     * @return true if there is a final report, false otherwise.
     * @see #isFinalReport(OperationInvokedReport.ReportPart)
     */
    public boolean hasFinalReport(Collection<Pair<OperationInvokedReport.ReportPart, MdibVersion>> reportParts) {
        return getFinalReport(reportParts).isPresent();
    }

    /**
     * Finds a final report in a collection of reports.
     *
     * @param reportParts the collection to skim.
     * @return first final report part that could be found or {@link Optional#empty()} if no final report exists.
     * @see #isFinalReport(OperationInvokedReport.ReportPart)
     */
    public Optional<Pair<OperationInvokedReport.ReportPart, MdibVersion>> getFinalReport(
            Collection<Pair<OperationInvokedReport.ReportPart, MdibVersion>> reportParts) {
        for (Pair<OperationInvokedReport.ReportPart, MdibVersion> reportPart : reportParts) {
            if (isFinalReport(reportPart.getLeft())) {
                return Optional.of(reportPart);
            }
        }
        return Optional.empty();
    }

    /**
     * Checks whether a report is a final report or not.
     * <p>
     * A report is defined as final if no more reports are expected to be delivered. This applies for
     * <ul>{@link org.somda.sdc.biceps.model.message.InvocationState#CNCLLD}
     * <li>{@link org.somda.sdc.biceps.model.message.InvocationState#CNCLLD_MAN}
     * <li>{@link org.somda.sdc.biceps.model.message.InvocationState#FIN}
     * <li>{@link org.somda.sdc.biceps.model.message.InvocationState#FIN_MOD}
     * <li>{@link org.somda.sdc.biceps.model.message.InvocationState#FAIL}
     * </ul>
     *
     * @param reportPart the report part to check.
     * @return true if the report is a final report, otherwise false.
     */
    public boolean isFinalReport(OperationInvokedReport.ReportPart reportPart) {
        switch (reportPart.getInvocationInfo().getInvocationState()) {
            case CNCLLD:
            case CNCLLD_MAN:
            case FIN:
            case FIN_MOD:
            case FAIL:
                return true;
            case WAIT:
            case START:
            default:
                return false;
        }
    }
}
