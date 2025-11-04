package org.somda.sdc.biceps.common;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.testutil.MockModelFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.org.somda.common.LoggingTestWatcher;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(LoggingTestWatcher.class)
class MdibTypeValidatorTest {
    private MdibTypeValidator matcher;

    private static final Map<Class<? extends AbstractDescriptor>, Class<? extends AbstractState>> singleStateMap
            // Map.of is only defined up to 10 elements, so we need to use the builder
            = ImmutableMap.<Class<? extends AbstractDescriptor>, Class<? extends AbstractState>>builder()
            .put(ActivateOperationDescriptor.class, ActivateOperationState.class)
            .put(AlertConditionDescriptor.class, AlertConditionState.class)
            .put(AlertSignalDescriptor.class, AlertSignalState.class)
            .put(AlertSystemDescriptor.class, AlertSystemState.class)
            .put(BatteryDescriptor.class, BatteryState.class)
            .put(ClockDescriptor.class, ClockState.class)
            .put(DistributionSampleArrayMetricDescriptor.class, DistributionSampleArrayMetricState.class)
            .put(EnumStringMetricDescriptor.class, EnumStringMetricState.class)
            .put(LimitAlertConditionDescriptor.class, LimitAlertConditionState.class)
            .put(MdsDescriptor.class, MdsState.class)
            .put(NumericMetricDescriptor.class, NumericMetricState.class)
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
            .build();

    private static final Map<Class<? extends AbstractDescriptor>, Class<? extends AbstractMultiState>> multiStateMap = Map.of(
            EnsembleContextDescriptor.class, EnsembleContextState.class,
            LocationContextDescriptor.class, LocationContextState.class,
            MeansContextDescriptor.class, MeansContextState.class,
            OperatorContextDescriptor.class, OperatorContextState.class,
            PatientContextDescriptor.class, PatientContextState.class,
            WorkflowContextDescriptor.class, WorkflowContextState.class
    );

    @BeforeEach
    public void setUp() {
        matcher = new MdibTypeValidator();
    }

    @Test
    void matchingClassTypes() {

        singleStateMap.forEach(
                (descOuter, stateOuter) -> {
                    singleStateMap.forEach(
                            (descInner, stateInner) -> {
                                if (descInner == descOuter) {
                                    assertTrue(matcher.match(descOuter, stateInner));
                                } else {
                                    assertFalse(matcher.match(descOuter, stateInner));
                                }
                            }
                    );
                    multiStateMap.forEach(
                            (descInner, stateInner) -> {
                                assertFalse(matcher.match(descOuter, stateInner));
                            }
                    );
                }
        );

        multiStateMap.forEach(
                (descOuter, stateOuter) -> {
                    multiStateMap.forEach(
                            (descInner, stateInner) -> {
                                if (descInner == descOuter) {
                                    assertTrue(matcher.match(descOuter, stateInner));
                                } else {
                                    assertFalse(matcher.match(descOuter, stateInner));
                                }
                            }
                    );
                    singleStateMap.forEach(
                            (descInner, stateInner) -> {
                                assertFalse(matcher.match(descOuter, stateInner));
                            }
                    );
                }
        );

    }

    @Test
    <T extends AbstractDescriptor, U extends AbstractState> void matchSingleStateInstance() {

        singleStateMap.forEach(
                (descOuter, stateOuter) -> {
                    Class<T> descOuterCast = (Class<T>) descOuter; // to force one warning up here
                    Class<U> stateOuterCast = (Class<U>) stateOuter;
                    T descriptor = MockModelFactory.createDescriptor("handle", descOuterCast);
                    U state = MockModelFactory.createState("handle", stateOuterCast);
                    U illegalSecondState = MockModelFactory.createState("handle", stateOuterCast);

                    U mismatchingHandleState = MockModelFactory.createState("invalid-handle", stateOuterCast);

                    assertTrue(matcher.match(descriptor, state));
                    assertTrue(matcher.match(descriptor, Collections.singletonList(state)));

                    assertFalse(matcher.match(descriptor, mismatchingHandleState));
                    assertFalse(matcher.match(descriptor, Collections.singletonList(mismatchingHandleState)));
                    assertFalse(matcher.match(descriptor, Arrays.asList(state, illegalSecondState)));
                    assertFalse(matcher.match(descriptor, Collections.emptyList()));

                    singleStateMap.forEach(
                            (descInner, stateInner) -> {
                                if (descOuter == descInner) return; // don't compare with the same class
                                Class<U> stateInnerCast = (Class<U>) stateInner;
                                U illegalStateType = MockModelFactory.createState("handle", stateInnerCast);

                                assertFalse(matcher.match(descriptor, illegalStateType));
                                assertFalse(matcher.match(descriptor, Collections.singletonList(illegalStateType)));
                            }
                    );

                }
        );

    }

    @Test
    <T extends AbstractDescriptor, U extends AbstractState, V extends AbstractContextState> void matchingMultiStateInstance() {
        multiStateMap.forEach(
                (descOuter, stateOuter) -> {
                    Class<T> descOuterCast = (Class<T>) descOuter; // to force one warning up here
                    Class<V> stateOuterCast = (Class<V>) stateOuter;

                    T descriptor = MockModelFactory.createDescriptor("handle", descOuterCast);
                    List<V> states = Arrays.asList(
                            MockModelFactory.createContextState("c1", "handle", stateOuterCast),
                            MockModelFactory.createContextState("c2", "handle", stateOuterCast),
                            MockModelFactory.createContextState("c3", "handle", stateOuterCast)
                    );

                    V mismatchingHandleState = MockModelFactory.createContextState("c4", "invalid-handle", stateOuterCast);
                    final List<V> mismatchingHandleStates = new ArrayList<>(states);
                    mismatchingHandleStates.add(mismatchingHandleState);

                    assertTrue(matcher.match(descriptor, states.get(0)));
                    assertTrue(matcher.match(descriptor, states));
                    assertTrue(matcher.match(descriptor, Collections.emptyList()));

                    assertFalse(matcher.match(descriptor, mismatchingHandleStates));

                    multiStateMap.forEach(
                            (descInner, stateInner) -> {
                                if (descOuter == descInner) return; // don't compare with the same class
                                Class<V> stateInnerCast = (Class<V>) stateInner;

                                V mismatchingTypeState = MockModelFactory.createContextState("e1", "handle", stateInnerCast);

                                final List<AbstractState> mismatchingTypeStates = Arrays.asList(
                                        states.get(0),
                                        mismatchingTypeState,
                                        states.get(1));
                                assertFalse(matcher.match(descriptor, mismatchingTypeStates));
                            }
                    );

                    singleStateMap.forEach(
                            (descInner, stateInner) -> {
                                if (descOuter == descInner) return; // don't compare with the same class
                                Class<U> stateInnerCast = (Class<U>) stateInner;
                                U illegalStateType = MockModelFactory.createState("handle", stateInnerCast);

                                assertFalse(matcher.match(descriptor, illegalStateType));
                                final List<AbstractState> mismatchingTypeStates = Arrays.asList(
                                        states.get(0),
                                        illegalStateType,
                                        states.get(1));
                                assertFalse(matcher.match(descriptor, Collections.singletonList(illegalStateType)));
                            }
                    );

                }
        );
    }

    @Test
    <T extends AbstractDescriptor, U extends AbstractState, V extends AbstractContextState> void singleAndMultiStateMatchers() {
        singleStateMap.forEach(
                (descOuter, stateOuter) -> {
                    Class<T> descOuterCast = (Class<T>) descOuter; // to force one warning up here
                    Class<U> stateOuterCast = (Class<U>) stateOuter;

                    T descriptor = MockModelFactory.createDescriptor("handle", descOuterCast);
                    U state = MockModelFactory.createState("handle", stateOuterCast);

                    assertTrue(matcher.isSingleStateDescriptor(descriptor));
                    assertTrue(matcher.isSingleState(state));
                }
        );

        multiStateMap.forEach(
                (descOuter, stateOuter) -> {
                    Class<T> descOuterCast = (Class<T>) descOuter; // to force one warning up here
                    Class<V> stateOuterCast = (Class<V>) stateOuter;

                    T descriptor = MockModelFactory.createDescriptor("handle", descOuterCast);
                    V state = MockModelFactory.createContextState("c", "handle", stateOuterCast);

                    assertTrue(matcher.isMultiStateDescriptor(descriptor));
                    assertTrue(matcher.isMultiState(state));
                }
        );
    }

}
