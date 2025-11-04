package org.somda.sdc.biceps.common.event;

import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.model.participant.AbstractMetricState;

import java.util.List;
import java.util.Map;

/**
 * Subscribe to this message in order to receive metric state changes.
 */
public class MetricStateModificationMessage extends StateModificationMessage<AbstractMetricState> {
    /**
     * Constructor.
     *
     * @param mdibAccess the MDIB access for {@link AbstractMdibAccessMessage}.
     * @param states the states of the message.
     */
    public MetricStateModificationMessage(MdibAccess mdibAccess, Map<String, List<AbstractMetricState>> states) {
        super(mdibAccess, states);
    }
}
