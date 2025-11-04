package org.somda.sdc.biceps.common.event;

import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.model.participant.AbstractDeviceComponentState;

import java.util.List;
import java.util.Map;

/**
 * Subscribe to this message in order to receive component state changes.
 * <p>
 * <em>Remark: components are clock, battery, MDS, VMD, channels, SCO and system context.</em>
 */
public class ComponentStateModificationMessage extends StateModificationMessage<AbstractDeviceComponentState> {
    /**
     * Constructor.
     *
     * @param mdibAccess the MDIB access for {@link AbstractMdibAccessMessage}.
     * @param states the states of the message.
     */
    public ComponentStateModificationMessage(
        MdibAccess mdibAccess, Map<String,
        List<AbstractDeviceComponentState>> states
    ) {
        super(mdibAccess, states);
    }
}
