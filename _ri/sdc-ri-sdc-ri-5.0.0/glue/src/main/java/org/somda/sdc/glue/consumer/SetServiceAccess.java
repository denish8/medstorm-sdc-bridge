package org.somda.sdc.glue.consumer;

import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.lang3.tuple.Pair;
import org.somda.sdc.biceps.model.message.AbstractSet;
import org.somda.sdc.biceps.model.message.AbstractSetResponse;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.glue.consumer.sco.ScoTransaction;

import javax.annotation.Nullable;

/**
 * API to invoke set operations.
 */
public interface SetServiceAccess {
    /**
     * Invokes a set operation.
     * <p>
     * If there is no service to access the SCO, this function returns with a cancelled future.
     *
     * @param setRequest    the set request to send to the remote peer.
     * @param responseClass the expected response class (due to type erasure cannot be designated generically.
     * @param <T>           the set request type.
     * @param <V>           the set response type.
     * @return a future to listen for the SCO transaction (which includes the set response).
     */
    <T extends AbstractSet, V extends AbstractSetResponse> ListenableFuture<ScoTransaction<V>> invoke(
            T setRequest,
            Class<V> responseClass);

    /**
     * Invokes a set operation.
     * <p>
     * If there is no service to access the SCO, this function returns with a cancelled future.
     *
     * @param setRequest     the set request to send to the remote peer.
     * @param responseClass  the expected response class (due to type erasure cannot be designated generically.
     * @param reportListener a callback function that accepts reports being received after invocation.
     * @param <T>            the set request type.
     * @param <V>            the set response type.
     * @return a future to listen for the SCO transaction (which includes the set response).
     */
    <T extends AbstractSet, V extends AbstractSetResponse> ListenableFuture<ScoTransaction<V>> invoke(
            T setRequest,
            @Nullable java.util.function.Consumer<Pair<OperationInvokedReport.ReportPart, MdibVersion>> reportListener,
            Class<V> responseClass);
}
