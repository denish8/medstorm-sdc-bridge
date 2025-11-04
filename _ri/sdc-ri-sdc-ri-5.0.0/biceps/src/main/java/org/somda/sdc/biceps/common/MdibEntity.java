package org.somda.sdc.biceps.common;

import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractMultiState;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.MdibVersion;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Representation of any MDIB object within a containment tree.
 * <p>
 * Any entity contains a list of states, a descriptor and an optional set of children.
 * {@linkplain MdibEntity} is an immutable class with read access only.
 */
public interface MdibEntity {
    /**
     * Gets the MDIB version of the entity where it has been changed the last time.
     *
     * @return an MDIB version.
     */
    MdibVersion getLastChanged();

    /**
     * Obtain a read-only list of the MDIB entity's child handles.
     *
     * @return readable list of children currently associated with the entity.
     */
    List<String> getChildren();

    /**
     * Obtain the entity's descriptor.
     *
     * @return descriptor information currently associated with the entity.
     */
    AbstractDescriptor getDescriptor();

    /**
     * Obtains the entity's descriptor as a given type.
     *
     * @param theClass the class to cast.
     * @param <T>      the descriptor class type.
     * @return descriptor information currently associated with the entity.
     */
    <T extends AbstractDescriptor> Optional<T> getDescriptor(Class<T> theClass);

    /**
     * Unique name to identify the entity.
     * <p>
     * Short-hand for {@code getDescriptor().getHandle()}
     *
     * @return the entity's unique handle.
     */
    String getHandle();

    /**
     * Obtains the entity's parent handle.
     *
     * @return the handle of the parent if known, otherwise {@linkplain Optional#empty()}.
     */
    Optional<String> getParent();

    /**
     * Obtains a copy of the MDIB entity's state list.
     *
     * @return list of states currently associated with the entity.
     */
    List<AbstractState> getStates();

    /**
     * Obtains the list of states cast to a specific type.
     *
     * @param theClass the class to cast.
     * @param <T>      the state class type.
     * @return list of cast states currently associated with the entity.
     * Please note that this list is empty either if there is no state available or there is no state available with
     * the given type information.
     */
    <T extends AbstractState> List<T> getStates(Class<T> theClass);

    /**
     * Obtains the first state if available.
     * <p>
     * This function is useful to retrieve single state information.
     *
     * @param theClass the class to cast.
     * @param <T>      the state class type.
     * @return the cast type of {@linkplain Optional#empty()} if there is no first state or a class cast error.
     */
    <T extends AbstractState> Optional<T> getFirstState(Class<T> theClass);

    /**
     * Convenience method to execute code if en entity hosts a single-state.
     *
     * @param consumer lambda that is supposed to be executed if the entity is a single state host.
     * @return an alternative to execute if the opposite turns out.
     */
    StateAlternative<List<AbstractMultiState>> doIfSingleState(Consumer<AbstractState> consumer);

    /**
     * Convenience method to execute code if en entity hosts a multi-state.
     *
     * @param consumer lambda that is supposed to be executed if the entity is a multi-state host.
     * @return an alternative to execute if the opposite turns out.
     */
    StateAlternative<AbstractState> doIfMultiState(Consumer<List<AbstractMultiState>> consumer);

    /**
     * Gets the descriptor class.
     * <p>
     * Short-hand for {@code getDescriptor().getClass()}.
     *
     * @return the descriptor class of the entity.
     */
    Class<? extends AbstractDescriptor> getDescriptorClass();

    /**
     * Gets the state class.
     *
     * @return the state class of the entity.
     */
    Class<? extends AbstractState> getStateClass();

    /**
     * Returns the handle of the Mds that is the root of this entity.
     *
     * If the entity is an Mds, the handle of the Mds is returned.
     *
     * @return the handle of the Mds that is the root of this entity
     */
    String getParentMds();

    /**
     * An alternative lambda to be executed.
     *
     * @param <T> the state type to accept.
     * @see #doIfSingleState(Consumer)
     * @see #doIfMultiState(Consumer)
     */
    interface StateAlternative<T> {
        void orElse(Consumer<T> consumer);
    }
}
