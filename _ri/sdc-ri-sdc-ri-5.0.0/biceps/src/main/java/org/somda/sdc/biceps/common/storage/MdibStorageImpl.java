package org.somda.sdc.biceps.common.storage;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.common.MdibDescriptionModification;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.MdibTypeValidator;
import org.somda.sdc.biceps.common.access.WriteDescriptionResult;
import org.somda.sdc.biceps.common.access.WriteStateResult;
import org.somda.sdc.biceps.common.factory.MdibEntityFactory;
import org.somda.sdc.biceps.common.storage.helper.MdibStorageUtil;
import org.somda.sdc.biceps.model.participant.AbstractContextDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractMultiState;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.ContextAssociation;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.biceps.model.participant.MdsDescriptor;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Default implementation of {@linkplain MdibStorage}.
 */
public class MdibStorageImpl implements MdibStorage {
    private static final Logger LOG = LogManager.getLogger(MdibStorageImpl.class);

    private final MdibEntityFactory entityFactory;
    private final MdibStorageUtil util;
    private final MdibTypeValidator typeValidator;
    private final Logger instanceLogger;
    private final Boolean storeNotAssociatedContextStates;

    private MdibVersion mdibVersion;
    private BigInteger mdDescriptionVersion;
    private BigInteger mdStateVersion;

    private Map<String, MdibEntity> entities;
    private ArrayList<String> rootEntities;
    private Map<String, AbstractContextState> contextStates;


    @AssistedInject
    MdibStorageImpl(MdibEntityFactory entityFactory,
                    MdibStorageUtil util,
                    MdibTypeValidator typeValidator,
                    @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                    @Named(org.somda.sdc.biceps.common.CommonConfig.STORE_NOT_ASSOCIATED_CONTEXT_STATES)
                            Boolean storeNotAssociatedContextStates) {
        this(
                MdibVersion.create(), BigInteger.valueOf(-1),
                BigInteger.valueOf(-1), entityFactory, util,
                typeValidator, frameworkIdentifier,
                storeNotAssociatedContextStates
        );
    }

    @AssistedInject
    MdibStorageImpl(@Assisted MdibVersion initialMdibVersion,
                    MdibEntityFactory entityFactory,
                    MdibStorageUtil util,
                    MdibTypeValidator typeValidator,
                    @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                    @Named(org.somda.sdc.biceps.common.CommonConfig.STORE_NOT_ASSOCIATED_CONTEXT_STATES)
                            Boolean storeNotAssociatedContextStates) {
        this(
                initialMdibVersion, BigInteger.valueOf(-1),
                BigInteger.valueOf(-1), entityFactory, util,
                typeValidator, frameworkIdentifier,
                storeNotAssociatedContextStates
        );
    }

    @AssistedInject
    MdibStorageImpl(@Assisted MdibVersion initialMdibVersion,
                    @Assisted("mdDescriptionVersion") BigInteger mdDescriptionVersion,
                    @Assisted("mdStateVersion") BigInteger mdStateVersion,
                    MdibEntityFactory entityFactory,
                    MdibStorageUtil util,
                    MdibTypeValidator typeValidator,
                    @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                    @Named(org.somda.sdc.biceps.common.CommonConfig.STORE_NOT_ASSOCIATED_CONTEXT_STATES)
                            Boolean storeNotAssociatedContextStates) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.mdibVersion = initialMdibVersion;
        this.mdDescriptionVersion = mdDescriptionVersion;
        this.mdStateVersion = mdStateVersion;
        this.entityFactory = entityFactory;
        this.util = util;
        this.typeValidator = typeValidator;
        this.storeNotAssociatedContextStates = storeNotAssociatedContextStates;

        this.entities = new HashMap<>();
        this.rootEntities = new ArrayList<>();
        this.contextStates = new HashMap<>();
    }

    public <T extends AbstractDescriptor> Optional<T> getDescriptor(String handle, Class<T> descrClass) {
        final MdibEntity mdibEntity = entities.get(handle);
        if (mdibEntity != null) {
            return util.exposeInstance(mdibEntity.getDescriptor(), descrClass);
        }
        return Optional.empty();
    }

    @Override
    public Optional<AbstractDescriptor> getDescriptor(String handle) {
        return getDescriptor(handle, AbstractDescriptor.class);
    }

    public Optional<MdibEntity> getEntity(String handle) {
        return Optional.ofNullable(entities.get(handle));
    }

    @Override
    public <T extends AbstractDescriptor> Collection<MdibEntity> findEntitiesByType(Class<T> type) {
        List<MdibEntity> result = new ArrayList<>();
        for (MdibEntity entity : entities.values()) {
            if (type.isAssignableFrom(entity.getDescriptor().getClass())) {
                result.add(entity);
            }
        }
        return result;
    }

    @Override
    public <T extends AbstractDescriptor> List<MdibEntity> getChildrenByType(String handle, Class<T> type) {
        final List<MdibEntity> result = new ArrayList<>();
        final Optional<MdibEntity> entity = getEntity(handle);
        if (entity.isEmpty()) {
            return result;
        }

        for (String child : entity.get().getChildren()) {
            getEntity(child).ifPresent(childEntity ->
            {
                if (type.isAssignableFrom(childEntity.getDescriptor().getClass())) {
                    result.add(childEntity);
                }
            });
        }

        return result;
    }

    public List<MdibEntity> getRootEntities() {
        return util.exposeEntityList(entities, rootEntities);
    }

    public Optional<AbstractState> getState(String handle) {
        return getState(handle, AbstractState.class);
    }

    public <T extends AbstractState> Optional<T> getState(String handle, Class<T> stateClass) {
        final MdibEntity entity = entities.get(handle);
        if (entity != null && entity.getStates().size() == 1) {
            return util.exposeInstance(entity.getStates().get(0), stateClass);
        }
        final AbstractContextState contextState = contextStates.get(handle);
        return util.exposeInstance(contextState, stateClass);
    }

    @Override
    public <T extends AbstractState> List<T> getStatesByType(Class<T> stateClass) {
        var result = new ArrayList<T>();
        for (MdibEntity entity : entities.values()) {
            entity.getStates().forEach(state -> {
                if (stateClass.isAssignableFrom(state.getClass())) {
                    result.add(stateClass.cast(state));
                }
            });
        }
        return result;
    }

    @Override
    public <T extends AbstractContextState> List<T> getContextStates(String descriptorHandle, Class<T> stateClass) {
        final MdibEntity entity = entities.get(descriptorHandle);
        if (entity == null || entity.getStates().isEmpty()) {
            return Collections.emptyList();
        }
        return util.exposeListOfType(entity.getStates(), stateClass);
    }

    @Override
    public List<AbstractContextState> getContextStates(String descriptorHandle) {
        final MdibEntity entity = entities.get(descriptorHandle);
        if (entity == null || entity.getStates().isEmpty()) {
            return Collections.emptyList();
        }
        return util.exposeListOfType(entity.getStates(), AbstractContextState.class);
    }

    @Override
    public List<AbstractMultiState> getMultiStates(String descriptorHandle) {
        final MdibEntity entity = entities.get(descriptorHandle);
        if (entity == null || entity.getStates().isEmpty()) {
            return Collections.emptyList();
        }
        return util.exposeListOfType(entity.getStates(), AbstractMultiState.class);
    }

    @Override
    public List<AbstractContextState> getContextStates() {
        return new ArrayList<>(contextStates.values());
    }

    @Override
    public <T extends AbstractContextState> List<T> findContextStatesByType(Class<T> stateClass) {
        var result = new ArrayList<T>();
        contextStates.forEach((handle, state) -> {
            if (stateClass.isAssignableFrom(state.getClass())) {
                result.add(stateClass.cast(state));
            }
        });
        return result;
    }

    @Override
    public WriteDescriptionResult apply(MdibVersion mdibVersion,
                                        @Nullable BigInteger mdDescriptionVersion,
                                        @Nullable BigInteger mdStateVersion,
                                        MdibDescriptionModifications descriptionModifications) {
        this.mdibVersion = mdibVersion;
        Optional.ofNullable(mdDescriptionVersion).ifPresent(version -> this.mdDescriptionVersion = version);
        Optional.ofNullable(mdDescriptionVersion).ifPresent(version -> this.mdStateVersion = version);

        final List<MdibEntity> insertedEntities = new ArrayList<>();
        final List<MdibEntity> updatedEntities = new ArrayList<>();
        final List<MdibEntity> deletedEntities = new ArrayList<>();

        var updatedParentEntitiesDueToInsert = new ArrayList<String>();
        for (var modification : descriptionModifications.getModifications()) {
            var sanitizedStates = removeNotAssociatedContextStates(modification.getStates());
            switch (modification.getModificationType()) {
                case INSERT:
                    insertEntity(modification, sanitizedStates, insertedEntities, updatedParentEntitiesDueToInsert);
                    break;
                case UPDATE:
                    updateEntity(modification, sanitizedStates, updatedEntities);
                    break;
                case DELETE:
                    deleteEntity(modification, deletedEntities, updatedEntities);
                    break;
                default:
                    instanceLogger.warn(
                            "Unknown modification type detected."
                                    + " Skip entry while description modification processing."
                    );
            }
        }

        // Add updated parent entities to updatedEntities in case they are not part of the updatedEntities
        // or insertedEntities list already
        for (var handle : updatedParentEntitiesDueToInsert) {
            if (updatedEntities.stream().noneMatch(mdibEntity -> handle.equals(mdibEntity.getHandle())) &&
                    insertedEntities.stream().noneMatch(mdibEntity -> handle.equals(mdibEntity.getHandle()))) {
                updatedEntities.add(Optional.ofNullable(entities.get(handle)).orElseThrow());
            }
        }

        return new WriteDescriptionResult(mdibVersion, insertedEntities, updatedEntities, deletedEntities);
    }

    private List<AbstractState> removeNotAssociatedContextStates(List<AbstractState> states) {
        var result = new LinkedList<>(states); // copy as the input might be immutable
        if (storeNotAssociatedContextStates) {
            // also return a copy in this case to have the same behavior afterwards
            return result;
        }
        result.removeIf(state ->
                state instanceof AbstractContextState &&
                        ContextAssociation.NO.equals(((AbstractContextState) state).getContextAssociation()));
        return result;
    }

    private void deleteEntity(MdibDescriptionModification modification,
                              List<MdibEntity> deletedEntities,
                              List<MdibEntity> updatedEntities) {
        Optional.ofNullable(entities.get(modification.getHandle())).ifPresent(mdibEntity -> {
            instanceLogger.debug(
                    "[{}] Delete entity: {}",
                    mdibVersion.getInstanceId(), modification.getDescriptor().getHandle()
            );
            mdibEntity.getParent().ifPresent(parentHandle ->
                    Optional.ofNullable(entities.get(parentHandle)).ifPresent(parentEntity -> {
                        var updatedParent = entityFactory.replaceChildren(parentEntity,
                                parentEntity.getChildren().stream()
                                        // filter out the removed entity only
                                        .filter(s -> !s.equals(mdibEntity.getHandle()))
                                        .collect(Collectors.toList()));

                        entities.put(updatedParent.getHandle(), updatedParent);
                        // if the child was found and removed - add parent to the updated entities list
                        if (parentEntity.getChildren().size() != updatedParent.getChildren().size()) {
                            updatedEntities.add(updatedParent);
                        }
                    })
            );
        });

        final MdibEntity deletedEntity = entities.get(modification.getHandle());
        if (deletedEntity == null) {
            instanceLogger.warn(
                    "Possible inconsistency detected. Entity to delete was not found: {}",
                    modification.getHandle()
            );
            return;
        }

        rootEntities.remove(modification.getHandle());
        entities.remove(modification.getHandle());
        contextStates.entrySet().removeIf(state ->
                state.getValue().getDescriptorHandle().equals(modification.getHandle()));

        deletedEntities.add(deletedEntity);
    }

    private void updateEntity(MdibDescriptionModification modification,
                              List<AbstractState> sanitizedStates,
                              List<MdibEntity> updatedEntities
    ) {
        Optional.ofNullable(entities.get(modification.getHandle())).ifPresent(mdibEntity -> {
            instanceLogger.debug("[{}] Update entity: {}", mdibVersion.getInstanceId(), modification.getDescriptor());

            entities.put(mdibEntity.getHandle(), entityFactory.replaceDescriptorAndStates(
                    mdibEntity,
                    modification.getDescriptor(),
                    sanitizedStates));

            updatedEntities.add(entityFactory.replaceDescriptorAndStates(
                    mdibEntity,
                    modification.getDescriptor(),
                    modification.getStates()));

            updateContextStatesMap(modification.getStates());
        });
    }

    private void updateContextStatesMap(List<AbstractState> states) {
        for (AbstractState state : states) {
            var contextState = typeValidator.toContextState(state);
            if (contextState.isEmpty()) {
                continue;
            }

            if (!storeNotAssociatedContextStates && getNotAssociatedContextState(state).isPresent()) {
                contextStates.remove(contextState.get().getHandle());
            } else {
                contextStates.put(contextState.get().getHandle(), contextState.get());
            }
        }
    }

    private String findParentMds(
        List<MdibEntity> insertedEntities,
        String parentHandle,
        String newEntityHandle
    ) {
        // find parent entity in inserted
        var parentEntity = insertedEntities.stream()
            .filter(it -> parentHandle.equals(it.getHandle()))
            .findFirst();

        if (!parentEntity.isPresent()) {
            parentEntity = Optional.ofNullable(this.entities.get(parentHandle));
        }

        var parent = parentEntity.orElseThrow(
            () -> new RuntimeException(String.format(
                "Could not find parent entity for %s, %s was missing", newEntityHandle, parentHandle
            ))
        );

        if (parent.getDescriptor() instanceof MdsDescriptor) {
            return parent.getHandle();
        } else {
            var grandParent = parent.getParent().orElseThrow(
                () -> new RuntimeException(String.format("No mandatory parent found for %s", parentHandle))
            );
            return findParentMds(insertedEntities, grandParent, newEntityHandle);
        }
    }

    private void insertEntity(MdibDescriptionModification modification,
                              List<AbstractState> sanitizedStates,
                              List<MdibEntity> insertedEntities,
                              List<String> updatedEntityHandles) {
        // find parent mds for entity
        String parentMdsHandle;
        if (modification.getDescriptor() instanceof MdsDescriptor) {
            parentMdsHandle = modification.getDescriptor().getHandle();
        } else {
            parentMdsHandle = findParentMds(
                insertedEntities,
                modification.getParentHandle().orElseThrow(
                    () -> new RuntimeException(
                        String.format("Mandatory parent handle missing for %s", modification.getHandle())
                    )
                ),
                modification.getHandle()
            );
        }


        var mdibEntityForStorage = entityFactory.createMdibEntity(
            modification.getParentHandle().orElse(null),
            new ArrayList<>(),
            modification.getDescriptor(),
            sanitizedStates,
            mdibVersion,
            parentMdsHandle
        );

        var mdibEntityForResultSet = entityFactory.createMdibEntity(
            modification.getParentHandle().orElse(null),
            new ArrayList<>(),
            modification.getDescriptor(),
            modification.getStates(),
            mdibVersion,
            parentMdsHandle
        );

        // Either add entity as child of a parent or expect it to be a root entity
        if (modification.getParentHandle().isPresent()) {
            Optional.ofNullable(entities.get(modification.getParentHandle().get())).ifPresent(parentEntity -> {
                var children = new ArrayList<>(parentEntity.getChildren());
                children.add(mdibEntityForStorage.getHandle());

                entities.put(parentEntity.getHandle(),
                        entityFactory.replaceChildren(parentEntity, Collections.unmodifiableList(children)));
                updatedEntityHandles.add(parentEntity.getHandle());
            });
        } else {
            rootEntities.add(mdibEntityForStorage.getHandle());
        }

        // Add to entities list
        entities.put(mdibEntityForStorage.getHandle(), mdibEntityForStorage);

        instanceLogger.debug(
                "[{}] Insert entity: {}",
                mdibVersion.getInstanceId(), mdibEntityForStorage.getDescriptor()
        );

        // Add to context states if context entity
        if (mdibEntityForStorage.getDescriptor() instanceof AbstractContextDescriptor) {
            contextStates.putAll(mdibEntityForStorage.getStates().stream()
                    .map(AbstractContextState.class::cast)
                    .collect(Collectors.toMap(AbstractMultiState::getHandle, state -> state)));
        }

        insertedEntities.add(mdibEntityForResultSet);
    }

    @Override
    public WriteStateResult apply(MdibVersion mdibVersion,
                                  @Nullable BigInteger mdStateVersion,
                                  MdibStateModifications stateModifications) throws RuntimeException {
        this.mdibVersion = mdibVersion;
        Optional.ofNullable(mdStateVersion).ifPresent(version -> this.mdStateVersion = version);

        MdibDescriptionModifications descriptionModifications = null;

        final Map<String, List<AbstractState>> modifiedStates = new HashMap<>();
        for (AbstractState modification : stateModifications.getStates()) {
            if (instanceLogger.isDebugEnabled()) {
                instanceLogger.debug("[{}] Update state: {}", mdibVersion.getSequenceId(), modification);
            }
            final MdibEntity mdibEntity = entities.get(modification.getDescriptorHandle());

            modifiedStates.computeIfAbsent(mdibEntity.getParentMds(), s -> new ArrayList<>()).add(modification);

            if (mdibEntity == null) {
                // Do not store context states when not associated
                if (!storeNotAssociatedContextStates) {
                    var contextState = getNotAssociatedContextState(modification);
                    if (contextState.isPresent()) {
                        instanceLogger.debug(
                                "Found update on context state {} with association=not-associated;"
                                        + " do not store in MDIB",
                                contextState.get().getHandle()
                        );
                        continue;
                    }
                }

                // this will insert states even if no descriptor/MDIB entity exists
                // to be used in remote MDIBS in case
                if (descriptionModifications == null) {
                    instanceLogger.error(
                            "Inserting states without descriptors is forbidden, descriptor handle {}",
                            modification.getDescriptorHandle()
                    );
                    throw new RuntimeException(
                            "Inserting states without descriptors is disabled,"
                                    + " descriptor handle " + modification.getDescriptorHandle()
                    );
                }
                AbstractDescriptor descr;
                try {
                    descr = typeValidator.resolveDescriptorType(modification.getClass())
                            .getConstructor().newInstance();
                } catch (ClassNotFoundException | InvocationTargetException | SecurityException
                        | NoSuchMethodException | InstantiationException | IllegalArgumentException
                        | IllegalAccessException e
                ) {
                    instanceLogger.warn(
                            "Ignore modification. Reason: could not instantiate descriptor type for handle {}.",
                            modification.getDescriptorHandle()
                    );
                    instanceLogger.trace("Ignore modification", e);
                    continue;
                }
                descr.setHandle(modification.getDescriptorHandle());
                descr.setDescriptorVersion(BigInteger.valueOf(-1));
                descriptionModifications.insert(descr, modification);
            } else {
                mdibEntity
                        .doIfSingleState(state -> {
                            entities.put(mdibEntity.getHandle(),
                                    entityFactory.replaceStates(mdibEntity, Collections.singletonList(modification)));
                        })
                        .orElse(states -> {
                            var modificationAsMultiState = typeValidator.toMultiState(modification).orElseThrow(() ->
                                    new RuntimeException(String.format(
                                            "Found a non-matching multi-state for multi-state entity update"
                                                    + " (descriptor handle: %s)",
                                            mdibEntity.getHandle())
                                    ));

                            var newStates = new ArrayList<AbstractMultiState>();
                            boolean found = false;
                            for (AbstractMultiState multiState : states) {
                                if (multiState.getHandle().equals(modificationAsMultiState.getHandle())) {
                                    found = true;
                                    if (!storeNotAssociatedContextStates
                                            && getNotAssociatedContextState(modificationAsMultiState).isPresent()) {
                                        instanceLogger.debug(
                                                "Found context state {} with association=not-associated;"
                                                        + " refuse storage in MDIB",
                                                modificationAsMultiState.getHandle()
                                        );
                                        contextStates.remove(multiState.getHandle());
                                    } else {
                                        instanceLogger.debug(
                                                "Replacing already present multi-state {}",
                                                multiState.getHandle()
                                        );
                                        newStates.add(modificationAsMultiState);
                                    }
                                } else {
                                    newStates.add(multiState);
                                }
                            }

                            if (!found && (storeNotAssociatedContextStates
                                    || getNotAssociatedContextState(modificationAsMultiState).isEmpty())) {
                                instanceLogger.debug("Adding new MultiState {}", modificationAsMultiState.getHandle());
                                newStates.add(modificationAsMultiState);
                            }

                            entities.put(mdibEntity.getHandle(), entityFactory.replaceStates(mdibEntity,
                                    Collections.unmodifiableList(newStates)));
                            newStates.stream()
                                    .filter(AbstractContextState.class::isInstance)
                                    .map(AbstractContextState.class::cast)
                                    .collect(Collectors.toList())
                                    .forEach(abstractContextState ->
                                            contextStates.put(abstractContextState.getHandle(), abstractContextState));
                        });
            }
        }

        // Only relevant to remote MDIBs where entities need to be present even if only states are subscribed
        // (an unlikely use-case, but should be supported nevertheless)
        if (descriptionModifications != null) {
            apply(mdibVersion, null, null, descriptionModifications);
        }

        return new WriteStateResult(mdibVersion, modifiedStates);
    }

    @Override
    public MdibVersion getMdibVersion() {
        return mdibVersion;
    }

    @Override
    public BigInteger getMdDescriptionVersion() {
        return mdDescriptionVersion;
    }

    @Override
    public BigInteger getMdStateVersion() {
        return mdStateVersion;
    }

    private Optional<AbstractContextState> getNotAssociatedContextState(AbstractState state) {
        if (state instanceof AbstractContextState) {
            var contextState = (AbstractContextState) state;
            if (contextState.getContextAssociation() == null
                    || ContextAssociation.NO.equals(contextState.getContextAssociation())) {
                return Optional.of(contextState);
            }
        }
        return Optional.empty();
    }
}
