package org.somda.sdc.glue.common;

import org.somda.sdc.biceps.model.participant.AbstractAlertState;
import org.somda.sdc.biceps.model.participant.AbstractOperationState;
import org.somda.sdc.biceps.model.participant.AlertActivation;
import org.somda.sdc.biceps.model.participant.AlertConditionMonitoredLimits;
import org.somda.sdc.biceps.model.participant.ClockState;
import org.somda.sdc.biceps.model.participant.LimitAlertConditionState;
import org.somda.sdc.biceps.model.participant.OperatingMode;
import org.somda.sdc.biceps.model.participant.Range;

/**
 * Defines required default values for all states.
 * <p>
 * Derive from this class to define more defaults. Required elements/attributes and their default values:
 * <ul>
 * <li>{@link AbstractAlertState#setActivationState(AlertActivation)} is set to {@link AlertActivation#ON}
 * <li>{@link LimitAlertConditionState#setLimits(Range)} is set to empty {@link Range}
 * <li>{@link LimitAlertConditionState#setMonitoredAlertLimits(AlertConditionMonitoredLimits)}
 * is set to {@link AlertConditionMonitoredLimits#ALL}
 * <li>{@link AbstractOperationState#setOperatingMode(OperatingMode)} is set to {@link OperatingMode#EN}
 * <li>{@link ClockState#setRemoteSync(boolean)} is set to false
 * </ul>
 */
public class RequiredDefaultStateValues implements DefaultStateValues {
    void onAbstractAlertState(AbstractAlertState state) {
        state.setActivationState(AlertActivation.ON);
    }

    void onLimitAlertConditionState(LimitAlertConditionState state) {
        onAbstractAlertState(state);
        state.setLimits(new Range());
        state.setMonitoredAlertLimits(AlertConditionMonitoredLimits.ALL);
    }

    void onAbstractOperationState(AbstractOperationState state) {
        state.setOperatingMode(OperatingMode.EN);
    }

    void onClockState(ClockState state) {
        state.setRemoteSync(false);
    }
}
