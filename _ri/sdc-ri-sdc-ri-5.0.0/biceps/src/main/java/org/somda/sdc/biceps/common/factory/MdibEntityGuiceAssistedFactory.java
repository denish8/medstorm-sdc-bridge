package org.somda.sdc.biceps.common.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Guice-based factory internally used by {@linkplain MdibEntityFactory}.
 *
 * @see MdibEntityFactory
 */
public interface MdibEntityGuiceAssistedFactory {
    /**
     * Creates an MDIB entity based on all possible fields.
     *
     * @param parent the parent of the entity.
     * @param children the list of child handles.
     * @param descriptor the descriptor of the entity.
     * @param states the states of the entity.
     * @param mdibVersion the MDIB version of the entity.
     * @param parentMds the handle of the Mds that is the root of this entity.
     * @return an {@link MdibEntity} instance.
     */
    MdibEntity createMdibEntity(@Assisted @Nullable String parent,
                                @Assisted("children") List<String> children,
                                @Assisted AbstractDescriptor descriptor,
                                @Assisted("states") List<AbstractState> states,
                                @Assisted MdibVersion mdibVersion,
                                @Assisted("parentMds") String parentMds
    );
}
