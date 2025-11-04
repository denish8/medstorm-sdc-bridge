package org.somda.sdc.dpws.soap.wstransfer;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import org.somda.sdc.dpws.guice.NetworkJobThreadPool;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.ReferenceParametersType;

/**
 * Default implementation of {@link TransferGetClient}.
 */
public class TransferGetClientImpl implements TransferGetClient {
    private final ExecutorWrapperService<ListeningExecutorService> executorService;
    private final SoapUtil soapUtil;

    @Inject
    TransferGetClientImpl(@NetworkJobThreadPool ExecutorWrapperService<ListeningExecutorService> executorService,
                          SoapUtil soapUtil) {
        this.executorService = executorService;
        this.soapUtil = soapUtil;
    }

    @Override
    public ListenableFuture<SoapMessage> sendTransferGet(RequestResponseClient requestResponseClient, String wsaTo) {
        return executorService.get().submit(() -> requestResponseClient.sendRequestResponse(
                soapUtil.createMessage(WsTransferConstants.WSA_ACTION_GET, wsaTo)));
    }

    @Override
    public ListenableFuture<SoapMessage> sendTransferGet(
            RequestResponseClient requestResponseClient,
            String wsaTo,
            ReferenceParametersType referenceParametersType
    ) {
        return executorService.get().submit(() -> {
            var request = soapUtil.createMessage(WsTransferConstants.WSA_ACTION_GET, wsaTo);
            request.getOriginalEnvelope().getHeader().getAny().add(referenceParametersType);
            return requestResponseClient.sendRequestResponse(request);
        });
    }
}
