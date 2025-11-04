package org.somda.sdc.glue.consumer.sco;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.model.message.InvocationState;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import test.org.somda.common.LoggingTestWatcher;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(LoggingTestWatcher.class)
class ScoUtilTest {
    private ScoUtil scoUtil;

    @BeforeEach
    void beforeEach() {
        this.scoUtil = new ScoUtil();
    }

    Pair<OperationInvokedReport.ReportPart, MdibVersion> dummyPair(OperationInvokedReport.ReportPart part) {
        return new ImmutablePair<>(part, MdibVersion.create());
    }

    @Test
    void hasFinalReport() {
        assertFalse(scoUtil.hasFinalReport(Arrays.asList(
            dummyPair(ScoArtifacts.createReportPart(InvocationState.WAIT)),
            dummyPair(ScoArtifacts.createReportPart(InvocationState.START)))));

        assertTrue(scoUtil.hasFinalReport(Arrays.asList(
            dummyPair(ScoArtifacts.createReportPart(InvocationState.WAIT)),
            dummyPair(ScoArtifacts.createReportPart(InvocationState.FAIL)),
            dummyPair(ScoArtifacts.createReportPart(InvocationState.START)))));

        assertTrue(scoUtil.hasFinalReport(Arrays.asList(
            dummyPair(ScoArtifacts.createReportPart(InvocationState.WAIT)),
            dummyPair(ScoArtifacts.createReportPart(InvocationState.START)),
            dummyPair(ScoArtifacts.createReportPart(InvocationState.CNCLLD_MAN)))));
    }

    @Test
    void getFinalReport() {
        final OperationInvokedReport.ReportPart expectedReport = ScoArtifacts.createReportPart(InvocationState.FAIL);
        final Optional<Pair<OperationInvokedReport.ReportPart, MdibVersion>> actualReport = scoUtil.getFinalReport(Arrays.asList(
                dummyPair(ScoArtifacts.createReportPart(InvocationState.WAIT)),
                dummyPair(expectedReport),
                dummyPair(ScoArtifacts.createReportPart(InvocationState.START))));
        assertTrue(actualReport.isPresent());
        assertEquals(expectedReport, actualReport.get().getLeft());
    }

    @Test
    void isFinalReport() {
        assertFalse(scoUtil.isFinalReport(ScoArtifacts.createReportPart(InvocationState.WAIT)));
        assertFalse(scoUtil.isFinalReport(ScoArtifacts.createReportPart(InvocationState.START)));
        assertTrue(scoUtil.isFinalReport(ScoArtifacts.createReportPart(InvocationState.FIN)));
        assertTrue(scoUtil.isFinalReport(ScoArtifacts.createReportPart(InvocationState.FAIL)));
        assertTrue(scoUtil.isFinalReport(ScoArtifacts.createReportPart(InvocationState.CNCLLD_MAN)));
        assertTrue(scoUtil.isFinalReport(ScoArtifacts.createReportPart(InvocationState.CNCLLD)));
    }
}