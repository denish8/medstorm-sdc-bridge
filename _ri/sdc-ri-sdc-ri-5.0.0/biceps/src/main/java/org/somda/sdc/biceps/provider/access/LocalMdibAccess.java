package org.somda.sdc.biceps.provider.access;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.common.access.MdibAccessObservable;
import org.somda.sdc.biceps.common.access.ReadTransactionProvider;
import org.somda.sdc.biceps.common.access.WriteDescriptionResult;
import org.somda.sdc.biceps.common.access.WriteStateResult;
import org.somda.sdc.biceps.common.storage.PreprocessingException;

/**
 * MDIB read and write access for the BICEPS provider side.
 */
public interface LocalMdibAccess extends MdibAccess, ReadTransactionProvider, MdibAccessObservable {
    /**
     * Processes the description modifications object, stores the data internally and trigger an event.
     * <p>
     * Data is passed through if the elements of the modification change set do not violate the MDIB storage.
     * <p>
     * <em>Attention: description modifications are expensive. Even if this operation allows to change states, it
     * should only be used for changes that affect descriptors.</em>
     *
     * @param mdibDescriptionModifications a set of insertions, updates and deletes.
     * @return a write result including inserted, updates and deleted entities.
     * @throws PreprocessingException if something goes wrong during preprocessing, i.e., the consistency of the MDIB
     *                                would be violated.
     */
    WriteDescriptionResult writeDescription(MdibDescriptionModifications mdibDescriptionModifications)
            throws PreprocessingException;

    /**
     * Processes the state modifications object, stores the data internally and triggers an event.
     * <p>
     * Data is passed through if the elements of the modification change set do not violate the MDIB storage.
     * <p>
     * <em>Hint: this function cheap in terms of runtime and should be used for state changes.</em>
     *
     * @param mdibStateModifications a set of state updates.
     * @return a write result including the updated entities.
     * @throws PreprocessingException if something goes wrong during preprocessing, i.e., the consistency of the MDIB
     *                                would be violated.
     */
    WriteStateResult writeStates(MdibStateModifications mdibStateModifications) throws PreprocessingException;
}
