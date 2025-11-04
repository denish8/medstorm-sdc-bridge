package org.somda.sdc.biceps.common.event;

import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.model.participant.AbstractAlertState;

import java.util.Map;
import java.util.List;

/**
 * Subscribe to this message in order to receive alert state changes.
 */
public class AlertStateModificationMessage  extends StateModificationMessage<AbstractAlertState> {
    /**
     * Constructor.
     *
     * @param mdibAccess the MDIB access for {@link AbstractMdibAccessMessage}.
     * @param states the states of the message.
     */
    public AlertStateModificationMessage(MdibAccess mdibAccess, Map<String, List<AbstractAlertState>> states) {
        super(mdibAccess, states);
    }
}
