package org.somda.sdc.biceps.common.factory;

import com.google.inject.Inject;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.MdibVersion;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Factory to create {@linkplain MdibEntity} instances.
 */
public class MdibEntityFactory {
    private final MdibEntityGuiceAssistedFactory factory;

    @Inject
    MdibEntityFactory(MdibEntityGuiceAssistedFactory factory) {
        this.factory = factory;
    }

    /**
     * Creates an MDIB entity based on all possible fields.
     *
     * @param parent      the parent of the entity.
     * @param children    the list of child handles.
     * @param descriptor  the descriptor of the entity.
     * @param states      the states of the entity.
     * @param mdibVersion the MDIB version of the entity.
     * @param parentMds   the handle of the Mds that is the root of this entity.
     * @return an {@link MdibEntity} instance.
     */
    public MdibEntity createMdibEntity(@Nullable String parent,
                                       List<String> children,
                                       AbstractDescriptor descriptor,
                                       List<AbstractState> states,
                                       MdibVersion mdibVersion,
                                       String parentMds) {
        return factory.createMdibEntity(parent, children, descriptor, states, mdibVersion, parentMds);
    }

    /**
     * Takes an entity and replaces descriptor and states.
     *
     * @param mdibEntity the entity where descriptors and states are supposed to be replaced.
     * @param descriptor the descriptor to replace.
     * @param states     the states to replace.
     * @return an {@link MdibEntity} instance with replaced descriptor and states.
     */
    public MdibEntity replaceDescriptorAndStates(MdibEntity mdibEntity,
                                                 AbstractDescriptor descriptor,
                                                 List<AbstractState> states) {
        return factory.createMdibEntity(mdibEntity.getParent().orElse(null), mdibEntity.getChildren(),
                descriptor, states, mdibEntity.getLastChanged(), mdibEntity.getParentMds());
    }

    /**
     * Takes an entity and replaces the states.
     *
     * @param mdibEntity the entity where states are supposed to be replaced.
     * @param states     the states to replace.
     * @return an {@link MdibEntity} instance with replaced states.
     */
    public MdibEntity replaceStates(MdibEntity mdibEntity,
                                    List<AbstractState> states) {
        return replaceDescriptorAndStates(mdibEntity, mdibEntity.getDescriptor(), states);
    }

    /**
     * Takes an entity and replaces the children.
     *
     * @param mdibEntity the entity where child handles are supposed to be replaced.
     * @param children   the child handles to replace.
     * @return an {@link MdibEntity} instance with replaced children.
     */
    public MdibEntity replaceChildren(MdibEntity mdibEntity, List<String> children) {
        return factory.createMdibEntity(mdibEntity.getParent().orElse(null), children,
                mdibEntity.getDescriptor(), mdibEntity.getStates(), mdibEntity.getLastChanged(),
            mdibEntity.getParentMds());
    }
}
