package org.somda.sdc.glue.consumer.sco;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.message.AbstractSet;
import org.somda.sdc.biceps.model.message.AbstractSetResponse;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.biceps.model.message.SetContextState;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.glue.common.WsdlConstants;
import org.somda.sdc.glue.consumer.SetServiceAccess;
import org.somda.sdc.glue.consumer.helper.HostingServiceLogger;
import org.somda.sdc.glue.consumer.sco.factory.OperationInvocationDispatcherFactory;
import org.somda.sdc.glue.consumer.sco.factory.ScoTransactionFactory;
import org.somda.sdc.glue.consumer.sco.helper.OperationInvocationDispatcher;
import org.somda.sdc.glue.guice.Consumer;

import javax.annotation.Nullable;

/**
 * Controller class that is responsible for invoking set requests and processing incoming operation invoked reports.
 */
public class ScoController implements SetServiceAccess {
    private static final Logger LOG = LogManager.getLogger(ScoController.class);
    private final HostedServiceProxy setServiceProxy;
    private final HostedServiceProxy contextServiceProxy;
    private final OperationInvocationDispatcher operationInvocationDispatcher;
    private final ExecutorWrapperService<ListeningExecutorService> executorService;
    private final SoapUtil soapUtil;
    private final ScoTransactionFactory scoTransactionFactory;
    private final Logger instanceLogger;

    @AssistedInject
    ScoController(@Assisted HostingServiceProxy hostingServiceProxy,
                  @Assisted("setServiceProxy") @Nullable HostedServiceProxy setServiceProxy,
                  @Assisted("contextServiceProxy") @Nullable HostedServiceProxy contextServiceProxy,
                  OperationInvocationDispatcherFactory operationInvocationDispatcherFactory,
                  @Consumer ExecutorWrapperService<ListeningExecutorService> executorService,
                  SoapUtil soapUtil,
                  ScoTransactionFactory scoTransactionFactory,
                  @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = HostingServiceLogger.getLogger(LOG, hostingServiceProxy, frameworkIdentifier);
        this.setServiceProxy = setServiceProxy;
        this.contextServiceProxy = contextServiceProxy;
        this.operationInvocationDispatcher =
                operationInvocationDispatcherFactory.createOperationInvocationDispatcher(hostingServiceProxy);
        this.executorService = executorService;
        this.soapUtil = soapUtil;
        this.scoTransactionFactory = scoTransactionFactory;

    }

    @Override
    public <T extends AbstractSet, V extends AbstractSetResponse> ListenableFuture<ScoTransaction<V>> invoke(
            T setRequest,
            Class<V> responseClass) {
        return invoke(setRequest, null, responseClass);
    }

    @Override
    public <T extends AbstractSet, V extends AbstractSetResponse> ListenableFuture<ScoTransaction<V>> invoke(
            T setRequest,
            @Nullable java.util.function.Consumer<Pair<OperationInvokedReport.ReportPart, MdibVersion>> reportListener,
            Class<V> responseClass) {
        return executorService.get().submit(() -> {
            instanceLogger.debug("Invoke {} operation with payload: {}",
                    setRequest.getClass().getSimpleName(), setRequest);
            final V response = responseClass.cast(sendMessage(setRequest, responseClass));
            instanceLogger.debug("Received {} message with payload: {}",
                    response.getClass().getSimpleName(), response);

            final ScoTransactionImpl<V> transaction =
                    scoTransactionFactory.createScoTransaction(response, reportListener);

            operationInvocationDispatcher.registerTransaction(transaction);

            return transaction;
        });
    }

    /**
     * Accepts an operation invoked report and dispatches report parts to SCO transactions.
     *
     * @param report the report to dispatch (note that a report can contain multiple report parts
     *               that belong to different transaction.
     * @see ScoTransaction
     */
    public void processOperationInvokedReport(OperationInvokedReport report) {
        operationInvocationDispatcher.dispatchReport(report);
    }

    private <T extends AbstractSet> Object sendMessage(T setRequest, Class<?> expectedResponseClass)
            throws InvocationException {
        String action = WsdlConstants.ACTION_SET_PREFIX + setRequest.getClass().getSimpleName();
        HostedServiceProxy hostedServiceProxy;
        if (setRequest.getClass().equals(SetContextState.class)) {
            if (contextServiceProxy == null) {
                throw new InvocationException("SetContextState request could not be sent: " +
                        "no context service available");
            }
            action = ActionConstants.ACTION_SET_CONTEXT_STATE;
            hostedServiceProxy = contextServiceProxy;
        } else {
            if (setServiceProxy == null) {
                throw new InvocationException("Set request could not be sent: no set service available");
            }
            hostedServiceProxy = setServiceProxy;
        }

        final SoapMessage request = soapUtil.createMessage(action, setRequest);
        try {
            final SoapMessage response = hostedServiceProxy.getRequestResponseClient().sendRequestResponse(request);
            return soapUtil.getBody(response, expectedResponseClass).orElseThrow(() ->
                    new InvocationException("Received unexpected response"));
        } catch (InterceptorException | SoapFaultException | MarshallingException | TransportException e) {
            throw new InvocationException(String.format("Request to %s failed: %s",
                    hostedServiceProxy.getActiveEprAddress(), e.getMessage()), e);
        }
    }
}
