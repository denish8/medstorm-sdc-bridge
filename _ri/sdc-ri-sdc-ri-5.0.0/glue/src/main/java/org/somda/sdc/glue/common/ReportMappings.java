package org.somda.sdc.glue.common;

import com.google.inject.Inject;
import org.somda.sdc.biceps.model.message.AbstractAlertReport;
import org.somda.sdc.biceps.model.message.AbstractComponentReport;
import org.somda.sdc.biceps.model.message.AbstractContextReport;
import org.somda.sdc.biceps.model.message.AbstractMetricReport;
import org.somda.sdc.biceps.model.message.AbstractOperationalStateReport;
import org.somda.sdc.biceps.model.message.AbstractReport;
import org.somda.sdc.biceps.model.message.EpisodicAlertReport;
import org.somda.sdc.biceps.model.message.EpisodicComponentReport;
import org.somda.sdc.biceps.model.message.EpisodicContextReport;
import org.somda.sdc.biceps.model.message.EpisodicMetricReport;
import org.somda.sdc.biceps.model.message.EpisodicOperationalStateReport;
import org.somda.sdc.biceps.model.message.PeriodicAlertReport;
import org.somda.sdc.biceps.model.message.PeriodicComponentReport;
import org.somda.sdc.biceps.model.message.PeriodicContextReport;
import org.somda.sdc.biceps.model.message.PeriodicMetricReport;
import org.somda.sdc.biceps.model.message.PeriodicOperationalStateReport;
import org.somda.sdc.biceps.model.participant.AbstractAlertState;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.AbstractDeviceComponentState;
import org.somda.sdc.biceps.model.participant.AbstractMetricState;
import org.somda.sdc.biceps.model.participant.AbstractOperationState;
import org.somda.sdc.biceps.model.participant.AbstractState;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reflection utility that provides class mappings between reports and other information items.
 */
public class ReportMappings {
    private final Map<Class<? extends AbstractReport>, Class<? extends AbstractState>> reportStateMapping;
    private final Map<Class<? extends AbstractState>, Class<? extends AbstractReport>> episodicStateReportMapping;
    private final Map<Class<? extends AbstractState>, Class<? extends AbstractReport>> periodicStateReportMapping;
    private final Map<Class<? extends AbstractReport>, String> episodicReportActionMapping;
    private final Map<Class<? extends AbstractReport>, String> periodicReportActionMapping;

    private final Set<Class<? extends AbstractReport>> reportMessageTypes;
    private final Set<Class<? extends AbstractState>> reportStateTypes;

    @Inject
    ReportMappings() {
        reportStateMapping = Map.of(
                AbstractAlertReport.class, AbstractAlertState.class,
                AbstractComponentReport.class, AbstractDeviceComponentState.class,
                AbstractContextReport.class, AbstractContextState.class,
                AbstractMetricReport.class, AbstractMetricState.class,
                AbstractOperationalStateReport.class, AbstractOperationState.class);
        reportMessageTypes = Collections.unmodifiableSet(reportStateMapping.keySet());

        episodicStateReportMapping = Map.of(
                AbstractAlertState.class, EpisodicAlertReport.class,
                AbstractDeviceComponentState.class, EpisodicComponentReport.class,
                AbstractContextState.class, EpisodicContextReport.class,
                AbstractMetricState.class, EpisodicMetricReport.class,
                AbstractOperationState.class, EpisodicOperationalStateReport.class);

        periodicStateReportMapping = Map.of(
                AbstractAlertState.class, PeriodicAlertReport.class,
                AbstractDeviceComponentState.class, PeriodicComponentReport.class,
                AbstractContextState.class, PeriodicContextReport.class,
                AbstractMetricState.class, PeriodicMetricReport.class,
                AbstractOperationState.class, PeriodicOperationalStateReport.class);

        reportStateTypes = Set.of(
                AbstractAlertState.class,
                AbstractDeviceComponentState.class,
                AbstractContextState.class,
                AbstractMetricState.class,
                AbstractOperationState.class);

        episodicReportActionMapping = Map.of(
                EpisodicAlertReport.class, ActionConstants.ACTION_EPISODIC_ALERT_REPORT,
                EpisodicComponentReport.class, ActionConstants.ACTION_EPISODIC_COMPONENT_REPORT,
                EpisodicContextReport.class, ActionConstants.ACTION_EPISODIC_CONTEXT_REPORT,
                EpisodicMetricReport.class, ActionConstants.ACTION_EPISODIC_METRIC_REPORT,
                EpisodicOperationalStateReport.class, ActionConstants.ACTION_EPISODIC_OPERATIONAL_STATE_REPORT);

        periodicReportActionMapping = Map.of(
                PeriodicAlertReport.class, ActionConstants.ACTION_PERIODIC_ALERT_REPORT,
                PeriodicComponentReport.class, ActionConstants.ACTION_PERIODIC_COMPONENT_REPORT,
                PeriodicContextReport.class, ActionConstants.ACTION_PERIODIC_CONTEXT_REPORT,
                PeriodicMetricReport.class, ActionConstants.ACTION_PERIODIC_METRIC_REPORT,
                PeriodicOperationalStateReport.class, ActionConstants.ACTION_PERIODIC_OPERATIONAL_STATE_REPORT);
    }

    public Class<? extends AbstractReport> getEpisodicReportClass(Class<? extends AbstractState> stateClass) {
        return getReportClass(stateClass, episodicStateReportMapping);
    }

    public Class<? extends AbstractReport> getPeriodicReportClass(Class<? extends AbstractState> stateClass) {
        return getReportClass(stateClass, periodicStateReportMapping);
    }

    private static Class<? extends AbstractReport> getReportClass(Class<? extends AbstractState> stateClass,
                                                                  Map<Class<? extends AbstractState>,
                                                                          Class<? extends AbstractReport>> mapping) {
        Class<?> superClass = stateClass;
        while (superClass != null) {
            final Class<? extends AbstractReport> reportClass = mapping.get(superClass);
            if (reportClass != null) {
                return reportClass;
            }
            superClass = superClass.getSuperclass();
        }
        throw new RuntimeException(String.format("Unknown state class found: %s", stateClass));
    }

    public String getEpisodicAction(Class<? extends AbstractReport> reportClass) {
        return getAction(reportClass, episodicReportActionMapping).orElseThrow(() ->
                new UnknownReportClassFoundException(reportClass));
    }

    public String getPeriodicAction(Class<? extends AbstractReport> reportClass) {
        return getAction(reportClass, periodicReportActionMapping).orElseThrow(() ->
               new UnknownReportClassFoundException(reportClass));
    }

    public String getAction(Class<? extends AbstractReport> reportClass) {
        return getAction(reportClass, episodicReportActionMapping).orElseGet(() ->
                getAction(reportClass, periodicReportActionMapping).orElseThrow(() ->
                            new UnknownReportClassFoundException(reportClass)));
    }

    private Optional<String> getAction(Class<? extends AbstractReport> reportClass,
                                       Map<Class<? extends AbstractReport>, String> mapping) {
        return Optional.ofNullable(mapping.get(reportClass));
    }

    public static class UnknownReportClassFoundException extends RuntimeException {
        public UnknownReportClassFoundException(Class<? extends AbstractReport> reportClass) {
            super(String.format("Unknown report class found: %s", reportClass));
        }
    }
}
