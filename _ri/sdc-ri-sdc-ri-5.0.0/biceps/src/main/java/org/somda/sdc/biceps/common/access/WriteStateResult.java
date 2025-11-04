package org.somda.sdc.biceps.common.access;

import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.common.model.ObjectStringifier;
import org.somda.sdc.common.model.Stringified;

import java.util.List;
import java.util.Map;

/**
 * Read-only result set of a write states call.
 */
public class WriteStateResult {
    @Stringified
    private final MdibVersion mdibVersion;
    private final Map<String, List<AbstractState>> states;

    /**
     * Constructor to initialize all values of the result set.
     *
     * @param mdibVersion the MDIB version.
     * @param states      all updated states.
     */
    public WriteStateResult(MdibVersion mdibVersion,
                            Map<String, List<AbstractState>> states) {
        this.mdibVersion = mdibVersion;
        this.states = states;
    }

    /**
     * Gets the MDIB version that ensued during the preceding write operation.
     *
     * @return the MDIB version.
     */
    public MdibVersion getMdibVersion() {
        return mdibVersion;
    }

    /**
     * Gets all updated states with their source mds as key.
     *
     * @return the states.
     */
    public Map<String, List<AbstractState>> getStates() {
        return states;
    }

    @Override
    public String toString() {
        return ObjectStringifier.stringify(this);
    }
}
