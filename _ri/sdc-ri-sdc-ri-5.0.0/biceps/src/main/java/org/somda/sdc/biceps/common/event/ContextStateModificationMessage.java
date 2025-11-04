package org.somda.sdc.biceps.common.event;

import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.model.participant.AbstractContextState;

import java.util.List;
import java.util.Map;

/**
 * Subscribe to this message in order to receive context state changes.
 */
public class ContextStateModificationMessage extends StateModificationMessage<AbstractContextState> {
    /**
     * Constructor.
     *
     * @param mdibAccess the MDIB access for {@link AbstractMdibAccessMessage}.
     * @param states the states of the message.
     */
    public ContextStateModificationMessage(MdibAccess mdibAccess, Map<String, List<AbstractContextState>> states) {
        super(mdibAccess, states);
    }
}
