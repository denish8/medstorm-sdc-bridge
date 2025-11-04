package com.example.consumer1;

import com.google.common.eventbus.Subscribe;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.somda.sdc.biceps.common.access.MdibAccessObserver;
import org.somda.sdc.biceps.common.event.AbstractMdibAccessMessage;
import org.somda.sdc.biceps.common.event.AlertStateModificationMessage;
import org.somda.sdc.biceps.common.event.ContextStateModificationMessage;
import org.somda.sdc.biceps.common.event.MetricStateModificationMessage;
import org.somda.sdc.biceps.common.event.OperationStateModificationMessage;
import org.somda.sdc.biceps.common.event.WaveformStateModificationMessage;
import org.somda.sdc.biceps.model.participant.AlertConditionState;

import java.util.HashMap;

/**
 * This class handles incoming reports on the provider.
 * <p>
 * Every incoming report triggers the respective handler, or the generic onUpdate handler if no
 * specialized handler is found.
 */
public class ConsumerReportProcessor implements MdibAccessObserver {
    private static final Logger LOG = LogManager.getLogger(ConsumerReportProcessor.class);


    private final HashMap<String, Long> metricChanges = new HashMap<>();
    private final HashMap<String, Long> conditionChanges = new HashMap<>();

    @Subscribe
    void onUpdate(AbstractMdibAccessMessage updates) {
        LOG.debug("onUpdate: {}", updates);
    }

    @Subscribe
    void onMetricChange(MetricStateModificationMessage modificationMessage) {
        LOG.info("onMetricChange");
        modificationMessage.getStates().forEach(
            (mdsHandle, states) -> {
                for (var state : states) {
                    LOG.info(state.toString());
                    var stateHandle = state.getDescriptorHandle();
                    var current = metricChanges.getOrDefault(stateHandle, 0L);
                    metricChanges.put(stateHandle, ++current);
                    LOG.info("{} has changed", state.getDescriptorHandle());
                }
            }
        );
    }

    @Subscribe
    void onWaveformChange(WaveformStateModificationMessage modificationMessage) {
        LOG.info("New waveform");
    }

    @Subscribe
    void onContextChange(ContextStateModificationMessage modificationMessage) {
        LOG.info("Context change");
    }

    @Subscribe
    void onAlertChange(AlertStateModificationMessage modificationMessage) {
        LOG.info("onAlertChange");
        modificationMessage.getStates()
            .values()
            .stream()
            .flatMap(it -> it.stream())
            .filter(it -> it instanceof AlertConditionState).forEach(
            state -> {
                var stateHandle = state.getDescriptorHandle();
                var current = conditionChanges.getOrDefault(stateHandle, 0L);
                conditionChanges.put(stateHandle, ++current);
                LOG.info("{} has changed", state.getDescriptorHandle());
            }
        );
    }

    @Subscribe
    void onOperationChange(OperationStateModificationMessage modificationMessage) {
        LOG.info("onOperationChange");
    }

    public HashMap<String, Long> getMetricChanges() {
        return metricChanges;
    }

    public HashMap<String, Long> getConditionChanges() {
        return conditionChanges;
    }
}
