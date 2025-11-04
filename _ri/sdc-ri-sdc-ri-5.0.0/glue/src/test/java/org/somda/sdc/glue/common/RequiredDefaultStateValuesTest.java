package org.somda.sdc.glue.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.glue.common.helper.DefaultStateValuesDispatcher;
import test.org.somda.common.LoggingTestWatcher;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(LoggingTestWatcher.class)
class RequiredDefaultStateValuesTest {
    @Test
    void requiredValuesPopulated() throws Exception {
        var dispatcher = new DefaultStateValuesDispatcher(new RequiredDefaultStateValues());

        var limitAlertConditionState = new LimitAlertConditionState();
        var activateOperationState = new ActivateOperationState();
        var clockState = new ClockState();

        dispatcher.dispatchDefaultStateValues(limitAlertConditionState);
        dispatcher.dispatchDefaultStateValues(activateOperationState);
        dispatcher.dispatchDefaultStateValues(clockState);

        assertEquals(AlertActivation.ON, limitAlertConditionState.getActivationState());
        assertEquals(AlertConditionMonitoredLimits.ALL, limitAlertConditionState.getMonitoredAlertLimits());
        assertNotNull(limitAlertConditionState.getLimits());
        assertNull(limitAlertConditionState.getLimits().getLower());
        assertNull(limitAlertConditionState.getLimits().getUpper());
        assertNull(limitAlertConditionState.getLimits().getExtension());
        assertNull(limitAlertConditionState.getLimits().getAbsoluteAccuracy());
        assertNull(limitAlertConditionState.getLimits().getRelativeAccuracy());
        assertNull(limitAlertConditionState.getLimits().getStepWidth());
        assertEquals(OperatingMode.EN, activateOperationState.getOperatingMode());
        assertFalse(clockState.isRemoteSync());
    }
}