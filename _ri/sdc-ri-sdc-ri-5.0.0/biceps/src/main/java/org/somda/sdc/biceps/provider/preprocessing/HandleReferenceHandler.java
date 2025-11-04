package org.somda.sdc.biceps.provider.preprocessing;

import org.somda.sdc.biceps.common.MdibDescriptionModification;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.storage.DescriptionPreprocessingSegment;
import org.somda.sdc.biceps.common.storage.MdibStorage;

/**
 * Preprocessing segment that appends descriptor references to states.
 * <p>
 * This segment allows the user to spare calling
 * {@link org.somda.sdc.biceps.model.participant.AbstractState#setDescriptorHandle(String)}
 * by automatically setting the descriptor reference at each state.
 */
public class HandleReferenceHandler implements DescriptionPreprocessingSegment {
    @Override
    public void process(MdibDescriptionModifications allModifications,
                        MdibDescriptionModification currentModification,
                        MdibStorage storage) {
        currentModification.getStates().forEach(abstractState ->
                abstractState.setDescriptorHandle(currentModification.getDescriptor().getHandle()));
    }
}
