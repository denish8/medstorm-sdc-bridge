package org.somda.sdc.biceps.common.event;

import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.model.participant.AbstractOperationState;

import java.util.List;
import java.util.Map;

/**
 * Subscribe to this message in order to receive operation state changes.
 */
public class OperationStateModificationMessage extends StateModificationMessage<AbstractOperationState> {
    /**
     * Constructor.
     *
     * @param mdibAccess the MDIB access for {@link AbstractMdibAccessMessage}.
     * @param states the states of the message.
     */
    public OperationStateModificationMessage(MdibAccess mdibAccess, Map<String, List<AbstractOperationState>> states) {
        super(mdibAccess, states);
    }
}
