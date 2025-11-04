package org.somda.sdc.glue.consumer.sco;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.somda.sdc.biceps.model.message.AbstractSetResponse;
import org.somda.sdc.biceps.model.message.InvocationInfo;
import org.somda.sdc.biceps.model.message.InvocationState;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.biceps.model.participant.MdibVersion;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

public class ScoArtifacts {

    public static MdibVersion mdibVersionFromReport(OperationInvokedReport report) {
        return new MdibVersion(
                report.getSequenceId(),
                report.getMdibVersion() != null ? report.getMdibVersion() : BigInteger.ZERO,
                report.getInstanceId() != null ? report.getMdibVersion() : BigInteger.ZERO
        );
    }

    public static Pair<OperationInvokedReport.ReportPart, MdibVersion> dummyPart(OperationInvokedReport.ReportPart part) {
        return new ImmutablePair<>(part, MdibVersion.create());
    }

    public static Pair<OperationInvokedReport.ReportPart, MdibVersion> dummyPart(InvocationState invocationState) {
        return dummyPart(invocationState, MdibVersion.create());
    }

    public static Pair<OperationInvokedReport.ReportPart, MdibVersion> dummyPart(long transactionId, InvocationState invocationState) {
        return dummyPart(transactionId, invocationState, MdibVersion.create());
    }

    public static Pair<OperationInvokedReport.ReportPart, MdibVersion> dummyPart(InvocationState invocationState, MdibVersion version) {
        return new ImmutablePair<>(createReportPart(invocationState), version);
    }

    public static Pair<OperationInvokedReport.ReportPart, MdibVersion> dummyPart(long transactionId, InvocationState invocationState, MdibVersion version) {
        return new ImmutablePair<>(createReportPart(transactionId, invocationState), version);
    }


    public static OperationInvokedReport createReport(InvocationState invocationState) {
        return createReport(0, invocationState);
    }

    public static OperationInvokedReport createReport(Collection<Map.Entry<Long, InvocationState>> parts) {
        OperationInvokedReport report = new OperationInvokedReport();

        for (Map.Entry<Long, InvocationState> part : parts) {
            report.getReportPart().add(createReportPart(part.getKey(), part.getValue()));
        }

        return report;
    }

    public static OperationInvokedReport.ReportPart createReportPart(InvocationState invocationState) {
        return createReportPart(0, invocationState);
    }

    public static OperationInvokedReport createReport(long transactionId, InvocationState invocationState) {
        OperationInvokedReport report = new OperationInvokedReport();
        report.getReportPart().add(createReportPart(transactionId, invocationState));
        return report;
    }

    public static OperationInvokedReport.ReportPart createReportPart(long transactionId, InvocationState invocationState) {
        OperationInvokedReport.ReportPart reportPart = new OperationInvokedReport.ReportPart();
        InvocationInfo invocationInfo = new InvocationInfo();
        invocationInfo.setInvocationState(invocationState);
        invocationInfo.setTransactionId(transactionId);
        reportPart.setInvocationInfo(invocationInfo);
        return reportPart;
    }

    public static <T extends AbstractSetResponse> T createResponse(long transactionId,
                                                                   InvocationState invocationState,
                                                                   Class<T> responseClass) {
        try {
            final T response = responseClass.getConstructor().newInstance();
            InvocationInfo invocationInfo = new InvocationInfo();
            invocationInfo.setInvocationState(invocationState);
            invocationInfo.setTransactionId(transactionId);
            response.setInvocationInfo(invocationInfo);
            return response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
