package org.somda.sdc.biceps.common;

import com.kscs.util.jaxb.Copyable;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Single change item.
 */
public class MdibDescriptionModification implements Copyable<MdibDescriptionModification>  {

    private final Type modificationType;
    private final AbstractDescriptor descriptor;
    private final List<AbstractState> states;
    private final String parentHandle;

    public MdibDescriptionModification(Type modificationType,
                                       AbstractDescriptor descriptor,
                                       @Nullable List<AbstractState> states,
                                       @Nullable String parentHandle) {
        this.modificationType = modificationType;
        this.descriptor = descriptor;
        this.states = states == null ? new ArrayList<>() : new ArrayList<>(states);
        this.parentHandle = parentHandle;
    }

    public MdibDescriptionModification(Type modificationType,
                                       AbstractDescriptor descriptor,
                                       @Nullable List<AbstractState> states) {
        this(modificationType, descriptor, states, null);
    }

    public Type getModificationType() {
        return modificationType;
    }

    public AbstractDescriptor getDescriptor() {
        return descriptor;
    }

    public String getHandle() {
        return descriptor.getHandle();
    }

    public List<AbstractState> getStates() {
        return states;
    }

    public Optional<String> getParentHandle() {
        return Optional.ofNullable(parentHandle);
    }

    @Override
    public MdibDescriptionModification createCopy() {
        return new MdibDescriptionModification(
            this.modificationType,
            this.descriptor.createCopy(),
            this.states.stream().map(AbstractState::createCopy).collect(Collectors.toList()),
            parentHandle
        );
    }

    /**
     * Designates the MDIB description modification type, i.e., insert, update, or delete.
     */
    public enum Type {
        INSERT,
        UPDATE,
        DELETE
    }
}
