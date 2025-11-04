package org.somda.sdc.biceps.common;

import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.testutil.MockModelFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import test.org.somda.common.LoggingTestWatcher;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(LoggingTestWatcher.class)
class MdibDescriptionModificationsTest {
    @Test
    void singleState() throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        final List<AbstractDescriptor> descriptors = Arrays.asList(
                MockModelFactory.createDescriptor("h1", NumericMetricDescriptor.class),
                MockModelFactory.createDescriptor("h2", StringMetricDescriptor.class),
                MockModelFactory.createDescriptor("h3", SystemContextDescriptor.class),
                MockModelFactory.createDescriptor("h4", BatteryDescriptor.class));
        final List<AbstractState> states = Arrays.asList(
                MockModelFactory.createState("h1", NumericMetricState.class),
                MockModelFactory.createState("h2", StringMetricState.class),
                MockModelFactory.createState("h3", SystemContextState.class),
                MockModelFactory.createState("h4", BatteryState.class));
        MdibDescriptionModifications mdibDescriptionModifications = MdibDescriptionModifications.create();

        for (int i = 0; i < descriptors.size(); ++i) {
            mdibDescriptionModifications.insert(descriptors.get(i), states.get(i));
        }
        for (int i = 0; i < descriptors.size(); ++i) {
            mdibDescriptionModifications.update(descriptors.get(i), states.get(i));
        }
        for (int i = 0; i < descriptors.size(); ++i) {
            mdibDescriptionModifications.add(MdibDescriptionModification.Type.DELETE, descriptors.get(i),
                    states.get(i));
        }

        checkResultsSingleState(mdibDescriptionModifications, descriptors, states,
                MdibDescriptionModification.Type.INSERT, descriptors.size(), 0);
        checkResultsSingleState(mdibDescriptionModifications, descriptors, states,
                MdibDescriptionModification.Type.UPDATE, descriptors.size(), descriptors.size());
        checkResultsSingleState(mdibDescriptionModifications, descriptors, states,
                MdibDescriptionModification.Type.DELETE, descriptors.size(), descriptors.size() * 2);
    }

    void checkResultsSingleState(MdibDescriptionModifications actualChangeSet,
                                 List<AbstractDescriptor> expectedDescriptors,
                                 List<AbstractState> expectedStates,
                                 MdibDescriptionModification.Type modificationType,
                                 int size,
                                 int offset) {
        for (int i = 0; i < size; ++i) {
            MdibDescriptionModification item = actualChangeSet.getModifications().get(offset + i);
            assertThat(item.getModificationType(), is(modificationType));
            assertThat(item.getDescriptor().getHandle(), is(expectedDescriptors.get(i).getHandle()));
            assertThat(item.getStates().size(), is(1));
            assertThat(item.getStates().get(0).getDescriptorHandle(), is(expectedStates.get(i).getDescriptorHandle()));
        }
    }

    @Test
    void multiState() throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        final List<AbstractContextDescriptor> descriptors = Arrays.asList(
                MockModelFactory.createDescriptor("c1", LocationContextDescriptor.class),
                MockModelFactory.createDescriptor("c2", PatientContextDescriptor.class));
        final Map<String, List<AbstractMultiState>> states = new HashMap<>();
        states.put("c1", Arrays.asList(
                MockModelFactory.createContextState("cs1", "c1", LocationContextState.class),
                MockModelFactory.createContextState("cs2", "c1", LocationContextState.class)));
        states.put("c2", Arrays.asList(
                MockModelFactory.createContextState("cs3", "c2", PatientContextState.class),
                MockModelFactory.createContextState("cs4", "c2", PatientContextState.class)));

        MdibDescriptionModifications mdibDescriptionModifications = MdibDescriptionModifications.create();

        for (AbstractContextDescriptor descriptor : descriptors) {
            mdibDescriptionModifications.insert(descriptor, states.get(descriptor.getHandle()));
        }
        for (AbstractContextDescriptor descriptor : descriptors) {
            mdibDescriptionModifications.update(descriptor, states.get(descriptor.getHandle()));
        }
        for (AbstractContextDescriptor descriptor : descriptors) {
            mdibDescriptionModifications.add(MdibDescriptionModification.Type.DELETE, descriptor,
                                             states.get(descriptor.getHandle()));
        }

        checkResultsMultiState(mdibDescriptionModifications, descriptors, states,
                MdibDescriptionModification.Type.INSERT, descriptors.size(), 0);
        checkResultsMultiState(mdibDescriptionModifications, descriptors, states,
                MdibDescriptionModification.Type.UPDATE, descriptors.size(), descriptors.size());
        checkResultsMultiState(mdibDescriptionModifications, descriptors, states,
                MdibDescriptionModification.Type.DELETE, descriptors.size(), descriptors.size() * 2);
    }

    void checkResultsMultiState(MdibDescriptionModifications actualChangeSet,
                                List<AbstractContextDescriptor> expectedDescriptors,
                                Map<String, List<AbstractMultiState>> expectedStates,
                                MdibDescriptionModification.Type modificationType,
                                int size,
                                int offset) {
        for (int i = 0; i < size; ++i) {
            MdibDescriptionModification item = actualChangeSet.getModifications().get(offset + i);
            assertThat(item.getModificationType(), is(modificationType));
            assertThat(item.getDescriptor().getHandle(), is(expectedDescriptors.get(i).getHandle()));
            assertThat(item.getStates().size(), is(expectedStates.get(item.getDescriptor().getHandle()).size()));
            for (int j = 0; j < item.getStates().size(); ++j) {
                final AbstractMultiState state = expectedStates.get(item.getDescriptor().getHandle()).get(j);
                assertThat(item.getStates().get(j).getDescriptorHandle(), is(state.getDescriptorHandle()));
            }
        }
    }

    @Test
    void handleDuplicate() throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        final List<AbstractDescriptor> descriptors = Arrays.asList(
                MockModelFactory.createDescriptor("h1", NumericMetricDescriptor.class),
                MockModelFactory.createDescriptor("h2", AlertSignalDescriptor.class));

        MdibDescriptionModifications mdibDescriptionModifications = MdibDescriptionModifications.create();
        try {
            mdibDescriptionModifications.insert(descriptors.get(0));
            mdibDescriptionModifications.insert(descriptors.get(1));
            mdibDescriptionModifications.insert(descriptors.get(2));
            Assertions.fail("Expected duplicate runtime exception not thrown");
        } catch (RuntimeException e) {
        }

        mdibDescriptionModifications.update(descriptors.get(0));
        mdibDescriptionModifications.update(descriptors.get(1));

        try {
            mdibDescriptionModifications.update(descriptors.get(0));
            Assertions.fail("Expected duplicate runtime exception not thrown");
        } catch (RuntimeException e) {
        }

        mdibDescriptionModifications.delete(descriptors.get(0));
        mdibDescriptionModifications.delete(descriptors.get(1));

        try {
            mdibDescriptionModifications.delete(descriptors.get(0));
            Assertions.fail("Expected duplicate runtime exception not thrown");
        } catch (RuntimeException e) {
        }

        final List<EnsembleContextState> contextStates = Arrays.asList(
                MockModelFactory.createContextState("cs1", "h3", EnsembleContextState.class),
                MockModelFactory.createContextState("h1", "h3", EnsembleContextState.class));

        try {
            mdibDescriptionModifications.insert(MockModelFactory.createDescriptor("h3", EnsembleContextDescriptor.class), contextStates);
            Assertions.fail("Expected duplicate runtime exception not thrown");
        } catch (RuntimeException e) {
        }
    }

}