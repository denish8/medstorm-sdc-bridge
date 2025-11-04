package org.somda.sdc.biceps.common;

import com.google.common.collect.HashMultimap;
// CHECKSTYLE.OFF: AvoidStarImport
// this is just too much to import without a star
import org.somda.sdc.biceps.model.participant.*;
// CHECKSTYLE.ON: AvoidStarImport

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class to verify cardinality and parent-child type correctness.
 */
public class MdibTreeValidator {
    private final HashMultimap<Class<?>, Class<?>> allowedParents;
    private final Set<Class<?>> oneChildEntities;

    @Inject
    MdibTreeValidator() {
        this.allowedParents = HashMultimap.create();
        this.oneChildEntities = new HashSet<>();

        setupAllowedParents();
        setupChildCardinality();
    }

    /**
     * Checks if a descriptor is allowed to appear many times as a child.
     *
     * @param child the descriptor to check.
     * @param <T>   a descriptor type.
     * @return true if it is allowed to appear more than once, false otherwise.
     */
    public <T extends AbstractDescriptor> boolean isManyAllowed(T child) {
        return !oneChildEntities.contains(child.getClass());
    }

    /**
     * Checks if the parent child descriptor relation is eligible.
     *
     * @param parent the parent descriptor to check against.
     * @param child  the child descriptor to check against.
     * @return true if parent child relationship is eligible in terms of the BICEPS hierarchy.
     */
    public boolean isValidParent(AbstractDescriptor parent, AbstractDescriptor child) {
        return allowedParents.containsEntry(child.getClass(), parent.getClass());
    }

    /**
     * Resolves allowed parents descriptor type for a given child descriptor type.
     *
     * @param child the child where to retrieve parents for.
     * @return a set of parent descriptor classes.
     */
    public Set<Class<?>> allowedParents(AbstractDescriptor child) {
        return allowedParents.get(child.getClass());
    }

    private void setupAllowedParents() {
        allowedParents.put(VmdDescriptor.class, MdsDescriptor.class);
        allowedParents.put(ChannelDescriptor.class, VmdDescriptor.class);
        allowedParents.put(NumericMetricDescriptor.class, ChannelDescriptor.class);
        allowedParents.put(StringMetricDescriptor.class, ChannelDescriptor.class);
        allowedParents.put(EnumStringMetricDescriptor.class, ChannelDescriptor.class);
        allowedParents.put(RealTimeSampleArrayMetricDescriptor.class, ChannelDescriptor.class);
        allowedParents.put(DistributionSampleArrayMetricDescriptor.class, ChannelDescriptor.class);
        allowedParents.replaceValues(
                AlertSystemDescriptor.class,
                Arrays.asList(MdsDescriptor.class, VmdDescriptor.class)
        );
        allowedParents.put(AlertConditionDescriptor.class, AlertSystemDescriptor.class);
        allowedParents.put(LimitAlertConditionDescriptor.class, AlertSystemDescriptor.class);
        allowedParents.put(AlertSignalDescriptor.class, AlertSystemDescriptor.class);
        allowedParents.replaceValues(ScoDescriptor.class, Arrays.asList(MdsDescriptor.class, VmdDescriptor.class));
        allowedParents.put(ActivateOperationDescriptor.class, ScoDescriptor.class);
        allowedParents.put(SetStringOperationDescriptor.class, ScoDescriptor.class);
        allowedParents.put(SetValueOperationDescriptor.class, ScoDescriptor.class);
        allowedParents.put(SetComponentStateOperationDescriptor.class, ScoDescriptor.class);
        allowedParents.put(SetMetricStateOperationDescriptor.class, ScoDescriptor.class);
        allowedParents.put(SetContextStateOperationDescriptor.class, ScoDescriptor.class);
        allowedParents.put(SetAlertStateOperationDescriptor.class, ScoDescriptor.class);
        allowedParents.put(ClockDescriptor.class, MdsDescriptor.class);
        allowedParents.put(BatteryDescriptor.class, MdsDescriptor.class);
        allowedParents.put(SystemContextDescriptor.class, MdsDescriptor.class);
        allowedParents.put(PatientContextDescriptor.class, SystemContextDescriptor.class);
        allowedParents.put(LocationContextDescriptor.class, SystemContextDescriptor.class);
        allowedParents.put(EnsembleContextDescriptor.class, SystemContextDescriptor.class);
        allowedParents.put(WorkflowContextDescriptor.class, SystemContextDescriptor.class);
        allowedParents.put(MeansContextDescriptor.class, SystemContextDescriptor.class);
        allowedParents.put(OperatorContextDescriptor.class, SystemContextDescriptor.class);
    }

    void setupChildCardinality() {
        oneChildEntities.add(AlertSystemDescriptor.class);
        oneChildEntities.add(SystemContextDescriptor.class);
        oneChildEntities.add(ScoDescriptor.class);
        oneChildEntities.add(PatientContextDescriptor.class);
        oneChildEntities.add(LocationContextDescriptor.class);
    }
}
