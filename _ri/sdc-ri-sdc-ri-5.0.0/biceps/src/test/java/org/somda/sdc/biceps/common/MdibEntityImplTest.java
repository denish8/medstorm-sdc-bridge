package org.somda.sdc.biceps.common;

import com.google.inject.Injector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.UnitTestUtil;
import org.somda.sdc.biceps.common.factory.MdibEntityFactory;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.testutil.MockModelFactory;
import test.org.somda.common.LoggingTestWatcher;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(LoggingTestWatcher.class)
class MdibEntityImplTest {
    private static final UnitTestUtil UT = new UnitTestUtil();

    private MdibEntityFactory mdibEntityFactory;
    private String expectedDescriptorHandle;
    private String expectedMdsHandle;
    private List<String> expectedStateHandles;
    private MdibEntity mdibEntity;

    @BeforeEach
    void setUp() {
        Injector injector = UT.getInjector();
        mdibEntityFactory = injector.getInstance(MdibEntityFactory.class);
        expectedDescriptorHandle = "descrHandle";
        expectedMdsHandle = "mdsHandle";
        expectedStateHandles = Arrays.asList("stateHandle1", "stateHandle2", "stateHandle3");
        mdibEntity = mdibEntityFactory.createMdibEntity(
                null,
                Collections.emptyList(),
                MockModelFactory.createDescriptor(expectedDescriptorHandle, PatientContextDescriptor.class),
                expectedStateHandles.stream()
                        .map(handle -> MockModelFactory.createContextState(handle, expectedDescriptorHandle, PatientContextState.class))
                        .collect(Collectors.toList()),
                MdibVersion.create(),
            expectedMdsHandle
        );
    }

    @Test
    void mdibEntityGetters() {
        assertTrue(mdibEntity.getParent().isEmpty());
        assertTrue(mdibEntity.getChildren().isEmpty());
        assertThat(mdibEntity.getDescriptor(), instanceOf(PatientContextDescriptor.class));
        assertEquals(expectedDescriptorHandle, mdibEntity.getDescriptor().getHandle());
        assertEquals(expectedStateHandles.size(), mdibEntity.getStates().size());
        assertTrue(mdibEntity.getFirstState(NumericMetricState.class).isEmpty());
        assertTrue(mdibEntity.getFirstState(PatientContextState.class).isPresent());
        assertEquals(expectedStateHandles.get(0), mdibEntity.getFirstState(PatientContextState.class).get().getHandle());
        assertEquals(0, mdibEntity.getStates(StringMetricState.class).size());
        assertEquals(expectedStateHandles.size(), mdibEntity.getStates(PatientContextState.class).size());
        for (int i = 0; i < expectedStateHandles.size(); ++i) {
            assertThat(mdibEntity.getStates(PatientContextState.class).get(i), instanceOf(PatientContextState.class));
            PatientContextState state = mdibEntity.getStates(PatientContextState.class).get(i);
            assertEquals(expectedStateHandles.get(i), state.getHandle());
            assertEquals(expectedDescriptorHandle, state.getDescriptorHandle());
            assertEquals(expectedMdsHandle, mdibEntity.getParentMds());
        }
    }
}
