package it.org.somda.glue.consumer;

import org.apache.commons.lang3.tuple.Pair;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import test.org.somda.common.TimedWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ReportListenerSpy implements java.util.function.Consumer<Pair<OperationInvokedReport.ReportPart, MdibVersion>> {
    private final TimedWait<List<Pair<OperationInvokedReport.ReportPart, MdibVersion>>> timedWaiter;

    public ReportListenerSpy() {
        this.timedWaiter = new TimedWait<>(ArrayList::new);
    }

    @Override
    public void accept(Pair<OperationInvokedReport.ReportPart, MdibVersion> reportPart) {
        timedWaiter.modifyData(reportParts -> reportParts.add(reportPart));
    }

    public List<Pair<OperationInvokedReport.ReportPart, MdibVersion>> getReports() {
        return timedWaiter.getData();
    }

    public boolean waitForReports(int reportCount, Duration waitTime) {
        return timedWaiter.waitForData(reportParts -> reportParts.size() == reportCount, waitTime);
    }

    public void reset() {
        timedWaiter.reset();
    }
}
