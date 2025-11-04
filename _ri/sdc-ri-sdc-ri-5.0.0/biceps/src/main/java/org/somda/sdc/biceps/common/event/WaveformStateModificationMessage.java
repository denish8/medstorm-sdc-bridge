package org.somda.sdc.biceps.common.event;

import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.model.participant.RealTimeSampleArrayMetricState;

import java.util.List;
import java.util.Map;

/**
 * Subscribe to this message in order to receive waveform changes.
 */
public class WaveformStateModificationMessage extends StateModificationMessage<RealTimeSampleArrayMetricState> {
    /**
     * Constructor.
     *
     * @param mdibAccess the MDIB access for {@link AbstractMdibAccessMessage}.
     * @param states     the states of the message.
     */
    public WaveformStateModificationMessage(
        MdibAccess mdibAccess,
        Map<String, List<RealTimeSampleArrayMetricState>> states
    ) {
        super(mdibAccess, states);
    }
}
