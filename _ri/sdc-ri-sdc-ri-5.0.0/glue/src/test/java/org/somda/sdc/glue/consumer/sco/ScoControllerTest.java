package org.somda.sdc.glue.consumer.sco;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.Assisted;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.somda.sdc.biceps.model.message.AbstractSet;
import org.somda.sdc.biceps.model.message.AbstractSetResponse;
import org.somda.sdc.biceps.model.message.Activate;
import org.somda.sdc.biceps.model.message.ActivateResponse;
import org.somda.sdc.biceps.model.message.InvocationState;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.biceps.model.message.SetAlertState;
import org.somda.sdc.biceps.model.message.SetAlertStateResponse;
import org.somda.sdc.biceps.model.message.SetComponentState;
import org.somda.sdc.biceps.model.message.SetComponentStateResponse;
import org.somda.sdc.biceps.model.message.SetContextState;
import org.somda.sdc.biceps.model.message.SetContextStateResponse;
import org.somda.sdc.biceps.model.message.SetMetricState;
import org.somda.sdc.biceps.model.message.SetMetricStateResponse;
import org.somda.sdc.biceps.model.message.SetString;
import org.somda.sdc.biceps.model.message.SetStringResponse;
import org.somda.sdc.biceps.model.message.SetValue;
import org.somda.sdc.biceps.model.message.SetValueResponse;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.ThisDeviceBuilder;
import org.somda.sdc.dpws.ThisModelBuilder;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.service.factory.HostingServiceFactory;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.glue.UnitTestUtil;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.glue.consumer.sco.factory.OperationInvocationDispatcherFactory;
import org.somda.sdc.glue.consumer.sco.factory.ScoControllerFactory;
import org.somda.sdc.glue.consumer.sco.factory.ScoTransactionFactory;
import org.somda.sdc.glue.consumer.sco.helper.OperationInvocationDispatcher;
import test.org.somda.common.LoggingTestWatcher;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(LoggingTestWatcher.class)
class ScoControllerTest {
    private Injector injector;
    private HostingServiceProxy hostingServiceProxy;
    private OperationInvocationDispatcher dispatcher;
    private HostedServiceProxy setServiceMock;
    private HostedServiceProxy contextServiceMock;
    private ScoController scoController;
    private ArgumentCaptor<SoapMessage> messageCaptor;
    private RequestResponseClient requestResponseClient;

    @BeforeEach
    void beforeEach() {
        messageCaptor = ArgumentCaptor.forClass(SoapMessage.class);
        requestResponseClient = mock(RequestResponseClient.class);

        setServiceMock = mock(HostedServiceProxy.class);
        contextServiceMock = mock(HostedServiceProxy.class);

        when(setServiceMock.getRequestResponseClient()).thenReturn(requestResponseClient);
        when(contextServiceMock.getRequestResponseClient()).thenReturn(requestResponseClient);

        // bind OperationInvocationDispatcherFactory to invocation dispatcher instance
        injector = new UnitTestUtil().createInjectorWithOverrides(new AbstractModule() {
            @Override
            protected void configure() {
                bind(OperationInvocationDispatcherFactory.class).to(DispatcherFactory.class);
                bind(ScoTransactionFactory.class).to(TransactionFactory.class);
            }
        });

        hostingServiceProxy = injector.getInstance(HostingServiceFactory.class).createHostingServiceProxy(
                "urn:uuid:441dfbea-40e5-406e-b2c4-154d3b8430bf",
                Collections.emptyList(),
                injector.getInstance(ThisDeviceBuilder.class).get(),
                injector.getInstance(ThisModelBuilder.class).get(),
                Collections.emptyMap(), // no services needed as inject in SCO controller separately
                0,
                mock(RequestResponseClient.class),
                "http://xAddr/");

        scoController = injector.getInstance(ScoControllerFactory.class).createScoController(hostingServiceProxy,
                setServiceMock,
                contextServiceMock);

        // start required thread pool(s)
        injector.getInstance(Key.get(
                new TypeLiteral<ExecutorWrapperService<ListeningExecutorService>>(){},
                org.somda.sdc.glue.guice.Consumer.class
        )).startAsync().awaitRunning();
    }

    @Test
    void scoInvoke() throws InterruptedException, ExecutionException, TimeoutException, InterceptorException,
            SoapFaultException, MarshallingException, TransportException {
        List<SetOperationTestSet> testSets = Arrays.asList(
                new SetOperationTestSet(
                        ActionConstants.ACTION_SET_CONTEXT_STATE,
                        new SetContextState(),
                        ScoArtifacts.createResponse(10, InvocationState.WAIT, SetContextStateResponse.class),
                        SetContextStateResponse.class
                ),
                new SetOperationTestSet(
                        ActionConstants.ACTION_ACTIVATE,
                        new Activate(),
                        ScoArtifacts.createResponse(10, InvocationState.WAIT, ActivateResponse.class),
                        ActivateResponse.class
                ),
                new SetOperationTestSet(
                        ActionConstants.ACTION_SET_ALERT_STATE,
                        new SetAlertState(),
                        ScoArtifacts.createResponse(10, InvocationState.WAIT, SetAlertStateResponse.class),
                        SetAlertStateResponse.class
                ),
                new SetOperationTestSet(
                        ActionConstants.ACTION_SET_COMPONENT_STATE,
                        new SetComponentState(),
                        ScoArtifacts.createResponse(10, InvocationState.WAIT, SetComponentStateResponse.class),
                        SetComponentStateResponse.class
                ),
                new SetOperationTestSet(
                        ActionConstants.ACTION_SET_METRIC_STATE,
                        new SetMetricState(),
                        ScoArtifacts.createResponse(10, InvocationState.WAIT, SetMetricStateResponse.class),
                        SetMetricStateResponse.class
                ),
                new SetOperationTestSet(
                        ActionConstants.ACTION_SET_STRING,
                        new SetString(),
                        ScoArtifacts.createResponse(10, InvocationState.WAIT, SetStringResponse.class),
                        SetStringResponse.class
                ),
                new SetOperationTestSet(
                        ActionConstants.ACTION_SET_VALUE,
                        new SetValue(),
                        ScoArtifacts.createResponse(10, InvocationState.WAIT, SetValueResponse.class),
                        SetValueResponse.class
                ));

        for (SetOperationTestSet testSet : testSets) {
            final SoapUtil soapUtil = injector.getInstance(SoapUtil.class);
            final SoapMessage response = soapUtil.createMessage(
                    ActionConstants.getResponseAction(testSet.expectedRequestAction), testSet.expectedResponse);

            when(requestResponseClient.sendRequestResponse(messageCaptor.capture())).thenReturn(response);

            ReportConsumer reportConsumer = new ReportConsumer();
            final ListenableFuture<? extends ScoTransaction<? extends AbstractSetResponse>> future =
                    scoController.invoke(testSet.expectedRequest, reportConsumer, testSet.expectedResponseClass);
            final ScoTransaction<? extends AbstractSetResponse> transaction = future.get(2, TimeUnit.SECONDS);
            // response must be a copy
            assertNotSame(testSet.expectedResponse, transaction.getResponse());
            assertEquals(testSet.expectedResponse.getInvocationInfo().getTransactionId(),
                    transaction.getResponse().getInvocationInfo().getTransactionId());

            final SoapMessage capturedRequest = messageCaptor.getValue();
            assertTrue(capturedRequest.getWsAddressingHeader().getAction().isPresent());
            assertEquals(testSet.expectedRequestAction, capturedRequest.getWsAddressingHeader().getAction().get().getValue());
            assertTrue(soapUtil.getBody(capturedRequest, testSet.expectedRequest.getClass()).isPresent());
            assertEquals(testSet.expectedRequest, soapUtil.getBody(capturedRequest, testSet.expectedRequest.getClass()).get());
        }
    }

    private static class DispatcherFactory implements OperationInvocationDispatcherFactory {
        static final OperationInvocationDispatcher INVOCATION_DISPATCHER = mock(OperationInvocationDispatcher.class);

        @Override
        public OperationInvocationDispatcher createOperationInvocationDispatcher(HostingServiceProxy hostingServiceProxy) {
            return INVOCATION_DISPATCHER;
        }
    }

    private static class TransactionFactory extends ScoTransactionFactory {
        static ScoTransactionImpl<?> lastCreatedTransaction = null;

        @Override
        public <T extends AbstractSetResponse> ScoTransactionImpl<T> createScoTransaction(@Assisted T response,
                                                                                          @Assisted @Nullable Consumer<Pair<OperationInvokedReport.ReportPart, MdibVersion>> reportListener) {
            final ScoTransactionImpl<T> scoTransaction = super.createScoTransaction(response, reportListener);
            lastCreatedTransaction = scoTransaction;
            return scoTransaction;
        }
    }

    private class ReportConsumer implements Consumer<Pair<OperationInvokedReport.ReportPart, MdibVersion>> {
        final List<Pair<OperationInvokedReport.ReportPart, MdibVersion>> reportParts = new ArrayList<>();

        @Override
        public void accept(Pair<OperationInvokedReport.ReportPart, MdibVersion> reportPart) {
            reportParts.add(reportPart);
        }
    }

    private class SetOperationTestSet {
        String expectedRequestAction;
        AbstractSet expectedRequest;
        AbstractSetResponse expectedResponse;
        Class<? extends AbstractSetResponse> expectedResponseClass;

        SetOperationTestSet(String expectedRequestAction,
                            AbstractSet expectedRequest,
                            AbstractSetResponse expectedResponse,
                            Class<? extends AbstractSetResponse> expectedResponseClass) {
            this.expectedRequestAction = expectedRequestAction;
            this.expectedRequest = expectedRequest;
            this.expectedResponse = expectedResponse;
            this.expectedResponseClass = expectedResponseClass;
        }
    }
}