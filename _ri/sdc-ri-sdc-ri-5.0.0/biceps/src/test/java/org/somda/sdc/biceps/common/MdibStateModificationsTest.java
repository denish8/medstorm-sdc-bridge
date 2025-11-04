package org.somda.sdc.biceps.common;

import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.AlertSignalState;
import org.somda.sdc.biceps.model.participant.NumericMetricState;
import org.somda.sdc.biceps.model.participant.StringMetricState;
import org.somda.sdc.biceps.provider.HandleGenerator;
import org.somda.sdc.biceps.testutil.MockModelFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.org.somda.common.LoggingTestWatcher;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(LoggingTestWatcher.class)
class MdibStateModificationsTest {
    private HandleGenerator handleGenerator;

    @BeforeEach
    public void setUp() {
        handleGenerator = HandleGenerator.create("test");
    }

    @Test
    void differentStateTypes() {
        int stateCount = 10;
        Collection<MdibStateModifications.Type> changeTypes = EnumSet.allOf(MdibStateModifications.Type.class);
        for (MdibStateModifications.Type changeType : changeTypes) {
            runTestForType(changeType, stateCount);
        }
    }

    private void runTestForType(MdibStateModifications.Type type, int stateCount) {
        List<AbstractState> states = new ArrayList<>();

        for (int i = 0; i < stateCount; ++i) {
            states.add(MockModelFactory.createState(handleGenerator.next(), type.getChangeBaseClass()));
        }

        assertThat(states.size(), is(stateCount));

        final MdibStateModifications stateModifications = MdibStateModifications.create(type);
        states.stream().forEach(stateModifications::add);
        assertThat(stateModifications.getStates().size(), is(states.size()));
        for (int i = 0; i < states.size(); ++i) {
            assertThat(stateModifications.getStates().get(i), is(states.get(i)));
        }
    }

    @Test
    void typeMismatch()  {
        final List<AbstractState> validMismatch = Arrays.asList(
                MockModelFactory.createState(handleGenerator.next(), NumericMetricState.class),
                MockModelFactory.createState(handleGenerator.next(), StringMetricState.class),
                MockModelFactory.createState(handleGenerator.next(), StringMetricState.class)
        );

        final List<AbstractState> invalidMismatch = Arrays.asList(
                MockModelFactory.createState(handleGenerator.next(), NumericMetricState.class),
                MockModelFactory.createState(handleGenerator.next(), AlertSignalState.class),
                MockModelFactory.createState(handleGenerator.next(), StringMetricState.class)
        );

        runTestForType(MdibStateModifications.Type.METRIC, validMismatch);

        try {
            runTestForType(MdibStateModifications.Type.ALERT, invalidMismatch);
            Assertions.fail("Invalid type mismatch as ClassCastException expected, but none was thrown ");
        } catch (ClassCastException e) {
        } catch (Exception e) {
            Assertions.fail("Unexpected exception was thrown with message: " + e.getMessage());
        }
    }

    private void runTestForType(MdibStateModifications.Type type, List<AbstractState> states) {
        final MdibStateModifications stateModifications = MdibStateModifications.create(type);
        stateModifications.addAll(states);
        assertThat(stateModifications.getStates().size(), is(states.size()));
        for (int i = 0; i < states.size(); ++i) {
            assertThat(stateModifications.getStates().get(i), is(states.get(i)));
        }
    }
}
