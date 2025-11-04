package org.somda.sdc.biceps.common;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.biceps.common.access.CopyManager;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractMultiState;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.MdibVersion;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Default implementation of {@link MdibEntity}.
 */
public class MdibEntityImpl implements MdibEntity {
    private final String parent;
    private final List<String> children;
    private final AbstractDescriptor descriptor;
    private final List<AbstractState> states;
    private final MdibVersion mdibVersion;
    private final CopyManager copyManager;
    private final Class<? extends AbstractState> stateClass;
    private final String parentMds;

    @AssistedInject
    MdibEntityImpl(@Assisted @Nullable String parent,
                   @Assisted("children") List<String> children,
                   @Assisted AbstractDescriptor descriptor,
                   @Assisted("states") List<AbstractState> states,
                   @Assisted MdibVersion mdibVersion,
                   @Assisted("parentMds") String parentMds,
                   CopyManager copyManager,
                   MdibTypeValidator typeValidator) {
        this.parent = parent;
        this.children = children;
        this.descriptor = descriptor;
        this.states = states;
        this.mdibVersion = mdibVersion;
        this.copyManager = copyManager;
        this.parentMds = parentMds;

        try {
            this.stateClass = typeValidator.resolveStateType(descriptor.getClass());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(String.format(
                    "Unexpected descriptor class with no matching state class found: %s",
                    descriptor.getClass()
            ));
        }
    }

    @Override
    public MdibVersion getLastChanged() {
        return mdibVersion;
    }

    @Override
    public List<String> getChildren() {
        return children;
    }

    @Override
    public AbstractDescriptor getDescriptor() {
        return copyManager.processOutput(descriptor);
    }

    @Override
    public <T extends AbstractDescriptor> Optional<T> getDescriptor(Class<T> theClass) {
        return theClass.isAssignableFrom(descriptor.getClass())
                ? Optional.of(theClass.cast(getDescriptor())) : Optional.empty();
    }

    @Override
    public String getHandle() {
        return descriptor.getHandle();
    }

    @Override
    public Optional<String> getParent() {
        return Optional.ofNullable(parent);
    }

    @Override
    public List<AbstractState> getStates() {
        return copyManager.processOutput(states);
    }

    @Override
    public <T extends AbstractState> List<T> getStates(Class<T> theClass) {
        if (!states.isEmpty() && theClass.isAssignableFrom(states.get(0).getClass())) {
            return (List<T>) copyManager.processOutput(states);
        }

        return new ArrayList<>();
    }

    @Override
    public <T extends AbstractState> Optional<T> getFirstState(Class<T> theClass) {
        if (!states.isEmpty() && theClass.isAssignableFrom(states.get(0).getClass())) {
            return Optional.of(theClass.cast(copyManager.processOutput(states.get(0))));
        }
        return Optional.empty();
    }

    @Override
    public StateAlternative<List<AbstractMultiState>> doIfSingleState(Consumer<AbstractState> consumer) {
        if (!getStates().isEmpty()) {
            // if AbstractMultiState is a superclass of the state, it's a multi-state
            if (!AbstractMultiState.class.isAssignableFrom(getStates().get(0).getClass())) {
                consumer.accept(getStates().get(0));
                return stateAlternativeConsumer -> {
                };
            }
        }

        return stateAlternativeConsumer -> stateAlternativeConsumer.accept((List) getStates());
    }

    @Override
    public StateAlternative<AbstractState> doIfMultiState(Consumer<List<AbstractMultiState>> consumer) {
        if (getStates().isEmpty()) {
            consumer.accept(Collections.emptyList());
            return stateAlternativeConsumer -> {
            };
        } else {
            if (AbstractMultiState.class.isAssignableFrom(getStates().get(0).getClass())) {
                consumer.accept((List) getStates());
                return stateAlternativeConsumer -> {
                };
            }
        }
        return stateAlternativeConsumer -> stateAlternativeConsumer.accept(getStates().get(0));
    }

    @Override
    public Class<? extends AbstractDescriptor> getDescriptorClass() {
        return descriptor.getClass();
    }

    @Override
    public String getParentMds() {
        return parentMds;
    }

    @Override
    public Class<? extends AbstractState> getStateClass() {
        return stateClass;
    }
}
