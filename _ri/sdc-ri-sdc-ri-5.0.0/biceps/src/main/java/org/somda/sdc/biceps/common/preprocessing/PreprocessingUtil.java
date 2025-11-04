package org.somda.sdc.biceps.common.preprocessing;

import com.google.inject.Injector;
import org.somda.sdc.biceps.common.storage.DescriptionPreprocessingSegment;
import org.somda.sdc.biceps.common.storage.StatePreprocessingSegment;

import java.util.ArrayList;
import java.util.List;

/**
 * Preprocessing utilities.
 */
public class PreprocessingUtil {

    /**
     * Takes a list of classes which extends {@linkplain DescriptionPreprocessingSegment} and
     * returns a list with instances of these {@linkplain DescriptionPreprocessingSegment}.
     *
     * @param descriptionSegments list of injection types.
     * @param injector           to retrieve instances of injection types.
     * @return instances of specified {@linkplain DescriptionPreprocessingSegment}.
     */
    public static List<DescriptionPreprocessingSegment> getDescriptionPreprocessingSegments(
            List<Class<? extends DescriptionPreprocessingSegment>> descriptionSegments,
            Injector injector) {
        var descriptionPreprocessingSegments = new ArrayList<DescriptionPreprocessingSegment>();
        for (Class<? extends DescriptionPreprocessingSegment> segment : descriptionSegments) {
            descriptionPreprocessingSegments.add(injector.getInstance(segment));
        }
        return descriptionPreprocessingSegments;
    }

    /**
     * Takes a list of classes which extends {@linkplain StatePreprocessingSegment} and
     * returns a list with instances of these {@linkplain StatePreprocessingSegment}, when a class also implements
     * {@linkplain DescriptionPreprocessingSegment} the instance of the descriptionPreprocessingSegments is used,
     * instead of a new one.
     *
     * @param stateSegments                   list of injection types.
     * @param descriptionPreprocessingSegments list of already retrieved {@linkplain DescriptionPreprocessingSegment}.
     * @param injector                        to retrieve instances of injection types.
     * @return instances of specified {@linkplain StatePreprocessingSegment}.
     */
    public static List<StatePreprocessingSegment> getStatePreprocessingSegments(
            List<Class<? extends StatePreprocessingSegment>> stateSegments,
            List<DescriptionPreprocessingSegment> descriptionPreprocessingSegments,
            Injector injector) {
        var statePreprocessingSegments = new ArrayList<StatePreprocessingSegment>();
        for (Class<? extends StatePreprocessingSegment> segment : stateSegments) {
            var existingSegment = descriptionPreprocessingSegments.stream()
                    .filter(descSegment -> segment.equals(descSegment.getClass())).findFirst();
            if (existingSegment.isPresent()) {
                statePreprocessingSegments.add((StatePreprocessingSegment) existingSegment.get());
            } else {
                statePreprocessingSegments.add(injector.getInstance(segment));
            }
        }
        return statePreprocessingSegments;
    }
}
