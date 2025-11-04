package org.somda.sdc.biceps.common.event;

import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.common.event.EventMessage;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Base class for any state modification messages.
 *
 * @param <T> the state type that is provided by the {@linkplain StateModificationMessage}.
 */
public class StateModificationMessage<T extends AbstractState>
        extends AbstractMdibAccessMessage implements EventMessage {
    private final Map<String, List<T>> states;

    /**
     * Constructor.
     *
     * @param mdibAccess the MDIB access for {@link AbstractMdibAccessMessage}.
     * @param states     a map containing the changed states for each parent mds.
     */
    protected StateModificationMessage(MdibAccess mdibAccess, Map<String, List<T>> states) {
        super(mdibAccess);
        this.states = Collections.unmodifiableMap(states);
    }

    public Map<String, List<T>> getStates() {
        return states;
    }

}
