package org.somda.sdc.glue.common.helper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.glue.common.DefaultStateValues;
import test.org.somda.common.LoggingTestWatcher;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(LoggingTestWatcher.class)
class DefaultStateValuesDispatcherTest {
    static final String DEFAULT_HANDLE = "default-handle";
    static final ComponentActivation DEFAULT_COMPONENT_ACTIVATION = ComponentActivation.NOT_RDY;
    static final BigDecimal DEFAULT_NUMERIC_METRIC_VALUE_VALUE = BigDecimal.TEN;
    static final String DEFAULT_STRING_METRIC_VALUE_VALUE = "foobar";

    @Test
    void dispatchDefaultStateValues() throws Exception {
        var dispatcher = new  DefaultStateValuesDispatcher(new DefaultValues());

        var alertSignalState = new AlertSignalState();
        var enumStringMetricState = new EnumStringMetricState();
        var numericMetricState = new NumericMetricState();
        var realTimeSampleArrayMetricState = new RealTimeSampleArrayMetricState();

        dispatcher.dispatchDefaultStateValues(alertSignalState);
        dispatcher.dispatchDefaultStateValues(enumStringMetricState);
        dispatcher.dispatchDefaultStateValues(numericMetricState);
        dispatcher.dispatchDefaultStateValues(realTimeSampleArrayMetricState);

        assertEquals(DEFAULT_HANDLE, alertSignalState.getDescriptorHandle());
        assertEquals(null, enumStringMetricState.getDescriptorHandle());
        assertEquals(DEFAULT_HANDLE, numericMetricState.getDescriptorHandle());
        assertEquals(DEFAULT_HANDLE, realTimeSampleArrayMetricState.getDescriptorHandle());

        assertNotNull(numericMetricState.getMetricValue());
        assertEquals(DEFAULT_NUMERIC_METRIC_VALUE_VALUE, numericMetricState.getMetricValue().getValue());
        assertEquals(DEFAULT_STRING_METRIC_VALUE_VALUE, enumStringMetricState.getMetricValue().getValue());
        assertNull(realTimeSampleArrayMetricState.getMetricValue());

        assertEquals(DEFAULT_COMPONENT_ACTIVATION, numericMetricState.getActivationState());
        assertEquals(null, enumStringMetricState.getActivationState());
        assertEquals(DEFAULT_COMPONENT_ACTIVATION, realTimeSampleArrayMetricState.getActivationState());
    }

    static class DefaultValues implements DefaultStateValues {
        void onAbstractState(AbstractState state) {
            state.setDescriptorHandle(DEFAULT_HANDLE);
        }

        void onAbstractMetricState(AbstractMetricState state) {
            onAbstractState(state);
            state.setActivationState(DEFAULT_COMPONENT_ACTIVATION);
        }

        void on(NumericMetricState state) {
            onAbstractMetricState(state);
            var metricValue = new NumericMetricValue();
            metricValue.setValue(DEFAULT_NUMERIC_METRIC_VALUE_VALUE);
            state.setMetricValue(metricValue);
        }

        void on(StringMetricState state) {
            var metricValue = new StringMetricValue();
            metricValue.setValue(DEFAULT_STRING_METRIC_VALUE_VALUE);
            state.setMetricValue(metricValue);
        }
    }
}