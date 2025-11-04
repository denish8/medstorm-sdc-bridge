package org.somda.sdc.biceps.common.storage;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.biceps.common.MdibDescriptionModification;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.MdibTypeValidator;
import org.somda.sdc.biceps.model.participant.AbstractMultiState;
import org.somda.sdc.biceps.model.participant.AbstractState;

import java.util.List;
import java.util.Optional;

/**
 * Provides a processing chain that is supposed to be run before any interaction with
 * an {@linkplain MdibStorage} instance.
 * <p>
 * The {@linkplain MdibStoragePreprocessingChain} offers processing functions for description and state change sets.
 */
public class MdibStoragePreprocessingChain {
    private final MdibStorage mdibStorage;
    private final List<DescriptionPreprocessingSegment> descriptionChainSegments;
    private final List<StatePreprocessingSegment> stateChainSegments;
    private final MdibTypeValidator typeValidator;

    @Inject
    MdibStoragePreprocessingChain(@Assisted MdibStorage mdibStorage,
                                  @Assisted List<DescriptionPreprocessingSegment> descriptionChainSegments,
                                  @Assisted List<StatePreprocessingSegment> stateChainSegments,
                                  MdibTypeValidator typeValidator) {
        this.mdibStorage = mdibStorage;
        this.descriptionChainSegments = descriptionChainSegments;
        this.stateChainSegments = stateChainSegments;
        this.typeValidator = typeValidator;
    }

    /**
     * Accepts a set of description modifications and applies them on every available description chain segment.
     *
     * @param modifications the modification to pass to the chain segments.
     * @throws PreprocessingException in case a chain segment fails.
     */
    public void processDescriptionModifications(MdibDescriptionModifications modifications)
            throws PreprocessingException {
        final List<MdibDescriptionModification> modificationList = modifications.getModifications();
        int sizeToIterate = modificationList.size();

        descriptionChainSegments.forEach(chainSegment ->
                chainSegment.beforeFirstModification(modifications, mdibStorage)
        );

        for (int i = 0; i < sizeToIterate; ++i) {
            for (DescriptionPreprocessingSegment chainSegment : descriptionChainSegments) {
                try {
                    chainSegment.process(modifications, modificationList.get(i), mdibStorage);
                // CHECKSTYLE.OFF: IllegalCatch
                } catch (Exception e) {
                // CHECKSTYLE.ON: IllegalCatch
                    throw new PreprocessingException(e.getMessage(), e.getCause(),
                            modificationList.get(i).getHandle(), chainSegment.toString());
                }
            }
            sizeToIterate = modificationList.size();
        }

        descriptionChainSegments.forEach(chainSegment ->
                chainSegment.afterLastModification(modifications, mdibStorage)
        );
    }

    /**
     * Accepts a set of state modifications and applies them on every available state chain segment.
     *
     * @param modifications the modification to pass to the chain segments.
     * @throws PreprocessingException in case a chain segment fails.
     */
    public void processStateModifications(MdibStateModifications modifications) throws PreprocessingException {
        stateChainSegments.forEach(chainSegment -> chainSegment.beforeFirstModification(modifications, mdibStorage));

        for (AbstractState modification : modifications.getStates()) {
            for (StatePreprocessingSegment chainSegment : stateChainSegments) {
                try {
                    chainSegment.process(modifications, modification, mdibStorage);
                // CHECKSTYLE.OFF: IllegalCatch
                } catch (Exception e) {
                // CHECKSTYLE.ON: IllegalCatch
                    final Optional<AbstractMultiState> multiState = typeValidator.toMultiState(modification);
                    String handle = modification.getDescriptorHandle();
                    if (multiState.isPresent()) {
                        handle = multiState.get().getHandle();
                    }

                    throw new PreprocessingException(e.getMessage(), e.getCause(), handle, chainSegment.toString());
                }
            }
        }

        stateChainSegments.forEach(chainSegment -> chainSegment.afterLastModification(modifications, mdibStorage));
    }
}
