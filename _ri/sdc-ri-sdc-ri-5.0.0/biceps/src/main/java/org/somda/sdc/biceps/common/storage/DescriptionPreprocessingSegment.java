package org.somda.sdc.biceps.common.storage;

import org.somda.sdc.biceps.common.MdibDescriptionModification;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;

/**
 * A segment that is applied during description modifications.
 */
public interface DescriptionPreprocessingSegment {
    /**
     * Function that is invoked before the first modification in the processing chain is applied.
     * <p>
     * Default behavior is <em>do nothing</em>.
     *
     * @param modifications all modifications for preprocessing.
     * @param mdibStorage   the MDIB storage to be used by the callback.
     */
    default void beforeFirstModification(MdibDescriptionModifications modifications, MdibStorage mdibStorage) {
    }

    /**
     * Function that is invoked after the last modification in the processing chain has been applied.
     * <p>
     * Default behavior is <em>do nothing</em>.
     *
     * @param modifications all modifications for preprocessing.
     * @param mdibStorage   the MDIB storage to be used by the callback.
     */
    default void afterLastModification(MdibDescriptionModifications modifications, MdibStorage mdibStorage) {
    }

    /**
     * In a sequence of modifications this function processes one modification.
     *
     * @param allModifications    all modifications.
     * @param currentModification the current modification to be processed.
     * @param storage             the MDIB storage for access.
     * @throws Exception an arbitrary exception if something goes wrong.
     */
    void process(MdibDescriptionModifications allModifications,
                 MdibDescriptionModification currentModification,
                 MdibStorage storage) throws Exception;
}
