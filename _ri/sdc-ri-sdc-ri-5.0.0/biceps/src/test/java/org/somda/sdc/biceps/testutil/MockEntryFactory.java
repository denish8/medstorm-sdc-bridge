package org.somda.sdc.biceps.testutil;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibTypeValidator;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractMultiState;
import org.somda.sdc.biceps.model.participant.AbstractState;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MockEntryFactory {
    private final MdibTypeValidator typeValidator;

    public MockEntryFactory(MdibTypeValidator typeValidator) {
        this.typeValidator = typeValidator;
    }

    public <T extends AbstractDescriptor, V extends AbstractState> MdibDescriptionModifications.Entry entry(String handle,
                                                                                                            Class<T> descrClass) throws Exception {
        return entry(handle, descrClass, null);
    }

    public <T extends AbstractDescriptor, V extends AbstractState> MdibDescriptionModifications.Entry entry(String handle,
                                                                                                            Class<T> descrClass,
                                                                                                            @Nullable String parentHandle) throws Exception {
        Class<V> stateClass = typeValidator.resolveStateType(descrClass);
        return new MdibDescriptionModifications.Entry(
                descriptor(handle, descrClass),
                state(handle, stateClass),
                parentHandle);
    }

    public <T extends AbstractDescriptor, V extends AbstractState> MdibDescriptionModifications.Entry entry(String handle,
                                                                                                            Class<T> descrClass,
                                                                                                            Consumer<T> descrCustomizer,
                                                                                                            Consumer<V> stateCustomizer,
                                                                                                            @Nullable String parentHandle) throws Exception {
        Class<V> stateClass = typeValidator.resolveStateType(descrClass);
        return new MdibDescriptionModifications.Entry(
                descriptor(handle, descrClass, descrCustomizer),
                state(handle, stateClass, stateCustomizer),
                parentHandle);
    }

    public <T extends AbstractDescriptor, V extends AbstractContextState> MdibDescriptionModifications.MultiStateEntry contextEntry(String handle,
                                                                                                                                    String stateHandle,
                                                                                                                                    Class<T> descrClass,
                                                                                                                                    String parentHandle) throws Exception {
        Class<V> stateClass = typeValidator.resolveStateType(descrClass);
        return new MdibDescriptionModifications.MultiStateEntry(
                descriptor(handle, descrClass),
                Collections.singletonList(MockModelFactory.createContextState(stateHandle, handle, stateClass)),
                parentHandle);
    }

    public <T extends AbstractDescriptor, V extends AbstractContextState> MdibDescriptionModifications.MultiStateEntry contextEntry(String handle,
                                                                                                                                    List<String> stateHandles,
                                                                                                                                    Class<T> descrClass,
                                                                                                                                    String parentHandle) throws Exception {
        Class<V> stateClass = typeValidator.resolveStateType(descrClass);
        final List<V> states = stateHandles.stream()
                .map(s -> MockModelFactory.createContextState(s, handle, stateClass))
                .collect(Collectors.toList());
        return new MdibDescriptionModifications.MultiStateEntry(
                descriptor(handle, descrClass),
                states,
                parentHandle);
    }

    public <T extends AbstractDescriptor> T descriptor(String handle, Class<T> theClass) {
        return descriptor(handle, theClass, t -> {});
    }

    public <T extends AbstractDescriptor> T descriptor(String handle, Class<T> theClass, Consumer<T> customizer) {
        var descr = MockModelFactory.createDescriptor(handle, theClass);
        customizer.accept(descr);
        return descr;
    }

    public <T extends AbstractState> T state(String handle, Class<T> theClass) {
        return state(handle, theClass, t -> {});
    }

    public <T extends AbstractState> T state(String handle, Class<T> theClass, Consumer<T> customizer) {
        var state = MockModelFactory.createState(handle, theClass);
        customizer.accept(state);
        return state;
    }

    public <T extends AbstractContextState> T state(String handle, String descrHandle, Class<T> theClass) {
        return MockModelFactory.createContextState(handle, descrHandle, theClass);
    }
}
