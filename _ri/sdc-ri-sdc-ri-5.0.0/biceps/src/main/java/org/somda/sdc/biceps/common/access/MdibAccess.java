package org.somda.sdc.biceps.common.access;

import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.MdibVersion;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Read access to an MDIB storage.
 */
public interface MdibAccess {

    /**
     * The latest known MDIB version.
     * <p>
     * This object returns the last known MDIB version which in case of remote access may not necessarily reflect the
     * MDIB version of the whole MDIB.
     * For local access the MDIB version reflects the latest state.
     *
     * @return the latest known MDIB version.
     */
    MdibVersion getMdibVersion();

    /**
     * The latest known MD description version.
     * <p>
     * This object returns the last known MDIB version which in case of remote access may be outdated.
     * For local access the MD description version reflects the latest state.
     *
     * @return the latest known MD description version.
     */
    BigInteger getMdDescriptionVersion();

    /**
     * The latest known MD description version.
     * <p>
     * This object returns the last known MDIB version which in case of remote access may be outdated.
     * For local access the MD description version reflects the latest state.
     *
     * @return The latest known MD state version.
     */
    BigInteger getMdStateVersion();

    /**
     * Retrieves a specific descriptor of the hosted {@link org.somda.sdc.biceps.model.participant.Mdib}.
     *
     * @param handle     Handle name of the descriptor.
     * @param descrClass Class to cast to. If cast fails, {@link Optional#empty()} will be returned.
     * @param <T>        any descriptor type.
     * @return {@link Optional} of the requested descriptor or {@link Optional#empty()} if not found or something
     * went wrong.
     */
    <T extends AbstractDescriptor> Optional<T> getDescriptor(String handle, Class<T> descrClass);

    /**
     * Retrieves a specific abstract descriptor of the hosted {@link org.somda.sdc.biceps.model.participant.Mdib}.
     *
     * @param handle Handle name of the descriptor.
     * @return {@link Optional} of the requested descriptor or {@link Optional#empty()} if not found or something
     * went wrong.
     */
    Optional<AbstractDescriptor> getDescriptor(String handle);

    /**
     * Gets an {@link MdibEntity} object with a specific handle.
     *
     * @param handle the handle to seek.
     * @return the entity of {@link Optional#empty()} if not found.
     */
    Optional<MdibEntity> getEntity(String handle);

    /**
     * Searches all entities that match a specific type.
     *
     * @param type the class to filter for.
     * @param <T>  the descriptor type defined by the class.
     * @return a collection of entities where {@code type} matches.
     */
    <T extends AbstractDescriptor> Collection<MdibEntity> findEntitiesByType(Class<T> type);

    /**
     * Resolves the children of a specific type given a parent handle.
     *
     * @param handle the parent handle of the entity.
     * @param type   The class to filter for.
     * @param <T>    the descriptor type defined by the class.
     * @return a list of children that matches {@code type} (while preserving ordering).
     */
    <T extends AbstractDescriptor> List<MdibEntity> getChildrenByType(String handle, Class<T> type);

    /**
     * Gets all {@link MdibEntity} objects that are root elements.
     *
     * @return the root elements, i.e., entities whose descriptors are of type
     * {@link org.somda.sdc.biceps.model.participant.MdsDescriptor}.
     */
    List<MdibEntity> getRootEntities();

    /**
     * Retrieves a specific abstract state of the hosted {@link org.somda.sdc.biceps.model.participant.Mdib}.
     *
     * @param handle the state or descriptor handle of the state to request (descriptor handle is used in case of
     *               single states).
     * @return {@link Optional} of the requested state or {@link Optional#empty()} if not found or something
     * went wrong.
     */
    Optional<AbstractState> getState(String handle);

    /**
     * Retrieves a specific state of the hosted {@link org.somda.sdc.biceps.model.participant.Mdib}.
     *
     * @param handle     the state or descriptor handle of the state to request (descriptor handle is used in case of
     *                   single states).
     * @param stateClass the class to cast to. If cast fails, {@link Optional#empty()} will be returned.
     * @param <T>        any state type.
     * @return {@link Optional} of the requested state or {@link Optional#empty()} if not found or something
     * went wrong.
     */
    <T extends AbstractState> Optional<T> getState(String handle, Class<T> stateClass);

    /**
     * Gets all states of a specific type.
     *
     * @param stateClass the class information to filter for.
     * @param <T>        the state type to filter for.
     * @return a list of all states with the given type.
     */
    <T extends AbstractState> List<T> getStatesByType(Class<T> stateClass);

    /**
     * Finds all context states of a certain descriptor given a state class.
     * <p>
     * <em>Attention: collections may be created on function call, hence be careful with performance issues.</em>
     *
     * @param descriptorHandle the descriptor handle to seek.
     * @param stateClass       the class to filter for.
     * @param <T>              any context state type.
     * @return a list of the context states of {@code descriptorHandle}.
     */
    <T extends AbstractContextState> List<T> getContextStates(String descriptorHandle, Class<T> stateClass);

    /**
     * Finds all context states of a certain descriptor.
     * <p>
     * <em>Attention: collections may be created on function call, hence be careful with performance issues.</em>
     *
     * @param descriptorHandle the descriptor handle to seek.
     * @return a list of the context states of {@code descriptorHandle}.
     */
    List<AbstractContextState> getContextStates(String descriptorHandle);

    /**
     * Gets all context states.
     * <p>
     * <em>Attention: collections may be created on function call, hence be careful with performance issues.</em>
     *
     * @return a list of all context states.
     */
    List<AbstractContextState> getContextStates();

    /**
     * Gets all context states of a specific type.
     * <p>
     * <em>Attention: collections may be created on function call, hence be careful with performance issues.</em>
     *
     * @param stateClass the class information to filter for.
     * @param <T>        the type to filter for.
     * @return a list of all context states of the specific type.
     */
    <T extends AbstractContextState> List<T> findContextStatesByType(Class<T> stateClass);

}
