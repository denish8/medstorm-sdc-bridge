package org.somda.sdc.glue.provider.services;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.access.ReadTransaction;
import org.somda.sdc.biceps.model.message.AbstractSet;
import org.somda.sdc.biceps.model.message.AbstractSetResponse;
import org.somda.sdc.biceps.model.message.Activate;
import org.somda.sdc.biceps.model.message.ActivateResponse;
import org.somda.sdc.biceps.model.message.GetContainmentTree;
import org.somda.sdc.biceps.model.message.GetContainmentTreeResponse;
import org.somda.sdc.biceps.model.message.GetContextStates;
import org.somda.sdc.biceps.model.message.GetContextStatesResponse;
import org.somda.sdc.biceps.model.message.GetDescriptor;
import org.somda.sdc.biceps.model.message.GetDescriptorResponse;
import org.somda.sdc.biceps.model.message.GetMdDescription;
import org.somda.sdc.biceps.model.message.GetMdDescriptionResponse;
import org.somda.sdc.biceps.model.message.GetMdState;
import org.somda.sdc.biceps.model.message.GetMdStateResponse;
import org.somda.sdc.biceps.model.message.GetMdib;
import org.somda.sdc.biceps.model.message.GetMdibResponse;
import org.somda.sdc.biceps.model.message.InvocationInfo;
import org.somda.sdc.biceps.model.message.ObjectFactory;
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
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.ContainmentTree;
import org.somda.sdc.biceps.model.participant.ContainmentTreeEntry;
import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.dpws.device.EventSourceAccess;
import org.somda.sdc.dpws.device.WebService;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.factory.SoapFaultFactory;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wseventing.ActionBasedEventSource;
import org.somda.sdc.dpws.soap.wseventing.model.WsEventingStatus;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.glue.common.MdibMapper;
import org.somda.sdc.glue.common.MdibVersionUtil;
import org.somda.sdc.glue.common.factory.MdibMapperFactory;
import org.somda.sdc.glue.provider.sco.IncomingSetServiceRequest;
import org.somda.sdc.glue.provider.sco.InvocationResponse;
import org.somda.sdc.glue.provider.sco.OperationInvocationReceiver;
import org.somda.sdc.glue.provider.sco.ScoController;
import org.somda.sdc.glue.provider.sco.factory.ScoControllerFactory;
import org.somda.sdc.glue.provider.services.helper.ReportGenerator;
import org.somda.sdc.glue.provider.services.helper.factory.ReportGeneratorFactory;

import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Implementation of the high-priority services.
 * <p>
 * High-priority services are all services that provide time-critical data:
 * <ul>
 * <li>Get service
 * <li>Set service
 * <li>Description event service
 * <li>State event service
 * <li>Context service
 * <li>Waveform service
 * <li>Containment tree service
 * </ul>
 * <p>
 */
public class HighPriorityServices extends WebService implements EventSourceAccess {

    private final Logger LOG = LogManager.getLogger();
    private final LocalMdibAccess mdibAccess;
    private final SoapUtil soapUtil;
    private final SoapFaultFactory faultFactory;
    private final ObjectFactory messageModelFactory;
    private final org.somda.sdc.biceps.model.participant.ObjectFactory participantModelFactory;
    private final MdibMapperFactory mdibMapperFactory;
    private final MdibVersionUtil mdibVersionUtil;
    private final WsAddressingUtil wsaUtil;
    private final ScoController scoController;

    private final InstanceIdentifier anonymousSource;
    private final ReportGenerator reportGenerator;

    private final ActionBasedEventSource actionBasedEventSource;

    @AssistedInject
    HighPriorityServices(@Assisted LocalMdibAccess mdibAccess,
                         ReportGeneratorFactory reportGeneratorFactory,
                         SoapUtil soapUtil,
                         SoapFaultFactory faultFactory,
                         ObjectFactory messageModelFactory,
                         org.somda.sdc.biceps.model.participant.ObjectFactory participantModelFactory,
                         MdibMapperFactory mdibMapperFactory,
                         ScoControllerFactory scoControllerFactory,
                         MdibVersionUtil mdibVersionUtil,
                         WsAddressingUtil wsaUtil,
                         ActionBasedEventSource actionBasedEventSource) {
        this.mdibAccess = mdibAccess;
        this.soapUtil = soapUtil;
        this.faultFactory = faultFactory;
        this.messageModelFactory = messageModelFactory;
        this.participantModelFactory = participantModelFactory;
        this.mdibMapperFactory = mdibMapperFactory;
        this.mdibVersionUtil = mdibVersionUtil;
        this.wsaUtil = wsaUtil;
        this.scoController = scoControllerFactory.createScoController(this, mdibAccess);
        this.reportGenerator = reportGeneratorFactory.createReportGenerator(this);
        this.actionBasedEventSource = actionBasedEventSource;
        this.registerEventSource(actionBasedEventSource);
        anonymousSource = new InstanceIdentifier();
        anonymousSource.setRootName("http://standards.ieee.org/downloads/11073/11073-20701-2018");
        anonymousSource.setExtensionName("AnonymousSdcParticipant");

        mdibAccess.registerObserver(reportGenerator);
    }

    /**
     * Registers an object that possesses callback functions for incoming set service requests.
     *
     * @param receiver the object that includes methods annotated with {@link IncomingSetServiceRequest}.
     * @see ScoController
     */
    public void addOperationInvocationReceiver(OperationInvocationReceiver receiver) {
        scoController.addOperationInvocationReceiver(receiver);
    }

    /**
     * Sends a periodic state report.
     * <p>
     * This function does not control periodicity.
     * Periodicity has to be controlled by the calling function.
     *
     * @param states      map with mds as key and lists of the states that are supposed to be notified as value.
     * @param mdibVersion the MDIB version the report belongs to.
     * @param <T>         the state type.
     */
    public <T extends AbstractState> void sendPeriodicStateReport(
            Map<String, List<T>> states,
            MdibVersion mdibVersion
    ) {
        reportGenerator.sendPeriodicStateReport(states, mdibVersion);
    }

    @MessageInterceptor(ActionConstants.ACTION_GET_MDIB)
    void getMdib(RequestResponseObject requestResponseObject) throws SoapFaultException {
        getRequest(requestResponseObject, GetMdib.class);

        final GetMdibResponse getMdibResponse = messageModelFactory.createGetMdibResponse();

        try (ReadTransaction transaction = mdibAccess.startTransaction()) {
            final MdibMapper mdibMapper = mdibMapperFactory.createMdibMapper(transaction);
            getMdibResponse.setMdib(mdibMapper.mapMdib());

            setResponse(requestResponseObject, getMdibResponse, transaction.getMdibVersion(),
                    ActionConstants.getResponseAction(ActionConstants.ACTION_GET_MDIB));
        }
    }

    @MessageInterceptor(ActionConstants.ACTION_GET_MD_DESCRIPTION)
    void getMdDescription(RequestResponseObject requestResponseObject) throws SoapFaultException {
        final GetMdDescription getMdDescription = getRequest(requestResponseObject, GetMdDescription.class);

        final GetMdDescriptionResponse getMdDescriptionResponse = messageModelFactory.createGetMdDescriptionResponse();

        try (ReadTransaction transaction = mdibAccess.startTransaction()) {
            final MdibMapper mdibMapper = mdibMapperFactory.createMdibMapper(transaction);
            getMdDescriptionResponse.setMdDescription(mdibMapper.mapMdDescription(getMdDescription.getHandleRef()));
            setResponse(requestResponseObject, getMdDescriptionResponse, transaction.getMdibVersion(),
                    ActionConstants.getResponseAction(ActionConstants.ACTION_GET_MD_DESCRIPTION));
        }
    }

    @MessageInterceptor(ActionConstants.ACTION_GET_MD_STATE)
    void getMdState(RequestResponseObject requestResponseObject) throws SoapFaultException {
        final GetMdState getMdState = getRequest(requestResponseObject, GetMdState.class);

        final GetMdStateResponse getMdStateResponse = messageModelFactory.createGetMdStateResponse();

        try (ReadTransaction transaction = mdibAccess.startTransaction()) {
            final MdibMapper mdibMapper = mdibMapperFactory.createMdibMapper(transaction);
            getMdStateResponse.setMdState(mdibMapper.mapMdState(getMdState.getHandleRef()));
            setResponse(requestResponseObject, getMdStateResponse, transaction.getMdibVersion(),
                    ActionConstants.getResponseAction(ActionConstants.ACTION_GET_MD_STATE));
        }
    }

    /**
     * Answers the incoming GetContextStates request.
     * <p>
     * Filters context states according to the following rules:
     * <ul>
     * <li>If the msg:GetContextStates/msg:HandleRef list is empty,
     * all context states in the MDIB SHALL be included in the result list.
     * <li>If a HANDLE reference from the msg:GetContextStates/msg:HandleRef list
     * does match a context descriptor HANDLE,
     * then all context states that belong to the corresponding context descriptor SHALL be included in the result list.
     * <li>If a HANDLE reference from the msg:GetContextStates/msg:HandleRef list
     * does match a context state HANDLE,
     * then the corresponding context state SHALL be included in the result list.
     * <li>If a HANDLE reference from the msg:GetContextStates/msg:HandleRef list does match an MDS descriptor,
     * then all context states that are part of this MDS SHALL be included in the result list.
     * </ul>
     *
     * @param requestResponseObject the request response object that contains the request data
     * @throws SoapFaultException if something went wrong during processing.
     */
    @MessageInterceptor(ActionConstants.ACTION_GET_CONTEXT_STATES)
    void getContextStates(RequestResponseObject requestResponseObject) throws SoapFaultException {
        final GetContextStates getContextStates = getRequest(requestResponseObject, GetContextStates.class);

        final GetContextStatesResponse getContextStatesResponse = messageModelFactory.createGetContextStatesResponse();

        try (ReadTransaction transaction = mdibAccess.startTransaction()) {
            final List<AbstractContextState> contextStates = transaction.getContextStates();
            List<AbstractContextState> filteredContextStates = new ArrayList<>();
            if (getContextStates.getHandleRef().isEmpty()) {
                // R5039: If the msg:GetContextStates/msg:HandleRef list is empty, all context states in the MDIB
                // SHALL be included in the result list.
                filteredContextStates = contextStates;
            } else {
                Set<String> filterSet = new HashSet<>(getContextStates.getHandleRef());
                for (AbstractContextState contextState : contextStates) {
                    var entity = transaction.getEntity(contextState.getDescriptorHandle()).orElseThrow(() ->
                            new SoapFaultException(faultFactory.createReceiverFault(
                                    String.format("Unexpected MDIB inconsistency: context descriptor with handle %s" +
                                                    " not found for context state with handle %s",
                                            contextState.getDescriptorHandle(), contextState.getHandle())))
                    );

                    // R5041: If a HANDLE reference from the msg:GetContextStates/msg:HandleRef list does match
                    // a context state HANDLE, then the corresponding context state SHALL be included in the
                    // result list.
                    var containsStateHandle = filterSet.contains(contextState.getHandle());

                    // R5040: If a HANDLE reference from the msg:GetContextStates/msg:HandleRef list does match
                    // a context descriptor HANDLE, then all context states that belong to the corresponding
                    // context descriptor SHALL be included in the result list.
                    var containsDescrHandle = filterSet.contains(contextState.getDescriptorHandle());

                    // R5042: If a HANDLE reference from the msg:GetContextStates/msg:HandleRef list does match an
                    // MDS descriptor, then all context states that are part of this MDS SHALL be included in the result
                    // list.
                    var containsMdsHandle = filterSet.contains(entity.getParentMds());

                    if (containsStateHandle || containsDescrHandle || containsMdsHandle) {
                        filteredContextStates.add(contextState);
                    }
                }
            }

            getContextStatesResponse.setContextState(filteredContextStates);
            setResponse(requestResponseObject, getContextStatesResponse, transaction.getMdibVersion(),
                    ActionConstants.getResponseAction(ActionConstants.ACTION_GET_CONTEXT_STATES));
        }
    }

    @MessageInterceptor(ActionConstants.ACTION_GET_CONTEXT_STATES_BY_FILTER)
    void getContextStatesByFilter(RequestResponseObject requestResponseObject) throws SoapFaultException {
        // todo DGr implement getContextStatesByFilter
        throw new SoapFaultException(faultFactory.createReceiverFault(
                "GetContextStatesByFilter is not available on this device"),
                requestResponseObject.getRequest().getWsAddressingHeader().getMessageId().orElse(null));
    }

    @MessageInterceptor(ActionConstants.ACTION_GET_CONTEXT_STATES_BY_IDENTIFICATION)
    void getContextStatesByIdentification(RequestResponseObject requestResponseObject) throws SoapFaultException {
        // todo DGr implement getContextStatesByIdentification
        throw new SoapFaultException(faultFactory.createReceiverFault(
                "GetContextStatesByIdentification is not available on this device"),
                requestResponseObject.getRequest().getWsAddressingHeader().getMessageId().orElse(null));
    }

    @MessageInterceptor(ActionConstants.ACTION_SET_VALUE)
    void setValue(RequestResponseObject requestResponseObject) throws SoapFaultException {
        processSetServiceRequest(requestResponseObject, SetValue.class, SetValueResponse.class,
                ActionConstants.getResponseAction(ActionConstants.ACTION_SET_VALUE),
                SetValue::getRequestedNumericValue);
    }

    @MessageInterceptor(ActionConstants.ACTION_SET_STRING)
    void setString(RequestResponseObject requestResponseObject) throws SoapFaultException {
        processSetServiceRequest(requestResponseObject, SetString.class, SetStringResponse.class,
                ActionConstants.getResponseAction(ActionConstants.ACTION_SET_STRING),
                SetString::getRequestedStringValue);
    }

    @MessageInterceptor(ActionConstants.ACTION_ACTIVATE)
    void activate(RequestResponseObject requestResponseObject) throws SoapFaultException {
        processSetServiceRequest(requestResponseObject, Activate.class, ActivateResponse.class,
                ActionConstants.getResponseAction(ActionConstants.ACTION_ACTIVATE),
                Activate::getArgument);
    }

    @MessageInterceptor(ActionConstants.ACTION_SET_COMPONENT_STATE)
    void setComponentState(RequestResponseObject requestResponseObject) throws SoapFaultException {
        processSetServiceRequest(requestResponseObject, SetComponentState.class, SetComponentStateResponse.class,
                ActionConstants.getResponseAction(ActionConstants.ACTION_SET_COMPONENT_STATE),
                SetComponentState::getProposedComponentState);
    }

    @MessageInterceptor(ActionConstants.ACTION_SET_CONTEXT_STATE)
    void setContextState(RequestResponseObject requestResponseObject) throws SoapFaultException {
        processSetServiceRequest(requestResponseObject, SetContextState.class, SetContextStateResponse.class,
                ActionConstants.getResponseAction(ActionConstants.ACTION_SET_CONTEXT_STATE),
                SetContextState::getProposedContextState);
    }

    @MessageInterceptor(ActionConstants.ACTION_SET_ALERT_STATE)
    void setAlertState(RequestResponseObject requestResponseObject) throws SoapFaultException {
        processSetServiceRequest(requestResponseObject, SetAlertState.class, SetAlertStateResponse.class,
                ActionConstants.getResponseAction(ActionConstants.ACTION_SET_ALERT_STATE),
                SetAlertState::getProposedAlertState);
    }

    @MessageInterceptor(ActionConstants.ACTION_SET_METRIC_STATE)
    void setMetricState(RequestResponseObject requestResponseObject) throws SoapFaultException {
        processSetServiceRequest(requestResponseObject, SetMetricState.class, SetMetricStateResponse.class,
                ActionConstants.getResponseAction(ActionConstants.ACTION_SET_METRIC_STATE),
                SetMetricState::getProposedMetricState);
    }

    @MessageInterceptor(ActionConstants.ACTION_GET_DESCRIPTOR)
    void getDescriptor(RequestResponseObject requestResponseObject) throws SoapFaultException {
        final GetDescriptor getDescriptor = getRequest(requestResponseObject, GetDescriptor.class);

        final GetDescriptorResponse getDescriptorResponse = messageModelFactory.createGetDescriptorResponse();

        try (ReadTransaction transaction = mdibAccess.startTransaction()) {
            for (String handle : getDescriptor.getHandleRef()) {
                transaction.getDescriptor(handle).ifPresent(descriptor ->
                        getDescriptorResponse.getDescriptor().add(descriptor)
                );
            }
            setResponse(requestResponseObject, getDescriptorResponse, transaction.getMdibVersion(),
                    ActionConstants.getResponseAction(ActionConstants.ACTION_GET_DESCRIPTOR));
        }
    }

    /**
     * Answers the incoming GetContainmentTree request.
     * <p>
     * The following rules apply based on meaningful/individual interpretation of BICEPS:
     * <ul>
     * <li>The result shall contain containment tree information of all child elements of the particular parent that is
     * matched by the first entry of msg:GetContainmentTree/msg:HandleRef. Others are ignored as they cannot be put to
     * the response message
     * <li>If no handle reference is provided, all containment tree elements on MDS level SHALL be returned.
     * </ul>
     *
     * @param requestResponseObject the request response object that contains the request data
     * @throws SoapFaultException if something went wrong during processing.
     */
    @MessageInterceptor(ActionConstants.ACTION_GET_CONTAINMENT_TREE)
    void getContainmentTree(RequestResponseObject requestResponseObject) throws SoapFaultException {
        final GetContainmentTree getContainmentTree = getRequest(requestResponseObject, GetContainmentTree.class);
        final GetContainmentTreeResponse getContainmentTreeResponse =
                messageModelFactory.createGetContainmentTreeResponse();
        final List<String> handleReferences = getContainmentTree.getHandleRef();
        final ContainmentTree containmentTree = participantModelFactory.createContainmentTree();

        try (ReadTransaction transaction = mdibAccess.startTransaction()) {
            // Collect entities
            List<MdibEntity> filteredEntities;
            if (handleReferences.isEmpty()) {
                filteredEntities = transaction.getRootEntities();
            } else {
                // Only consider first handle as others cannot be answered due to BICEPS XML Schema limitations
                // (request can contain multiple handles, response can only answer to one of them)
                // BICEPS does not define how to compute the result, hence all handles except the first one are ignored)
                filteredEntities = new ArrayList<>();
                var parentEntity = transaction.getEntity(handleReferences.get(0));
                if (parentEntity.isPresent()) {
                    var entity = parentEntity.get();
                    containmentTree.setChildrenCount(entity.getChildren().size());
                    containmentTree.setHandleRef(entity.getHandle());
                    entity.getParent().ifPresent(containmentTree::setParentHandleRef);
                    containmentTree.setEntryType(getContainmentTreeEntryType(entity));

                    entity.getChildren().forEach(childHandle ->
                            transaction.getEntity(childHandle).ifPresent(filteredEntities::add));
                }
            }

            // Prepare response
            for (MdibEntity entity : filteredEntities) {
                final ContainmentTreeEntry entry = participantModelFactory.createContainmentTreeEntry();
                entry.setChildrenCount(entity.getChildren().size());
                entry.setHandleRef(entity.getHandle());
                entity.getParent().ifPresent(entry::setParentHandleRef);
                entry.setType(entity.getDescriptor().getType());
                entry.setEntryType(getContainmentTreeEntryType(entity));
                containmentTree.getEntry().add(entry);
            }

            getContainmentTreeResponse.setContainmentTree(containmentTree);
            setResponse(requestResponseObject, getContainmentTreeResponse, transaction.getMdibVersion(),
                    ActionConstants.getResponseAction(ActionConstants.ACTION_GET_CONTAINMENT_TREE));
        }
    }

    private <T> T getRequest(RequestResponseObject requestResponseObject, Class<T> bodyType) throws SoapFaultException {
        return soapUtil.getBody(requestResponseObject.getRequest(), bodyType).orElseThrow(() ->
                new SoapFaultException(faultFactory.createSenderFault(String.format("%s SOAP request body is malformed",
                        bodyType.getSimpleName())),
                        requestResponseObject.getRequest().getWsAddressingHeader().getMessageId().orElse(null)));
    }

    private <T> void setResponse(RequestResponseObject requestResponseObject,
                                 T response,
                                 MdibVersion mdibVersion,
                                 String responseAction) throws SoapFaultException {
        try {
            mdibVersionUtil.setMdibVersion(mdibVersion, response);
        } catch (Exception e) {
            throw new SoapFaultException(faultFactory.createReceiverFault("Could not create MDIB version."),
                    requestResponseObject.getRequest().getWsAddressingHeader().getMessageId().orElse(null));
        }
        requestResponseObject.getResponse().getWsAddressingHeader().setAction(wsaUtil.createAttributedURIType(
                responseAction));
        soapUtil.setBody(response, requestResponseObject.getResponse());
    }

    private <T extends AbstractSetResponse> T getResponseObjectAsTypeOrThrow(
            InvocationResponse responseData, Class<T> type) throws SoapFaultException {
        try {
            final T response = type.getConstructor().newInstance();
            response.setSequenceId(responseData.getMdibVersion().getSequenceId());
            response.setInstanceId(responseData.getMdibVersion().getInstanceId());
            response.setMdibVersion(responseData.getMdibVersion().getVersion());

            InvocationInfo invocationInfo = new InvocationInfo();
            invocationInfo.setTransactionId(responseData.getTransactionId());
            invocationInfo.setInvocationState(responseData.getInvocationState());
            invocationInfo.setInvocationError(responseData.getInvocationError());
            invocationInfo.setInvocationErrorMessage(responseData.getInvocationErrorMessage());
            response.setInvocationInfo(invocationInfo);
            return response;
        } catch (Exception e) {
            throw new SoapFaultException(faultFactory.createReceiverFault(
                    String.format("Response message could not be generated. Reason: %s", e.getMessage())));
        }
    }

    private InstanceIdentifier determineSource(RequestResponseObject requestResponseObject) {
        final var transportInfo = requestResponseObject.getCommunicationContext().getTransportInfo();

        if (!transportInfo.getX509Certificates().isEmpty()) {
            // An SDC SERVICE PROVIDER SHALL set pm:OperationInvokedReport/pm:ReportPart/pm:InvocationSource to
            //  - @Root = “http://standards.ieee.org/downloads/11073/11073-20701-2018/X509Certificate/PEM”
            //  - @Extension =  being the X.509 certificate of the SDC PARTICIPANT that invoked the SERVICE OPERATION referenced by the enclosing report part, encoded as PEM text.

            // peer certificate is first in chain
            final var peerCert = transportInfo.getX509Certificates().get(0);
            final var stringWriter = new StringWriter();
            try (final var writer = new JcaPEMWriter(stringWriter)) {
                writer.writeObject(peerCert);
            } catch (IOException e) {
                LOG.warn("Could not determine caller source, falling back to anonymous caller", e);
                return anonymousSource;
            }

            final var knownInstanceIdentifier = new InstanceIdentifier();
            knownInstanceIdentifier.setRootName("http://standards.ieee.org/downloads/11073/11073-20701-2018/X509Certificate/PEM");
            knownInstanceIdentifier.setExtensionName(stringWriter.toString());

            return knownInstanceIdentifier;
        }

        return anonymousSource;
    }

    private <T extends AbstractSet, V extends AbstractSetResponse> void processSetServiceRequest(
            RequestResponseObject requestResponseObject,
            Class<T> requestClass,
            Class<V> responseClass,
            String responseAction,
            Function<T, ?> getPayload) throws SoapFaultException {
        T request = getRequest(requestResponseObject, requestClass);
        final InvocationResponse invocationResponse;
        try {
            invocationResponse = scoController.processIncomingSetOperation(
                    request.getOperationHandleRef(),
                    determineSource(requestResponseObject),
                    getPayload.apply(request),
                    soapUtil.determineRequestDistinguishedName(requestResponseObject)
            );
        } catch (Exception e) {
            throw new SoapFaultException(faultFactory.createReceiverFault(
                    String.format("Error while processing set service request: %s", e.getMessage())),
                    requestResponseObject.getRequest().getWsAddressingHeader().getMessageId().orElse(null));
        }

        setResponse(requestResponseObject, getResponseObjectAsTypeOrThrow(invocationResponse, responseClass),
                invocationResponse.getMdibVersion(), responseAction);
    }

    private QName getContainmentTreeEntryType(MdibEntity entity) throws SoapFaultException {
        for (Annotation annotation : entity.getDescriptorClass().getAnnotations()) {
            if (annotation.annotationType() == XmlType.class) {
                return new QName(((XmlType) annotation).namespace(), ((XmlType) annotation).name());
            }
        }
        throw new SoapFaultException(faultFactory.createReceiverFault(String.format(
                "Could not resolve entry type the requested descriptor %s. Operation aborted.", entity.getHandle())));
    }

    @Override
    public void sendNotification(String action, Object payload) throws MarshallingException, TransportException {
        actionBasedEventSource.sendNotification(action, payload);
    }

    @Override
    public void subscriptionEndToAll(WsEventingStatus status) throws TransportException {
        actionBasedEventSource.subscriptionEndToAll(status);
    }
}
