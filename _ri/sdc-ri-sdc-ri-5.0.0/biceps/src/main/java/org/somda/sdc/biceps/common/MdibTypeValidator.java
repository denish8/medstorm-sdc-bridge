package org.somda.sdc.biceps.common;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.somda.sdc.biceps.model.participant.AbstractContextDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractMultiState;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.ActivateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.ActivateOperationState;
import org.somda.sdc.biceps.model.participant.AlertConditionDescriptor;
import org.somda.sdc.biceps.model.participant.AlertConditionState;
import org.somda.sdc.biceps.model.participant.AlertSignalDescriptor;
import org.somda.sdc.biceps.model.participant.AlertSignalState;
import org.somda.sdc.biceps.model.participant.AlertSystemDescriptor;
import org.somda.sdc.biceps.model.participant.AlertSystemState;
import org.somda.sdc.biceps.model.participant.BatteryDescriptor;
import org.somda.sdc.biceps.model.participant.BatteryState;
import org.somda.sdc.biceps.model.participant.ChannelDescriptor;
import org.somda.sdc.biceps.model.participant.ChannelState;
import org.somda.sdc.biceps.model.participant.ClockDescriptor;
import org.somda.sdc.biceps.model.participant.ClockState;
import org.somda.sdc.biceps.model.participant.DistributionSampleArrayMetricDescriptor;
import org.somda.sdc.biceps.model.participant.DistributionSampleArrayMetricState;
import org.somda.sdc.biceps.model.participant.EnsembleContextDescriptor;
import org.somda.sdc.biceps.model.participant.EnsembleContextState;
import org.somda.sdc.biceps.model.participant.EnumStringMetricDescriptor;
import org.somda.sdc.biceps.model.participant.EnumStringMetricState;
import org.somda.sdc.biceps.model.participant.LimitAlertConditionDescriptor;
import org.somda.sdc.biceps.model.participant.LimitAlertConditionState;
import org.somda.sdc.biceps.model.participant.LocationContextDescriptor;
import org.somda.sdc.biceps.model.participant.LocationContextState;
import org.somda.sdc.biceps.model.participant.MdsDescriptor;
import org.somda.sdc.biceps.model.participant.MdsState;
import org.somda.sdc.biceps.model.participant.MeansContextDescriptor;
import org.somda.sdc.biceps.model.participant.MeansContextState;
import org.somda.sdc.biceps.model.participant.NumericMetricDescriptor;
import org.somda.sdc.biceps.model.participant.NumericMetricState;
import org.somda.sdc.biceps.model.participant.OperatorContextDescriptor;
import org.somda.sdc.biceps.model.participant.OperatorContextState;
import org.somda.sdc.biceps.model.participant.PatientContextDescriptor;
import org.somda.sdc.biceps.model.participant.PatientContextState;
import org.somda.sdc.biceps.model.participant.RealTimeSampleArrayMetricDescriptor;
import org.somda.sdc.biceps.model.participant.RealTimeSampleArrayMetricState;
import org.somda.sdc.biceps.model.participant.ScoDescriptor;
import org.somda.sdc.biceps.model.participant.ScoState;
import org.somda.sdc.biceps.model.participant.SetAlertStateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.SetAlertStateOperationState;
import org.somda.sdc.biceps.model.participant.SetComponentStateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.SetComponentStateOperationState;
import org.somda.sdc.biceps.model.participant.SetContextStateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.SetContextStateOperationState;
import org.somda.sdc.biceps.model.participant.SetMetricStateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.SetMetricStateOperationState;
import org.somda.sdc.biceps.model.participant.SetStringOperationDescriptor;
import org.somda.sdc.biceps.model.participant.SetStringOperationState;
import org.somda.sdc.biceps.model.participant.SetValueOperationDescriptor;
import org.somda.sdc.biceps.model.participant.SetValueOperationState;
import org.somda.sdc.biceps.model.participant.StringMetricDescriptor;
import org.somda.sdc.biceps.model.participant.StringMetricState;
import org.somda.sdc.biceps.model.participant.SystemContextDescriptor;
import org.somda.sdc.biceps.model.participant.SystemContextState;
import org.somda.sdc.biceps.model.participant.VmdDescriptor;
import org.somda.sdc.biceps.model.participant.VmdState;
import org.somda.sdc.biceps.model.participant.WorkflowContextDescriptor;
import org.somda.sdc.biceps.model.participant.WorkflowContextState;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Utility class to validate MDIB instances.
 */
public class MdibTypeValidator {
    private static final String ABSTRACT_PREFIX = "Abstract";

    private static final String DESCRIPTOR_SUFFIX = "Descriptor";
    private static final int DESCRIPTOR_SUFFIX_LENGTH = DESCRIPTOR_SUFFIX.length();

    private static final String STATE_SUFFIX = "State";
    private static final int STATE_SUFFIX_LENGTH = STATE_SUFFIX.length();

    private static final BiMap<Class<? extends AbstractDescriptor>, Class<? extends AbstractState>>
            DESCRIPTOR_CLASS_TO_STATE_CLASS = Maps.synchronizedBiMap(HashBiMap.create(
                    ImmutableMap.<Class<? extends AbstractDescriptor>, Class<? extends AbstractState>>builder()
                            .put(ActivateOperationDescriptor.class, ActivateOperationState.class)
                            .put(AlertConditionDescriptor.class, AlertConditionState.class)
                            .put(AlertSignalDescriptor.class, AlertSignalState.class)
                            .put(AlertSystemDescriptor.class, AlertSystemState.class)
                            .put(BatteryDescriptor.class, BatteryState.class)
                            .put(ChannelDescriptor.class, ChannelState.class)
                            .put(ClockDescriptor.class, ClockState.class)
                            .put(DistributionSampleArrayMetricDescriptor.class,
                                 DistributionSampleArrayMetricState.class)
                            .put(EnsembleContextDescriptor.class, EnsembleContextState.class)
                            .put(EnumStringMetricDescriptor.class, EnumStringMetricState.class)
                            .put(LimitAlertConditionDescriptor.class, LimitAlertConditionState.class)
                            .put(LocationContextDescriptor.class, LocationContextState.class)
                            .put(MdsDescriptor.class, MdsState.class)
                            .put(MeansContextDescriptor.class, MeansContextState.class)
                            .put(NumericMetricDescriptor.class, NumericMetricState.class)
                            .put(OperatorContextDescriptor.class, OperatorContextState.class)
                            .put(PatientContextDescriptor.class, PatientContextState.class)
                            .put(RealTimeSampleArrayMetricDescriptor.class, RealTimeSampleArrayMetricState.class)
                            .put(ScoDescriptor.class, ScoState.class)
                            .put(SetAlertStateOperationDescriptor.class, SetAlertStateOperationState.class)
                            .put(SetComponentStateOperationDescriptor.class, SetComponentStateOperationState.class)
                            .put(SetContextStateOperationDescriptor.class, SetContextStateOperationState.class)
                            .put(SetMetricStateOperationDescriptor.class, SetMetricStateOperationState.class)
                            .put(SetStringOperationDescriptor.class, SetStringOperationState.class)
                            .put(SetValueOperationDescriptor.class, SetValueOperationState.class)
                            .put(StringMetricDescriptor.class, StringMetricState.class)
                            .put(SystemContextDescriptor.class, SystemContextState.class)
                            .put(VmdDescriptor.class, VmdState.class)
                            .put(WorkflowContextDescriptor.class, WorkflowContextState.class).build()
            ));
    private static final BiMap<Class<? extends AbstractState>, Class<? extends AbstractDescriptor>>
            STATE_CLASS_TO_DESCRIPTOR_CLASS = Maps.synchronizedBiMap(DESCRIPTOR_CLASS_TO_STATE_CLASS.inverse());

    @Inject
    MdibTypeValidator() {
    }

    /**
     * Gets the base name of a descriptor class.
     * <p>
     * The base name is the name of the class without <em>Descriptor</em> suffix.
     *
     * @param descrClass the class where to resolve the base name from.
     * @return the base name.
     */
    public String resolveDescriptorBaseName(Class<? extends AbstractDescriptor> descrClass) {
        return descrClass.getSimpleName().substring(0, descrClass.getSimpleName().length() - DESCRIPTOR_SUFFIX_LENGTH);
    }

    /**
     * Gets the base name of a state class.
     * <p>
     * The base name is the name of the class without <em>State</em> suffix.
     *
     * @param stateClass the class where to resolve the base name from.
     * @return the base name.
     */
    public String resolveStateBaseName(Class<? extends AbstractState> stateClass) {
        return stateClass.getSimpleName().substring(0, stateClass.getSimpleName().length() - STATE_SUFFIX_LENGTH);
    }

    /**
     * Checks if descriptor and state classes match.
     * <p>
     * A match is given if both classes do not implement the abstract flavor and share the same name prefix
     * excluding the Descriptor and State suffix.
     *
     * @param descrClass the descriptor class to match.
     * @param stateClass the state class to match.
     * @return true if classes match, otherwise false.
     */
    public boolean match(Class<? extends AbstractDescriptor> descrClass,
                         Class<? extends AbstractState> stateClass) {

        if (descrClass.getSimpleName().startsWith(ABSTRACT_PREFIX) ||
                stateClass.getSimpleName().startsWith(ABSTRACT_PREFIX)) {
            return false;
        }

        final String name1 = resolveDescriptorBaseName(descrClass);
        final String name2 = resolveStateBaseName(stateClass);
        return name1.equals(name2);
    }

    /**
     * Checks if the given descriptor states pairing is valid.
     * <p>
     * A match is given if
     * <ul>
     *   <li>descriptor and states match in terms of {@link #match(Class, Class)},
     *   <li>{@link AbstractState#getDescriptorHandle()} equals {@link AbstractDescriptor#getHandle()}, and
     *   <li>single state descriptors do correspond to exactly one state.
     * </ul>
     *
     * @param descriptor the descriptor to test.
     * @param states     the list of states to test.
     * @param <D>        any descriptor class.
     * @param <S>        any state class.
     * @return true if instances match, otherwise false.
     */
    public <D extends AbstractDescriptor, S extends AbstractState> boolean match(D descriptor, List<S> states) {
        boolean singleStateOk = isSingleStateDescriptor(descriptor) && states.size() == 1;
        boolean multiStateOk = isMultiStateDescriptor(descriptor);
        boolean typesAndHandleRefsOk = states.stream().noneMatch(s ->
                !descriptor.getHandle().equals(s.getDescriptorHandle()) ||
                        !match(descriptor.getClass(), s.getClass()));
        return typesAndHandleRefsOk && (singleStateOk || multiStateOk);
    }

    /**
     * Tries to match a descriptor with exactly one state.
     * <p>
     * Hint: does also work for multi-state lists of size 1.
     *
     * @param descriptor the descriptor to test.
     * @param state      the state to test.
     * @param <D>        any descriptor class.
     * @param <S>        any state class.
     * @return true if descriptor and state match, otherwise false.
     * @see #match(AbstractDescriptor, List)
     */
    public <D extends AbstractDescriptor, S extends AbstractState> boolean match(D descriptor, S state) {
        return match(descriptor, Collections.singletonList(state));
    }

    /**
     * Checks if a descriptor is a single state descriptor (true) or not (false).
     *
     * @param descriptor the descriptor to test.
     * @param <T>        any descriptor class.
     * @return true if the descriptor is a single state descriptor, false otherwise.
     */
    public <T extends AbstractDescriptor> boolean isSingleStateDescriptor(T descriptor) {
        return !isMultiStateDescriptor(descriptor);
    }

    /**
     * Checks if a state is a single state (true) or not (false).
     *
     * @param state the state to test.
     * @param <T>   any state class.
     * @return true if the state is a single state, false otherwise.
     */
    public <T extends AbstractState> boolean isSingleState(T state) {
        return !isMultiState(state);
    }

    /**
     * Checks if a descriptor is a multi-state descriptor (true) or not (false).
     *
     * @param descriptor the descriptor to test.
     * @param <T>        any descriptor class.
     * @return true if the descriptor is a multi-state descriptor, false otherwise.
     */
    public <T extends AbstractDescriptor> boolean isMultiStateDescriptor(T descriptor) {
        return descriptor instanceof AbstractContextDescriptor;
    }

    /**
     * Checks if a state is a multi-state (true) or not (false).
     *
     * @param state the state to test.
     * @param <T>   any state class.
     * @return true if the state is a multi-state, false otherwise.
     */
    public <T extends AbstractState> boolean isMultiState(T state) {
        return state instanceof AbstractMultiState;
    }

    /**
     * Checks if a state is a context state (true) or not (false).
     *
     * @param state the state to test.
     * @param <T>   any state class.
     * @return true if the state is a context state, false otherwise.
     */
    public <T extends AbstractState> boolean isContextState(T state) {
        return state instanceof AbstractContextState;
    }

    /**
     * Tries to cast to a multi-state.
     *
     * @param state the state to cast.
     * @param <T>   any state class.
     * @return The cast multi-state or {@linkplain Optional#empty()} if the state was not a multi-state.
     */
    public <T extends AbstractState> Optional<AbstractMultiState> toMultiState(T state) {
        if (isMultiState(state)) {
            return Optional.of((AbstractMultiState) state);
        }
        return Optional.empty();
    }

    /**
     * Tries to cast to a context state.
     *
     * @param state the state to cast.
     * @param <T>   any state class.
     * @return The cast multi-state or {@linkplain Optional#empty()} if the state was not a multi-state.
     */
    public <T extends AbstractState> Optional<AbstractContextState> toContextState(T state) {
        if (isContextState(state)) {
            return Optional.of((AbstractContextState) state);
        }
        return Optional.empty();
    }

    /**
     * Resolves the descriptor type belonging to a state type.
     * @param stateClass to resolve the descriptor type for
     * @param <T> a state type
     * @param <V> a descriptor type
     * @return the descriptor type matching the passed state
     * @throws ClassNotFoundException if no matching descriptor class has been found
     */
    public <T extends AbstractState, V extends AbstractDescriptor> Class<V> resolveDescriptorType(Class<T> stateClass)
            throws ClassNotFoundException {
        var descriptorClass = (Class<V>) STATE_CLASS_TO_DESCRIPTOR_CLASS.get(stateClass);
        if (descriptorClass == null) {
            // this should never happen, but here is a reflection based fallback anyway
            final String baseName = stateClass.getCanonicalName()
                    .substring(0, stateClass.getCanonicalName().length() - STATE_SUFFIX_LENGTH);
            descriptorClass = (Class<V>) Class.forName(baseName + DESCRIPTOR_SUFFIX);
            STATE_CLASS_TO_DESCRIPTOR_CLASS.put(stateClass, descriptorClass);
        }
        return descriptorClass;
    }

    /**
     * Resolves the state type belonging to a descriptor type.
     * @param descriptorClass to resolve the state type for
     * @param <T> a descriptor type
     * @param <V> a state type
     * @return the state type matching the passed descriptor
     * @throws ClassNotFoundException if no matching state class has been found
     */
    public <T extends AbstractDescriptor, V extends AbstractState> Class<V> resolveStateType(Class<T> descriptorClass)
            throws ClassNotFoundException {
        // as this method is called very often in normal operation, a lookup table is used instead of reflection
        var stateClass = (Class<V>) DESCRIPTOR_CLASS_TO_STATE_CLASS.get(descriptorClass);
        if (stateClass == null) {
            // this should never happen, but here is a reflection based fallback anyway
            final String baseName = descriptorClass.getCanonicalName()
                    .substring(0, descriptorClass.getCanonicalName().length() - DESCRIPTOR_SUFFIX_LENGTH);
            stateClass =  (Class<V>) Class.forName(baseName + STATE_SUFFIX);
            DESCRIPTOR_CLASS_TO_STATE_CLASS.put(descriptorClass, stateClass);
        }
        return stateClass;
    }
}
