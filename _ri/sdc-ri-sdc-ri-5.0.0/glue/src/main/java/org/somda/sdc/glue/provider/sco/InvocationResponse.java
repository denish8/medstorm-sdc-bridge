package org.somda.sdc.glue.provider.sco;

import org.somda.sdc.biceps.model.message.InvocationError;
import org.somda.sdc.biceps.model.message.InvocationState;
import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.biceps.model.participant.MdibVersion;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Initial response required to answer a set service request.
 * <p>
 * The object is supposed to be created by using {@link Context#createSuccessfulResponse(MdibVersion, InvocationState)}
 * and {@link Context#createUnsuccessfulResponse(MdibVersion, InvocationState, InvocationError, List)}.
 */
public class InvocationResponse {
    private final MdibVersion mdibVersion;
    private final long transactionId;
    private final InvocationState invocationState;
    private final InvocationError invocationError;
    private final List<LocalizedText> invocationErrorMessage;

    InvocationResponse(MdibVersion mdibVersion,
                       long transactionId,
                       InvocationState invocationState,
                       @Nullable InvocationError invocationError,
                       @Nullable List<LocalizedText> invocationErrorMessage) {
        this.mdibVersion = mdibVersion;
        this.transactionId = transactionId;
        this.invocationState = invocationState;
        this.invocationError = invocationError;
        this.invocationErrorMessage = invocationErrorMessage;
    }

    public MdibVersion getMdibVersion() {
        return mdibVersion;
    }

    public long getTransactionId() {
        return transactionId;
    }

    public InvocationState getInvocationState() {
        return invocationState;
    }

    public InvocationError getInvocationError() {
        return invocationError;
    }

    public List<LocalizedText> getInvocationErrorMessage() {
        return invocationErrorMessage;
    }
}
