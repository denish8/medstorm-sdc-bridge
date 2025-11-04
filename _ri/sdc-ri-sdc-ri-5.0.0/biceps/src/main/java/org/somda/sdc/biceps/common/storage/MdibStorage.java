package org.somda.sdc.biceps.common.storage;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.common.access.WriteDescriptionResult;
import org.somda.sdc.biceps.common.access.WriteStateResult;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractMultiState;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.MdibVersion;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Registry-based access to {@linkplain MdibEntity} instances derived
 * from an {@linkplain org.somda.sdc.biceps.model.participant.Mdib}.
 * <p>
 * In this case "registry-based" means that there is fast (hash map) access to any {@link MdibEntity} instance if
 * not mentioned otherwise.
 */
public interface MdibStorage {
    /**
     * Applies description modifications on this object regardless of any consistency checks.
     * <p>
     * Versions are applied without being verified.
     *
     * @param mdibVersion              the MDIB version to apply.
     * @param mdDescriptionVersion     the MD description version to apply. Value null leaves version as is.
     * @param mdStateVersion           the MD state version to apply. Value null leaves version as is.
     * @param descriptionModifications the modifications to apply.
     * @return a result set with inserted, updated and deleted entities.
     */
    WriteDescriptionResult apply(MdibVersion mdibVersion,
                                 @Nullable BigInteger mdDescriptionVersion,
                                 @Nullable BigInteger mdStateVersion,
                                 MdibDescriptionModifications descriptionModifications);

    /**
     * Applies state modifications on this object regardless of any consistency checks.
     * <p>
     * Versions are applied without being verified.
     *
     * @param mdibVersion        the MDIB version to apply.
     * @param mdStateVersion     the MD state version to apply. Value null leaves version as is.
     * @param stateModifications the modifications to apply.
     * @return a result set with updated states.
     */
    WriteStateResult apply(MdibVersion mdibVersion,
                           @Nullable BigInteger mdStateVersion,
                           MdibStateModifications stateModifications);

    /**
     * The latest known MDIB version.
     *
     * @return the latest known MDIB version.
     * @see MdibAccess#getMdibVersion()
     */
    MdibVersion getMdibVersion();

    /**
     * The latest known MD description version.
     *
     * @return the latest known MD description version.
     * @see MdibAccess#getMdDescriptionVersion()
     */
    BigInteger getMdDescriptionVersion();

    /**
     * The latest known MD state version.
     *
     * @return The latest known MD state version.
     * @see MdibAccess#getMdStateVersion()
     */
    BigInteger getMdStateVersion();

    /**
     * Retrieves a specific descriptor of the hosted {@link org.somda.sdc.biceps.model.participant.Mdib}.
     *
     * @param handle     Handle name of the descriptor.
     * @param descrClass Class to cast to. If cast fails, {@link Optional#empty()} will be returned.
     * @param <T>        any abstract descriptor.
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
     * @param <T>        any abstract state.
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
     *
     * @param descriptorHandle the descriptor handle to seek.
     * @param stateClass       the class to filter for.
     * @param <T>              any abstract context state.
     * @return a list of the context states of {@code descriptorHandle}.
     * @see MdibAccess#getContextStates(String, Class)
     */
    <T extends AbstractContextState> List<T> getContextStates(String descriptorHandle, Class<T> stateClass);

    /**
     * Finds all context states of a certain descriptor.
     *
     * @param descriptorHandle the descriptor handle to seek.
     * @return a list of the context states of {@code descriptorHandle}.
     * @see MdibAccess#getContextStates(String)
     */
    List<AbstractContextState> getContextStates(String descriptorHandle);

    /**
     * Finds all multi states of a certain handle.
     *
     * @param descriptorHandle the descriptor handle to seek.
     * @return a list of the multi states of {@code descriptorHandle}.
     */
    List<AbstractMultiState> getMultiStates(String descriptorHandle);

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
     *
     * @param stateClass the class information to filter for.
     * @param <T>        the context type to filter for.
     * @return a list of all context states with the given type.
     */
    <T extends AbstractContextState> List<T> findContextStatesByType(Class<T> stateClass);
}
