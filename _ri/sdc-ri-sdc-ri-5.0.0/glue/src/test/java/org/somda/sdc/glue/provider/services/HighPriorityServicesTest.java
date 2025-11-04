package org.somda.sdc.glue.provider.services;

import com.google.inject.Injector;
import it.org.somda.sdc.dpws.soap.Ssl;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.somda.sdc.biceps.common.MdibTypeValidator;
import org.somda.sdc.biceps.model.message.ActivateResponse;
import org.somda.sdc.biceps.model.message.GetContextStatesResponse;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.biceps.model.participant.EnsembleContextDescriptor;
import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.biceps.model.participant.PatientContextDescriptor;
import org.somda.sdc.biceps.model.participant.SystemContextDescriptor;
import org.somda.sdc.biceps.provider.access.factory.LocalMdibAccessFactory;
import org.somda.sdc.biceps.testutil.BaseTreeModificationsSet;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.biceps.testutil.MockEntryFactory;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.sdc.dpws.soap.ApplicationInfo;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.TransportInfo;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wseventing.SourceSubscriptionManager;
import org.somda.sdc.dpws.soap.wseventing.Subscriptions;
import org.somda.sdc.dpws.soap.wseventing.model.Notification;
import org.somda.sdc.glue.UnitTestUtil;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.glue.provider.services.factory.ServicesFactory;

import javax.annotation.Nullable;
import java.io.StringWriter;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.calls;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HighPriorityServicesTest {
    private static final int MDS_0_CONTEXT_STATES_COUNT = 6;
    private static final int MDS_1_CONTEXT_STATES_COUNT = 4;
    private static final int CONTEXTDESCRIPTOR_DEFAULT_STATES_COUNT = 1;
    private static final int CONTEXTDESCRIPTOR_7_STATES_COUNT = 3;

    private static final UnitTestUtil IT = new UnitTestUtil();

    private final Injector injector = IT.getInjector();
    private final MockEntryFactory mockEntryFactory = new MockEntryFactory(injector.getInstance(MdibTypeValidator.class));
    private final org.somda.sdc.biceps.model.message.ObjectFactory messageModelFactory =
            injector.getInstance(org.somda.sdc.biceps.model.message.ObjectFactory.class);
    private final SoapUtil soapUtil = injector.getInstance(SoapUtil.class);
    private final WsAddressingUtil wsaUtil = injector.getInstance(WsAddressingUtil.class);

    private HighPriorityServices highPriorityServices;

    @BeforeEach
    void setUp() throws Exception {
        var tree = new BaseTreeModificationsSet(mockEntryFactory);
        var mods = tree.createBaseTree();
        mods.insert(mockEntryFactory.entry(Handles.SYSTEMCONTEXT_1, SystemContextDescriptor.class, Handles.MDS_1))
                .insert(mockEntryFactory.contextEntry(Handles.CONTEXTDESCRIPTOR_6, Handles.CONTEXT_6,
                        PatientContextDescriptor.class, Handles.SYSTEMCONTEXT_1))
                .insert(mockEntryFactory.contextEntry(Handles.CONTEXTDESCRIPTOR_7,
                        List.of(Handles.CONTEXT_7, Handles.CONTEXT_8, Handles.CONTEXT_9),
                        EnsembleContextDescriptor.class, Handles.SYSTEMCONTEXT_1));

        var mdibAccess = injector.getInstance(LocalMdibAccessFactory.class).createLocalMdibAccess();
        mdibAccess.writeDescription(mods);

        highPriorityServices = injector.getInstance(ServicesFactory.class).createHighPriorityServices(mdibAccess);
    }

    @Test
    void getContextStatesAll() throws SoapFaultException {
        var response = invokeGetContextStates(List.of());
        assertEquals(MDS_0_CONTEXT_STATES_COUNT + MDS_1_CONTEXT_STATES_COUNT, response.getContextState().size());
    }

    @Test
    void getContextStatesFilterWithMds() throws SoapFaultException {
        var response = invokeGetContextStates(List.of(Handles.MDS_1));
        assertEquals(MDS_1_CONTEXT_STATES_COUNT, response.getContextState().size());

        response = invokeGetContextStates(List.of(Handles.MDS_0));
        assertEquals(MDS_0_CONTEXT_STATES_COUNT, response.getContextState().size());

        response = invokeGetContextStates(List.of(Handles.MDS_0, Handles.MDS_1));
        assertEquals(MDS_0_CONTEXT_STATES_COUNT + MDS_1_CONTEXT_STATES_COUNT, response.getContextState().size());
    }

    @Test
    void getContextStatesFilterWithDescriptorHandle() throws SoapFaultException {
        var response = invokeGetContextStates(List.of(Handles.CONTEXTDESCRIPTOR_0));
        assertEquals(CONTEXTDESCRIPTOR_DEFAULT_STATES_COUNT, response.getContextState().size());

        response = invokeGetContextStates(List.of(Handles.CONTEXTDESCRIPTOR_1));
        assertEquals(CONTEXTDESCRIPTOR_DEFAULT_STATES_COUNT, response.getContextState().size());

        response = invokeGetContextStates(List.of(Handles.CONTEXTDESCRIPTOR_7));
        assertEquals(CONTEXTDESCRIPTOR_7_STATES_COUNT, response.getContextState().size());

        response = invokeGetContextStates(List.of(Handles.CONTEXTDESCRIPTOR_1, Handles.CONTEXTDESCRIPTOR_7));
        assertEquals(CONTEXTDESCRIPTOR_DEFAULT_STATES_COUNT + CONTEXTDESCRIPTOR_7_STATES_COUNT,
                response.getContextState().size());
    }

    @Test
    void getContextStatesFilterWithStateHandle() throws SoapFaultException {
        var someContextStateHandles = List.of(
                Handles.CONTEXT_0, Handles.CONTEXT_1, Handles.CONTEXT_6, Handles.CONTEXT_8, Handles.CONTEXT_9);
        var response = invokeGetContextStates(someContextStateHandles);

        // exemplary check if result contains the right context states
        assertEquals(someContextStateHandles.size(), response.getContextState().stream().filter(state ->
                someContextStateHandles.contains(state.getHandle())
        ).collect(Collectors.toSet()).size());

        response = invokeGetContextStates(List.of(Handles.CONTEXT_2));
        assertEquals(1, response.getContextState().size());
    }

    @Test
    void callerSourceAnonymousDeterminedCorrectly() throws Exception {
        testActivateInvocationSource(null);
    }

    @Test
    void callerSourceKnownDeterminedCorrectly() throws Exception {
        final X509Certificate testCert = Ssl.getServerCertificate();
        testActivateInvocationSource(testCert);
    }

    void testActivateInvocationSource(@Nullable X509Certificate certificate) throws Exception {
        final var mockNotifyTo = new EndpointReferenceType();
        mockNotifyTo.setAddress(wsaUtil.createAttributedURIType(soapUtil.createRandomUuidUri()));

        final var mockSubscriptionManager = mock(SourceSubscriptionManager.class);
        when(mockSubscriptionManager.getFilters())
                .thenReturn(List.of(ActionConstants.ACTION_OPERATION_INVOKED_REPORT));
        when(mockSubscriptionManager.getSubscriptionId())
                .thenReturn("id");
                when(mockSubscriptionManager.getNotifyTo())
                .thenReturn(mockNotifyTo);

        final var mockSubscriptions = mock(Subscriptions.class);
        when(mockSubscriptions.get(any())).thenReturn(Optional.of(mockSubscriptionManager));

        final var eventSource = highPriorityServices.getEventSources().stream().findFirst().orElseThrow();
        eventSource.subscribe(mockSubscriptionManager);
        eventSource.init(mockSubscriptions);

        try {
            invokeActivate(Handles.OPERATION_0, certificate);

            // sleep a little to make sure the report was sent
            Thread.sleep(100);

            // retrieve OperationInvokedReport and check
            final var captor = ArgumentCaptor.forClass(Notification.class);
            verify(mockSubscriptionManager, times(1)).offerNotification(captor.capture());

            final var capturedValues = captor.getAllValues();
            assertEquals(1, capturedValues.size());

            final var report = soapUtil.getBody(capturedValues.get(0).getPayload(), OperationInvokedReport.class).orElseThrow();
            final var invocationSource = report.getReportPart().get(0).getInvocationSource();

            if (certificate != null) {
                assertEquals("http://standards.ieee.org/downloads/11073/11073-20701-2018/X509Certificate/PEM", invocationSource.getRootName());
                final var stringWriter = new StringWriter();
                try (final var writer = new JcaPEMWriter(stringWriter)) {
                    writer.writeObject(certificate);
                }
                final var written = stringWriter.toString();
                assertFalse(written.isBlank());
                assertEquals(written, invocationSource.getExtensionName());
            } else {
                assertEquals("http://standards.ieee.org/downloads/11073/11073-20701-2018", invocationSource.getRootName());
                assertEquals("AnonymousSdcParticipant", invocationSource.getExtensionName());
            }
        } finally {
            eventSource.unsubscribe(mockSubscriptionManager);
        }
    }

    private SoapMessage createGetContextStatesRequest(List<String> handleRefs) {
        var getContextStates = messageModelFactory.createGetContextStates();
        getContextStates.setHandleRef(handleRefs);
        return soapUtil.createMessage(
                ActionConstants.ACTION_GET_CONTEXT_STATES,
                getContextStates
        );
    }

    private SoapMessage createGetContextStatesResponse() {
        return soapUtil.createMessage(ActionConstants.getResponseAction(ActionConstants.ACTION_GET_CONTEXT_STATES));
    }

    private RequestResponseObject createGetContextStatesRequestResponseObject(List<String> handleRefs) {
        return new RequestResponseObject(
                createGetContextStatesRequest(handleRefs),
                createGetContextStatesResponse(),
                mock(CommunicationContext.class)
        );
    }

    private SoapMessage createActivate(String handle) {
        var activate = messageModelFactory.createActivate();
        activate.setOperationHandleRef(handle);
        final var msg = soapUtil.createMessage(
                ActionConstants.ACTION_ACTIVATE,
                activate
        );
        msg.getWsAddressingHeader().setMessageId(wsaUtil.createAttributedURIType(soapUtil.createRandomUuidUri()));
        return msg;
    }

    private SoapMessage createActivateResponse() {
        return soapUtil.createMessage(ActionConstants.getResponseAction(ActionConstants.ACTION_ACTIVATE));
    }

    private RequestResponseObject createActivateRequestResponseObject(String handle, @Nullable X509Certificate callerCert) {
        final List<X509Certificate> certs;
        if (callerCert != null) {
            certs = List.of(callerCert);
        } else {
            certs = Collections.emptyList();
        }
        final var ctx = new CommunicationContext(
            new ApplicationInfo(),
            new TransportInfo(
                "https",
                null, null, null, null, certs
            ),
            null
        );

        return new RequestResponseObject(
            createActivate(handle),
            createActivateResponse(),
            ctx
        );
    }

    private GetContextStatesResponse invokeGetContextStates(List<String> handleRefs) throws SoapFaultException {
        var rr = createGetContextStatesRequestResponseObject(handleRefs);
        highPriorityServices.getContextStates(rr);
        return soapUtil.getBody(rr.getResponse(), GetContextStatesResponse.class).orElseThrow();
    }

    private ActivateResponse invokeActivate(String handle, @Nullable X509Certificate callerCert) throws SoapFaultException {
        var rr = createActivateRequestResponseObject(handle, callerCert);
        highPriorityServices.activate(rr);
        return soapUtil.getBody(rr.getResponse(), ActivateResponse.class).orElseThrow();
    }
}