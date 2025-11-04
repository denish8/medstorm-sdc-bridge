package org.somda.sdc.biceps.consumer.preprocessing;

import com.google.inject.Inject;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.MdibTypeValidator;
import org.somda.sdc.biceps.common.storage.MdibStorage;
import org.somda.sdc.biceps.common.storage.StatePreprocessingSegment;
import org.somda.sdc.biceps.model.participant.AbstractMultiState;
import org.somda.sdc.biceps.model.participant.AbstractState;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Preprocessing segment that removes changes from modification sets that have already
 * been applied on the MDIB storage before.
 * <p>
 * This typically happens after a description change when states are sent out in addition
 * to the description modification message.
 * Those additional elements will be thrown away.
 */
public class VersionDuplicateHandler implements StatePreprocessingSegment {
    private final MdibTypeValidator typeValidator;

    private List<AbstractState> omittedStates;

    @Inject
    VersionDuplicateHandler(MdibTypeValidator typeValidator) {
        this.typeValidator = typeValidator;
    }

    @Override
    public void beforeFirstModification(MdibStateModifications modifications, MdibStorage mdibStorage) {
        omittedStates = null;
    }

    @Override
    public void process(MdibStateModifications modifications, AbstractState modification, MdibStorage storage) {
        String handle = modification.getDescriptorHandle();
        final Optional<AbstractMultiState> multiState = typeValidator.toMultiState(modification);
        if (multiState.isPresent()) {
            handle = multiState.get().getHandle();
        }

        final Optional<AbstractState> state = storage.getState(handle);
        if (state.isEmpty()) {
            return;
        }

        // If state version from storage is greater or equal than the one to be updated,
        // then assume the modification to be outdated => add to skipped states
        if (impliedValueMapper(state.get().getStateVersion())
                .compareTo(impliedValueMapper(modification.getStateVersion())) >= 0) {
            if (omittedStates == null) {
                omittedStates = new ArrayList<>(modifications.getStates().size());
            }
            omittedStates.add(modification);
        }
    }

    @Override
    public void afterLastModification(MdibStateModifications modifications, MdibStorage mdibStorage) {
        if (omittedStates == null) {
            return;
        }

        modifications.getStates().removeAll(omittedStates);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    private BigInteger impliedValueMapper(@Nullable BigInteger value) {
        if (value != null) {
            return value;
        }
        return BigInteger.ZERO;
    }
}
