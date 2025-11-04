package org.somda.sdc.glue.provider.sco;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.somda.sdc.biceps.common.MdibTypeValidator;
import org.somda.sdc.biceps.model.message.InvocationState;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.biceps.model.participant.LocationContextState;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.biceps.model.participant.PatientContextState;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.provider.access.factory.LocalMdibAccessFactory;
import org.somda.sdc.biceps.testutil.BaseTreeModificationsSet;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.biceps.testutil.MockEntryFactory;
import org.somda.sdc.dpws.device.EventSourceAccess;
import org.somda.sdc.glue.UnitTestUtil;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.glue.provider.sco.factory.ScoControllerFactory;
import test.org.somda.common.LoggingTestWatcher;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(LoggingTestWatcher.class)
class ScoControllerTest {
    private static final UnitTestUtil IT = new UnitTestUtil();

    private ScoController scoController;
    private EventSourceAccess eventSourceAccessMock;
    private Receiver receiver;
    private ArgumentCaptor<String> actionCaptor;
    private ArgumentCaptor<OperationInvokedReport> reportCaptor;

    @BeforeEach
    void beforeEach() throws Exception {
        eventSourceAccessMock = mock(EventSourceAccess.class);
        actionCaptor = ArgumentCaptor.forClass(String.class);
        reportCaptor = ArgumentCaptor.forClass(OperationInvokedReport.class);

        var modifications = new BaseTreeModificationsSet(new MockEntryFactory(IT.getInjector().getInstance(MdibTypeValidator.class)));

        var localMdibAccess = IT.getInjector().getInstance(LocalMdibAccessFactory.class).createLocalMdibAccess();
        localMdibAccess.writeDescription(modifications.createBaseTree());

        receiver = new Receiver();
        scoController = IT.getInjector().getInstance(ScoControllerFactory.class).createScoController(eventSourceAccessMock, localMdibAccess);
        scoController.addOperationInvocationReceiver(receiver);
    }

    @Test
    @DisplayName("Test that handle-based invocation receivers are processed correctly")
    void testHandleSpecificReceiver() {
        final var instanceIdentifier = new InstanceIdentifier();
        instanceIdentifier.setRootName("http://example.com/toor");
        instanceIdentifier.setExtensionName("foo");

        var operation = Handles.OPERATION_0;
        var callbackId = Receiver.ID_SET_STRING_SPECIFIC;
        var itemSize = 1;
        var itemIndex = 0;
        var transactionId = 0;
        {
            final var expectedPayload = "Test";
            final var callerId = "cn=Bastian Beispielmensch, o=Stiftung Beispielhaft, c=DE;";
            scoController.processIncomingSetOperation(operation, instanceIdentifier, expectedPayload, callerId);
            assertEquals(itemSize, receiver.getItems().size());
            assertEquals(callbackId, receiver.getItems().get(itemIndex).getCallbackId());
            assertEquals(instanceIdentifier, receiver.getItems().get(itemIndex).getContext().getInvocationSource());
            assertEquals(operation, receiver.getItems().get(itemIndex).getContext().getOperationHandle());
            assertEquals(transactionId, receiver.getItems().get(itemIndex).getContext().getTransactionId());
            assertTrue(receiver.getItems().get(itemIndex).getData() instanceof String);
            assertEquals(expectedPayload, receiver.getItems().get(itemIndex).getData());
            assertEquals(callerId, receiver.getItems().get(itemIndex).getContext().getCallerId());
        }

        operation = Handles.OPERATION_1;
        callbackId = Receiver.ID_ACTIVATE_SPECIFIC;
        itemSize++;
        itemIndex++;
        transactionId++;
        {
            final var arg1 = "Test";
            final var arg2 = Integer.valueOf(100);
            final var expectedPayload = List.of(arg1, arg2);
            scoController.processIncomingSetOperation(operation, instanceIdentifier, expectedPayload, null);

            assertEquals(itemSize, receiver.getItems().size());
            assertEquals(callbackId, receiver.getItems().get(itemIndex).getCallbackId());
            assertEquals(instanceIdentifier, receiver.getItems().get(itemIndex).getContext().getInvocationSource());
            assertEquals(operation, receiver.getItems().get(itemIndex).getContext().getOperationHandle());
            assertEquals(transactionId, receiver.getItems().get(itemIndex).getContext().getTransactionId());
            assertTrue(receiver.getItems().get(itemIndex).getData() instanceof List);
            assertEquals(expectedPayload, receiver.getItems().get(itemIndex).getData());
        }

        operation = Handles.OPERATION_2;
        callbackId = Receiver.ID_SET_CONTEXT_SPECIFIC;
        itemSize++;
        itemIndex++;
        transactionId++;
        {
            final var contextState1 = new PatientContextState();
            contextState1.setHandle("TestHandle1");
            final var contextState2 = new LocationContextState();
            contextState2.setHandle("TestHandle2");
            final var expectedPayload = List.of(contextState1, contextState2);
            scoController.processIncomingSetOperation(operation, instanceIdentifier, expectedPayload, null);

            assertEquals(itemSize, receiver.getItems().size());
            assertEquals(callbackId, receiver.getItems().get(itemIndex).getCallbackId());
            assertEquals(instanceIdentifier, receiver.getItems().get(itemIndex).getContext().getInvocationSource());
            assertEquals(operation, receiver.getItems().get(itemIndex).getContext().getOperationHandle());
            assertEquals(transactionId, receiver.getItems().get(itemIndex).getContext().getTransactionId());
            assertTrue(receiver.getItems().get(itemIndex).getData() instanceof List);
            assertEquals(expectedPayload, receiver.getItems().get(itemIndex).getData());
        }
    }

    @Test
    @DisplayName("Test that catch-all invocation receivers are processed correctly")
    void testHandleDefaultReceiver() {
        final var instanceIdentifier = new InstanceIdentifier();
        instanceIdentifier.setRootName("http://example.com/toor");
        instanceIdentifier.setExtensionName("foo");

        // set to a handle that has no specific handler but exists
        final var operation = Handles.OPERATION_7;
        final var sourceMds = Handles.MDS_1;
        var callbackId = Receiver.ID_SET_STRING_ALL;
        var itemSize = 1;
        var itemIndex = 0;
        var transactionId = 0;
        {
            final var expectedPayload = "Test";
            scoController.processIncomingSetOperation(operation, instanceIdentifier, expectedPayload, null);
            assertEquals(itemSize, receiver.getItems().size());
            assertEquals(callbackId, receiver.getItems().get(itemIndex).getCallbackId());
            assertEquals(instanceIdentifier, receiver.getItems().get(itemIndex).getContext().getInvocationSource());
            assertEquals(operation, receiver.getItems().get(itemIndex).getContext().getOperationHandle());
            assertEquals(transactionId, receiver.getItems().get(itemIndex).getContext().getTransactionId());
            assertTrue(receiver.getItems().get(itemIndex).getData() instanceof String);
            assertEquals(expectedPayload, receiver.getItems().get(itemIndex).getData());
            assertEquals(sourceMds, receiver.getItems().get(itemIndex).getContext().getSourceMds());
        }

        callbackId = Receiver.ID_LIST_ALL;
        itemSize++;
        itemIndex++;
        transactionId++;
        {
            final var contextState1 = new PatientContextState();
            contextState1.setHandle("TestHandle1");
            final var contextState2 = new LocationContextState();
            contextState2.setHandle("TestHandle2");
            final var expectedPayload = List.of(contextState1, contextState2);
            scoController.processIncomingSetOperation(operation, instanceIdentifier, expectedPayload, null);

            assertEquals(itemSize, receiver.getItems().size());
            assertEquals(callbackId, receiver.getItems().get(itemIndex).getCallbackId());
            assertEquals(instanceIdentifier, receiver.getItems().get(itemIndex).getContext().getInvocationSource());
            assertEquals(operation, receiver.getItems().get(itemIndex).getContext().getOperationHandle());
            assertEquals(transactionId, receiver.getItems().get(itemIndex).getContext().getTransactionId());
            assertTrue(receiver.getItems().get(itemIndex).getData() instanceof List);
            assertEquals(expectedPayload, receiver.getItems().get(itemIndex).getData());
            assertEquals(sourceMds, receiver.getItems().get(itemIndex).getContext().getSourceMds());
        }

        callbackId = Receiver.ID_LIST_ALL;
        itemSize++;
        itemIndex++;
        transactionId++;
        {
            final var arg1 = "Test";
            final var arg2 = Integer.valueOf(100);
            final var expectedPayload = List.of(arg1, arg2);
            scoController.processIncomingSetOperation(operation, instanceIdentifier, expectedPayload, null);

            assertEquals(itemSize, receiver.getItems().size());
            assertEquals(callbackId, receiver.getItems().get(itemIndex).getCallbackId());
            assertEquals(instanceIdentifier, receiver.getItems().get(itemIndex).getContext().getInvocationSource());
            assertEquals(operation, receiver.getItems().get(itemIndex).getContext().getOperationHandle());
            assertEquals(transactionId, receiver.getItems().get(itemIndex).getContext().getTransactionId());
            assertTrue(receiver.getItems().get(itemIndex).getData() instanceof List);
            assertEquals(expectedPayload, receiver.getItems().get(itemIndex).getData());
            assertEquals(sourceMds, receiver.getItems().get(itemIndex).getContext().getSourceMds());
        }
    }

    @Test
    @DisplayName("Test precedence of handle-based invocation receivers over catch-all invocation receivers")
    void testInvocationReceiverPrecedence() {
        SecondReceiver secondReceiver = new SecondReceiver();

        scoController.addOperationInvocationReceiver(secondReceiver);

        final var instanceIdentifier = new InstanceIdentifier();
        instanceIdentifier.setRootName("http://example.com/toor");
        instanceIdentifier.setExtensionName("foo");

        var operation = Handles.OPERATION_0;
        var callbackId = Receiver.ID_SET_STRING_SPECIFIC;
        var itemSize = 1;
        var itemIndex = 0;
        var transactionId = 0;
        {
            final var expectedPayload = "Test";
            scoController.processIncomingSetOperation(operation, instanceIdentifier, expectedPayload, null);
            assertEquals(itemSize, receiver.getItems().size());
            assertEquals(callbackId, receiver.getItems().get(itemIndex).getCallbackId());
            assertEquals(instanceIdentifier, receiver.getItems().get(itemIndex).getContext().getInvocationSource());
            assertEquals(operation, receiver.getItems().get(itemIndex).getContext().getOperationHandle());
            assertEquals(transactionId, receiver.getItems().get(itemIndex).getContext().getTransactionId());
            assertTrue(receiver.getItems().get(itemIndex).getData() instanceof String);
            assertEquals(expectedPayload, receiver.getItems().get(itemIndex).getData());
        }

        operation = Handles.OPERATION_3;
        callbackId = SecondReceiver.ID_SET_STRING_SPECIFIC;
        itemSize = 1;
        itemIndex = 0;
        transactionId++;
        {
            final var expectedPayload = "Test";
            scoController.processIncomingSetOperation(operation, instanceIdentifier, expectedPayload, null);
            assertEquals(itemSize, secondReceiver.getItems().size());
            assertEquals(callbackId, secondReceiver.getItems().get(itemIndex).getCallbackId());
            assertEquals(instanceIdentifier, secondReceiver.getItems().get(itemIndex).getContext().getInvocationSource());
            assertEquals(operation, secondReceiver.getItems().get(itemIndex).getContext().getOperationHandle());
            assertEquals(transactionId, secondReceiver.getItems().get(itemIndex).getContext().getTransactionId());
            assertTrue(secondReceiver.getItems().get(itemIndex).getData() instanceof String);
            assertEquals(expectedPayload, secondReceiver.getItems().get(itemIndex).getData());

        }
    }

    @Test
    @DisplayName("Test that unknown receivers result in an exception")
    void testInvocationForUnknownReceiver() {
        final var instanceIdentifier = new InstanceIdentifier();
        final var operation = "<any handle>";
        final var expectedPayload = Integer.valueOf(100);

        var ex = assertThrows(RuntimeException.class, () -> scoController.processIncomingSetOperation(operation, instanceIdentifier, expectedPayload, null));
        assertTrue(ex.getMessage().contains(operation));
    }

    @Test
    @DisplayName("Test that an invocation result generated by the SCO controller also triggers an invocation invoked report")
    void testImplicitOperationInvokedReportOnErrorGeneratedInScoController() throws Exception {
        final var expectedInstanceIdentifier = new InstanceIdentifier();
        expectedInstanceIdentifier.setRootName("http://example.com/toor");
        expectedInstanceIdentifier.setExtensionName("foo");

        final var expectedInvocationState = InvocationState.FAIL;
        final var operationHandle = Handles.OPERATION_0;

        // pass a big decimal to make sure no receiver will be found
        final var response = scoController.processIncomingSetOperation(
            operationHandle,
            expectedInstanceIdentifier,
            BigDecimal.ZERO,
            null
        );

        // make sure the error result is also sent as report
        verify(eventSourceAccessMock).sendNotification(actionCaptor.capture(), reportCaptor.capture());

        final var report = reportCaptor.getValue().getReportPart().get(0);
        assertEquals(ActionConstants.ACTION_OPERATION_INVOKED_REPORT, actionCaptor.getValue());
        assertEquals(expectedInvocationState, report.getInvocationInfo().getInvocationState());
        assertEquals(operationHandle, report.getOperationHandleRef());
        assertEquals(response.getTransactionId(), report.getInvocationInfo().getTransactionId());
        assertEquals(expectedInstanceIdentifier.getRootName(), report.getInvocationSource().getRootName());
        assertEquals(expectedInstanceIdentifier.getExtensionName(), report.getInvocationSource().getExtensionName());
    }

    private static class Receiver implements OperationInvocationReceiver {
        private final List<Item> items = new ArrayList<>();

        public final static String ID_SET_STRING_SPECIFIC = "setStringForSpecificHandle";
        public final static String ID_ACTIVATE_SPECIFIC = "activateForSpecificHandle";
        public final static String ID_SET_CONTEXT_SPECIFIC = "setContextForSpecificHandle";
        public final static String ID_SET_STRING_ALL = "setStringCatchAll";
        public final static String ID_LIST_ALL = "listCatchAll";

        @IncomingSetServiceRequest(operationHandle = Handles.OPERATION_0)
        InvocationResponse setStringForSpecificHandle(Context context, String data) {
            items.add(new Item(ID_SET_STRING_SPECIFIC, context, data));
            return context.createSuccessfulResponse(MdibVersion.create(), InvocationState.FIN);
        }

        @IncomingSetServiceRequest(operationHandle = Handles.OPERATION_1)
        InvocationResponse activateForSpecificHandle(Context context, List<Object> data) {
            items.add(new Item(ID_ACTIVATE_SPECIFIC, context, data));
            return context.createSuccessfulResponse(MdibVersion.create(), InvocationState.FIN);
        }

        @IncomingSetServiceRequest(operationHandle = Handles.OPERATION_2)
        InvocationResponse setContextForSpecificHandle(Context context, List<AbstractContextState> data) {
            items.add(new Item(ID_SET_CONTEXT_SPECIFIC, context, data));
            return context.createSuccessfulResponse(MdibVersion.create(), InvocationState.FIN);
        }

        @IncomingSetServiceRequest
        InvocationResponse setStringCatchAll(Context context, String data) {
            items.add(new Item(ID_SET_STRING_ALL, context, data));
            return context.createSuccessfulResponse(MdibVersion.create(), InvocationState.FIN);
        }

        @IncomingSetServiceRequest
        InvocationResponse listCatchAll(Context context, List<Object> data) {
            items.add(new Item(ID_LIST_ALL, context, data));
            return context.createSuccessfulResponse(MdibVersion.create(), InvocationState.FIN);
        }

        public List<Item> getItems() {
            return items;
        }
    }

    private static class SecondReceiver implements OperationInvocationReceiver {
        private final List<Item> items = new ArrayList<>();
        public final static String ID_SET_STRING_SPECIFIC = "secondSetStringForSpecificHandle";

        @IncomingSetServiceRequest(operationHandle = Handles.OPERATION_3)
        InvocationResponse setStringForSpecificHandle(Context context, String data) {
            items.add(new Item(ID_SET_STRING_SPECIFIC, context, data));
            return context.createSuccessfulResponse(MdibVersion.create(), InvocationState.FIN);
        }

        public List<Item> getItems() {
            return items;
        }
    }

    private static class Item {
        private final String callbackId;
        private final Context context;
        private final Object data;

        public Item(String callbackId, Context context, Object data) {
            this.callbackId = callbackId;
            this.context = context;
            this.data = data;
        }

        public String getCallbackId() {
            return callbackId;
        }

        public Context getContext() {
            return context;
        }

        public Object getData() {
            return data;
        }
    }
}