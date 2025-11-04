package org.somda.sdc.biceps.common.storage;

import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.model.participant.AbstractState;

/**
 * A segment that is applied during state modifications.
 */
public interface StatePreprocessingSegment {
    /**
     * Function that is invoked before the first modification in the processing chain is applied.
     * <p>
     * Default behavior is <em>do nothing</em>.
     *
     * @param modifications all modifications for preprocessing.
     * @param mdibStorage   the MDIB storage to be used by the callback.
     */
    default void beforeFirstModification(MdibStateModifications modifications, MdibStorage mdibStorage) {
    }

    /**
     * In a sequence of modifications this function processes one modification.
     *
     * @param modifications all modifications that are being processed.
     * @param modification  the current modification to be processed.
     * @param storage       the MDIB storage for access.
     * @throws Exception an arbitrary exception if something goes wrong.
     */
    void process(MdibStateModifications modifications, AbstractState modification, MdibStorage storage)
            throws Exception;

    /**
     * Function that is invoked after the last modification in the processing chain has been applied.
     * <p>
     * Default behavior is <em>do nothing</em>.
     *
     * @param modifications all modifications for preprocessing.
     * @param mdibStorage   the MDIB storage to be used by the callback.
     */
    default void afterLastModification(MdibStateModifications modifications, MdibStorage mdibStorage) {
    }
}
