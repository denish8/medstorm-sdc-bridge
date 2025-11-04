package org.somda.sdc.glue.consumer.sco.helper;

import com.google.inject.Injector;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.model.message.AbstractSetResponse;
import org.somda.sdc.biceps.model.message.InvocationState;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.biceps.model.message.SetValueResponse;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.common.guice.AbstractConfigurationModule;
import org.somda.sdc.dpws.ThisDeviceBuilder;
import org.somda.sdc.dpws.ThisModelBuilder;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.service.factory.HostingServiceFactory;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.glue.UnitTestUtil;
import org.somda.sdc.glue.consumer.ConsumerConfig;
import org.somda.sdc.glue.consumer.sco.ScoArtifacts;
import org.somda.sdc.glue.consumer.sco.ScoTransactionImpl;
import org.somda.sdc.glue.consumer.sco.factory.OperationInvocationDispatcherFactory;
import test.org.somda.common.LoggingTestWatcher;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(LoggingTestWatcher.class)
class OperationInvocationDispatcherTest {
    private static final UnitTestUtil UT = new UnitTestUtil();

    private Injector injector;
    private HostingServiceProxy hostingServiceProxy;
    private OperationInvocationDispatcher dispatcher;

    private Duration timeout = Duration.ofMillis(500);

    @BeforeEach
    void beforeEach() {
        injector = UT.createInjectorWithOverrides(new AbstractConfigurationModule() {
            @Override
            protected void defaultConfigure() {
                bind(ConsumerConfig.AWAITING_TRANSACTION_TIMEOUT,
                    Duration.class,
                    timeout);
            }
        });

        hostingServiceProxy = injector.getInstance(HostingServiceFactory.class).createHostingServiceProxy(
                "urn:uuid:441dfbea-40e5-406e-b2c4-154d3b8430bf",
                Collections.emptyList(),
                UT.getInjector().getInstance(ThisDeviceBuilder.class).get(),
                UT.getInjector().getInstance(ThisModelBuilder.class).get(),
                Collections.emptyMap(),
                0,
                mock(RequestResponseClient.class),
                "http://xAddr/");

        dispatcher = injector
                .getInstance(OperationInvocationDispatcherFactory.class)
                .createOperationInvocationDispatcher(hostingServiceProxy);
    }

    @Test
    void dispatchReportWithoutMatchingTransaction() {
        ScoTransactionImpl<SetValueResponse> scoTransaction = (ScoTransactionImpl<SetValueResponse>) mock(ScoTransactionImpl.class);
        long expectedTransactionId = 5;
        OperationInvokedReport report = ScoArtifacts.createReport(expectedTransactionId, InvocationState.CNCLLD);

        dispatcher.registerTransaction(scoTransaction);
        dispatcher.dispatchReport(report);

        verify(scoTransaction, times(0))
                .receiveIncomingReport(any());
        verify(scoTransaction, atLeast(1)).getTransactionId();
    }

    @Test
    void dispatchReportWithMatchingTransaction() {
        ScoTransactionImpl<SetValueResponse> scoTransaction = (ScoTransactionImpl<SetValueResponse>) mock(ScoTransactionImpl.class);
        long expectedTransactionId = 5;
        when(scoTransaction.getTransactionId()).thenReturn(expectedTransactionId);

        var report = ScoArtifacts.createReport(expectedTransactionId, InvocationState.CNCLLD);
        report.setSequenceId("joinky:ploinky");
        var mdibVersion = ScoArtifacts.mdibVersionFromReport(report);

        dispatcher.registerTransaction(scoTransaction);
        dispatcher.dispatchReport(report);

        verify(scoTransaction, times(1))
                .receiveIncomingReport(new ImmutablePair<>(report.getReportPart().get(0), mdibVersion));
        verify(scoTransaction, atLeast(1)).getTransactionId();
    }

    @Test
    void dispatchReportWithMultipleReportParts() {
        final List<ScoTransactionImpl<? extends AbstractSetResponse>> transactions = new ArrayList<>();
        long maxReports = 5;
        OperationInvokedReport report = new OperationInvokedReport();
        report.setSequenceId("joinky:ploinky");
        for (long i = 0; i < maxReports; ++i) {
            ScoTransactionImpl<SetValueResponse> transaction = (ScoTransactionImpl<SetValueResponse>) mock(ScoTransactionImpl.class);
            when(transaction.getTransactionId()).thenReturn(i);
            transactions.add(transaction);
            report.getReportPart().add(ScoArtifacts.createReportPart(i, InvocationState.FIN));
            dispatcher.registerTransaction(transaction);
        }
        var mdibVersion = ScoArtifacts.mdibVersionFromReport(report);

        dispatcher.dispatchReport(report);

        for (int i = 0; i < maxReports; ++i) {
            ScoTransactionImpl<? extends AbstractSetResponse> scoTransaction = transactions.get(i);
            verify(scoTransaction, times(1))
                    .receiveIncomingReport(new ImmutablePair<>(report.getReportPart().get(i), mdibVersion));
            verify(scoTransaction, atLeast(1)).getTransactionId();
        }
    }

    @Test
    void concurrentDispatch() throws Exception {
        final List<ScoTransactionImpl<? extends AbstractSetResponse>> transactions = new ArrayList<>();
        long maxReports = 2;
        OperationInvokedReport report = new OperationInvokedReport();
        for (long i = 0; i < maxReports; ++i) {
            ScoTransactionImpl<SetValueResponse> transaction = (ScoTransactionImpl<SetValueResponse>) mock(ScoTransactionImpl.class);
            when(transaction.getTransactionId()).thenReturn(i);
            transactions.add(transaction);
            report.getReportPart().add(ScoArtifacts.createReportPart(i, InvocationState.FIN));
            dispatcher.registerTransaction(transaction);
        }

        dispatcher.dispatchReport(report);
        Thread.sleep(timeout.toMillis() * 2);
        dispatcher.dispatchReport(report);
    }
}