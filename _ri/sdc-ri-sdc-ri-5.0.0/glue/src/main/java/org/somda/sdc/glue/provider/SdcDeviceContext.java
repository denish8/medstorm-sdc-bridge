package org.somda.sdc.glue.provider;

import com.google.common.util.concurrent.Service;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.dpws.device.Device;
import org.somda.sdc.glue.provider.sco.OperationInvocationReceiver;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Context data passed to {@linkplain SdcDevicePlugin} in order to access {@linkplain SdcDevice} data.
 */
public interface SdcDeviceContext {
    /**
     * Gets the encapsulated device of an {@linkplain SdcDevice}.
     * <p>
     * <em>Do not call any startup or shutdown functions from that device reference as it is still managed by the
     * enclosing {@link SdcDevice} instance that provides this context.</em>
     *
     * @return the DPWS device.
     */
    Device getDevice();

    /**
     * Gets the {@linkplain LocalMdibAccess} passed to the {@linkplain SdcDevice} constructor.
     *
     * @return the local MDIB access.
     */
    LocalMdibAccess getLocalMdibAccess();

    /**
     * Gets a collection of {@linkplain OperationInvocationReceiver} instances
     * passed to the {@linkplain SdcDevice} constructor.
     *
     * @return an unmodifiable {@link OperationInvocationReceiver} collection.
     */
    Collection<OperationInvocationReceiver> getOperationInvocationReceivers();

    /**
     * Gets the enclosing {@link SdcDevice} service state.
     *
     * @return {@link SdcDevice#state()} of the enclosing SDC device.
     */
    Service.State getServiceState();

    /**
     * Sends a periodic state report.
     * <p>
     * This function does not control periodicity.
     * Periodicity has to be controlled by the calling function.
     * <p>
     * Note that only one report type is supported per call, e.g. it is not possible to mix metric and context states.
     * In accordance with the SDC, the following state types have to be sent in separate reports:
     * <ul>
     * <li>Metric states (every subclass of {@link org.somda.sdc.biceps.model.participant.AbstractMetricState})
     * <li>Alert states (every subclass of {@link org.somda.sdc.biceps.model.participant.AbstractAlertState})
     * <li>Context states (every subclass of {@link org.somda.sdc.biceps.model.participant.AbstractContextState})
     * <li>Operational states (every subclass of {@link org.somda.sdc.biceps.model.participant.AbstractOperationState})
     * <li>Component states (every subclass of
     * {@link org.somda.sdc.biceps.model.participant.AbstractDeviceComponentState})
     * </ul>
     *
     * @param states      map with mds as key and lists of the states that are supposed to be notified as value.
     * @param mdibVersion the MDIB version the report belongs to.
     * @param <T>         the state type that.
     */
    <T extends AbstractState> void sendPeriodicStateReport(Map<String, List<T>> states, MdibVersion mdibVersion);
}
