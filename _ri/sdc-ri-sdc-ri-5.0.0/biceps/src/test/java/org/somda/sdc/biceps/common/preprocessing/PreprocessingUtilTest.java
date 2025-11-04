package org.somda.sdc.biceps.common.preprocessing;

import com.google.inject.Injector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.biceps.UnitTestUtil;
import org.somda.sdc.biceps.common.storage.DescriptionPreprocessingSegment;
import org.somda.sdc.biceps.common.storage.StatePreprocessingSegment;
import org.somda.sdc.biceps.consumer.preprocessing.DuplicateContextStateHandleHandler;
import org.somda.sdc.biceps.provider.preprocessing.DuplicateChecker;
import org.somda.sdc.biceps.provider.preprocessing.HandleReferenceHandler;
import org.somda.sdc.biceps.provider.preprocessing.TypeConsistencyChecker;
import org.somda.sdc.biceps.provider.preprocessing.VersionHandler;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link PreprocessingUtil}.
 */
class PreprocessingUtilTest {
    private static final UnitTestUtil UT = new UnitTestUtil();

    private List<DescriptionPreprocessingSegment> descriptionPreprocessingSegmentList;
    private List<StatePreprocessingSegment> statePreprocessingSegmentList;

    private List<Class<? extends DescriptionPreprocessingSegment>> descriptionPreprocessingClassesList;
    private List<Class<? extends StatePreprocessingSegment>> statePreprocessingClassesList;

    private Injector injector;

    @BeforeEach
    void setup() {
        descriptionPreprocessingSegmentList = new ArrayList<>();
        statePreprocessingSegmentList = new ArrayList<>();

        injector = UT.getInjector();
    }

    @Test
    void testGetDescriptionPreprocessingSegments() {
        descriptionPreprocessingClassesList = List.of(DuplicateChecker.class, TypeConsistencyChecker.class, VersionHandler.class,
                HandleReferenceHandler.class, DescriptorChildRemover.class);

        descriptionPreprocessingSegmentList = PreprocessingUtil.getDescriptionPreprocessingSegments(descriptionPreprocessingClassesList, injector);
        assertEquals(5, descriptionPreprocessingSegmentList.size(),
                String.format("There should be 5 descriptionPreprocessingSegments, but there are %s", descriptionPreprocessingSegmentList.size()));

        //assert the right order
        assertTrue(descriptionPreprocessingSegmentList.get(0).getClass().isAssignableFrom(DuplicateChecker.class));
        assertTrue(descriptionPreprocessingSegmentList.get(1).getClass().isAssignableFrom(TypeConsistencyChecker.class));
        assertTrue(descriptionPreprocessingSegmentList.get(2).getClass().isAssignableFrom(VersionHandler.class));
        assertTrue(descriptionPreprocessingSegmentList.get(3).getClass().isAssignableFrom(HandleReferenceHandler.class));
        assertTrue(descriptionPreprocessingSegmentList.get(4).getClass().isAssignableFrom(DescriptorChildRemover.class));
    }

    @Test
    void testGetStatePreprocessingSegments() {
        descriptionPreprocessingClassesList = List.of(DuplicateChecker.class, TypeConsistencyChecker.class, VersionHandler.class,
                HandleReferenceHandler.class, DescriptorChildRemover.class);
        statePreprocessingClassesList = List.of(DuplicateContextStateHandleHandler.class, VersionHandler.class);

        //prepare the list of descriptionPreprocessingSegments
        descriptionPreprocessingSegmentList = PreprocessingUtil.getDescriptionPreprocessingSegments(descriptionPreprocessingClassesList, injector);

        statePreprocessingSegmentList = PreprocessingUtil.getStatePreprocessingSegments(statePreprocessingClassesList, descriptionPreprocessingSegmentList, injector);
        assertEquals(2, statePreprocessingSegmentList.size(),
                String.format("There should be 2 statePreprocessingSegments, but there are %s", statePreprocessingSegmentList.size()));

        //assert the right order
        assertTrue(statePreprocessingSegmentList.get(0).getClass().isAssignableFrom(DuplicateContextStateHandleHandler.class));
        assertTrue(statePreprocessingSegmentList.get(1).getClass().isAssignableFrom(VersionHandler.class));

        var versionHandlerFromDescriptionList = descriptionPreprocessingSegmentList.stream().filter(
                segment -> segment.getClass().isAssignableFrom(VersionHandler.class)
        ).findFirst();
        var versionHandlerFromStateList = statePreprocessingSegmentList.stream().filter(
                segment -> segment.getClass().isAssignableFrom(VersionHandler.class)
        ).findFirst();

        assertSame(versionHandlerFromDescriptionList.get(), versionHandlerFromStateList.get(),
                String.format("StatePreprocessingSegments do not contain the same"
                        + " VersionHandler instance as DescriptionPreprocessingSegments:"
                        + " %s", versionHandlerFromDescriptionList.get()));
    }
}
