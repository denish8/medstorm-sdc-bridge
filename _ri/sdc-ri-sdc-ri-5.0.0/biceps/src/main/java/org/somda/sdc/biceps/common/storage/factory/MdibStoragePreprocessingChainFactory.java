package org.somda.sdc.biceps.common.storage.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.biceps.common.storage.DescriptionPreprocessingSegment;
import org.somda.sdc.biceps.common.storage.MdibStorage;
import org.somda.sdc.biceps.common.storage.MdibStoragePreprocessingChain;
import org.somda.sdc.biceps.common.storage.StatePreprocessingSegment;

import java.util.List;

/**
 * Factory to create {@linkplain MdibStoragePreprocessingChain} instances.
 */
public interface MdibStoragePreprocessingChainFactory {
    /**
     * Creates a new {@linkplain MdibStoragePreprocessingChain} instance.
     *
     * @param mdibStorage              the MDIB storage the processing chain belongs to.
     * @param descriptionChainSegments a list of description processing segments.
     *                                 The segments are applied in the same order as inserted to the list.
     * @param stateChainSegments       a list of state processing segments.
     *                                 The segments are applied in the same order as inserted to the list.
     * @return a new {@link MdibStoragePreprocessingChain} instance.
     */
    MdibStoragePreprocessingChain createMdibStoragePreprocessingChain(
            @Assisted MdibStorage mdibStorage,
            @Assisted List<DescriptionPreprocessingSegment> descriptionChainSegments,
            @Assisted List<StatePreprocessingSegment> stateChainSegments
    );
}
