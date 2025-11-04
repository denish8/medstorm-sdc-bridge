package org.somda.sdc.biceps.consumer.access;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.common.access.MdibAccessObservable;
import org.somda.sdc.biceps.common.access.ReadTransactionProvider;
import org.somda.sdc.biceps.common.access.WriteDescriptionResult;
import org.somda.sdc.biceps.common.access.WriteStateResult;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.model.participant.MdibVersion;

import javax.annotation.Nullable;
import java.math.BigInteger;

/**
 * MDIB read and write access for the BICEPS consumer side.
 */
public interface RemoteMdibAccess extends MdibAccess, ReadTransactionProvider, MdibAccessObservable {
    /**
     * Processes the description modifications object, stores the data internally and trigger an event.
     * <p>
     * Data is passed through if the elements of the modification change set do not violate the MDIB storage.
     * <p>
     * <em>Attention: description modifications are expensive. Even if this operation allows to change states, it
     * should only be used for changes that affect descriptors.</em>
     *
     * @param mdibVersion                  the MDIB version to apply.
     * @param mdDescriptionVersion         the MD description version to apply. Leave null if unknown.
     * @param mdStateVersion               the MD state version to apply. Leave null if unknown.
     * @param mdibDescriptionModifications a set of insertions, updates and deletes.
     * @return a write result including inserted, updates and deleted entities.
     * @throws PreprocessingException if something goes wrong during preprocessing, i.e., the consistency of the MDIB
     *                                would be violated.
     */
    WriteDescriptionResult writeDescription(MdibVersion mdibVersion,
                                            @Nullable BigInteger mdDescriptionVersion,
                                            @Nullable BigInteger mdStateVersion,
                                            MdibDescriptionModifications mdibDescriptionModifications)
            throws PreprocessingException;

    /**
     * Processes the state modifications object, stores the data internally and triggers an event.
     * <p>
     * Data is passed through if the elements of the modification change set do not violate the MDIB storage.
     * <p>
     * <em>Hint: this function cheap in terms of runtime and should be used for state changes.</em>
     *
     * @param mdibVersion            the MDIB version to apply.
     * @param mdibStateModifications a set of state updates.
     * @return a write result including the updated entities.
     * @throws PreprocessingException if something goes wrong during preprocessing, i.e., the consistency of the MDIB
     *                                would be violated.
     */
    WriteStateResult writeStates(MdibVersion mdibVersion,
                                 MdibStateModifications mdibStateModifications) throws PreprocessingException;
}
