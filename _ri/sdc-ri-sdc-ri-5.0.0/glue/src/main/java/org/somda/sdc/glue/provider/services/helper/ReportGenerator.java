package org.somda.sdc.glue.provider.services.helper;

import com.google.common.eventbus.Subscribe;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.access.MdibAccessObserver;
import org.somda.sdc.biceps.common.event.AlertStateModificationMessage;
import org.somda.sdc.biceps.common.event.ComponentStateModificationMessage;
import org.somda.sdc.biceps.common.event.ContextStateModificationMessage;
import org.somda.sdc.biceps.common.event.DescriptionModificationMessage;
import org.somda.sdc.biceps.common.event.MetricStateModificationMessage;
import org.somda.sdc.biceps.common.event.OperationStateModificationMessage;
import org.somda.sdc.biceps.common.event.WaveformStateModificationMessage;
import org.somda.sdc.biceps.model.message.AbstractReport;
import org.somda.sdc.biceps.model.message.AbstractReportPart;
import org.somda.sdc.biceps.model.message.DescriptionModificationReport;
import org.somda.sdc.biceps.model.message.DescriptionModificationType;
import org.somda.sdc.biceps.model.message.EpisodicAlertReport;
import org.somda.sdc.biceps.model.message.EpisodicComponentReport;
import org.somda.sdc.biceps.model.message.EpisodicContextReport;
import org.somda.sdc.biceps.model.message.EpisodicMetricReport;
import org.somda.sdc.biceps.model.message.EpisodicOperationalStateReport;
import org.somda.sdc.biceps.model.message.ObjectFactory;
import org.somda.sdc.biceps.model.message.WaveformStream;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.biceps.model.participant.RealTimeSampleArrayMetricState;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.device.EventSourceAccess;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.glue.common.MdibVersionUtil;
import org.somda.sdc.glue.common.ReportMappings;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Helper class to generate and send reports.
 * <p>
 * As there is currently support for one MDS only ({@link org.somda.sdc.biceps.common.MdibEntity} does not come
 * with an MDS handle reference), no SourceMds attribute is set and hence all changes go into one report part.
 */
public class ReportGenerator implements MdibAccessObserver {
    private static final Logger LOG = LogManager.getLogger(ReportGenerator.class);
    private static final String REFLECTION_ERROR_STRING = "Reflection error caught. Sending of notification aborted.";

    private final EventSourceAccess eventSourceAccess;
    private final ObjectFactory bicepsMessageFactory;
    private final ReportMappings reportMappings;
    private final MdibVersionUtil mdibVersionUtil;
    private final Logger instanceLogger;

    @AssistedInject
    ReportGenerator(@Assisted EventSourceAccess eventSourceAccess,
                    ObjectFactory bicepsMessageFactory,
                    ReportMappings reportMappings,
                    MdibVersionUtil mdibVersionUtil,
                    @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.eventSourceAccess = eventSourceAccess;
        this.bicepsMessageFactory = bicepsMessageFactory;
        this.reportMappings = reportMappings;
        this.mdibVersionUtil = mdibVersionUtil;
    }

    /**
     * Tries to send a periodic state event given states and an MDIB version.
     *
     * @param states      the states to put to the report.
     * @param mdibVersion the MDIB version used for the report.
     * @param <T>         the state type.
     */
    public <T extends AbstractState> void sendPeriodicStateReport(
        Map<String, List<T>> states,
        MdibVersion mdibVersion
    ) {
        if (states.isEmpty()) {
            return;
        }

        var reportClass = reportMappings.getPeriodicReportClass(
            states.values().stream().findFirst().orElseThrow().get(0).getClass()
        );
        sendStateChange(
                mdibVersion,
                states,
                reportClass);
    }

    @Subscribe
    void onAlertChange(AlertStateModificationMessage modificationMessage) {
        sendStateChange(
                modificationMessage.getMdibAccess().getMdibVersion(),
                modificationMessage.getStates(),
                EpisodicAlertReport.class);
    }

    @Subscribe
    void onComponentChange(ComponentStateModificationMessage modificationMessage) {
        sendStateChange(
                modificationMessage.getMdibAccess().getMdibVersion(),
                modificationMessage.getStates(),
                EpisodicComponentReport.class);
    }

    @Subscribe
    void onContextChange(ContextStateModificationMessage modificationMessage) {
        sendStateChange(
                modificationMessage.getMdibAccess().getMdibVersion(),
                modificationMessage.getStates(),
                EpisodicContextReport.class);
    }

    @Subscribe
    void onMetricChange(MetricStateModificationMessage modificationMessage) {
        sendStateChange(
                modificationMessage.getMdibAccess().getMdibVersion(),
                modificationMessage.getStates(),
                EpisodicMetricReport.class);
    }

    @Subscribe
    void onOperationChange(OperationStateModificationMessage modificationMessage) {
        sendStateChange(
                modificationMessage.getMdibAccess().getMdibVersion(),
                modificationMessage.getStates(),
                EpisodicOperationalStateReport.class);
    }

    @Subscribe
    void onDescriptionChange(DescriptionModificationMessage modificationMessage) {
        final DescriptionModificationReport report = bicepsMessageFactory.createDescriptionModificationReport();
        appendReport(report, DescriptionModificationType.DEL, modificationMessage.getDeletedEntities());
        appendReport(report, DescriptionModificationType.CRT, modificationMessage.getInsertedEntities());
        appendReport(report, DescriptionModificationType.UPT, modificationMessage.getUpdatedEntities());

        try {
            mdibVersionUtil.setMdibVersion(modificationMessage.getMdibAccess().getMdibVersion(), report);
            eventSourceAccess.sendNotification(ActionConstants.ACTION_DESCRIPTION_MODIFICATION_REPORT, report);
        } catch (MarshallingException e) {
            instanceLogger.warn("Could not marshal message for description modification report with version {}: {}",
                    modificationMessage.getMdibAccess().getMdibVersion(), e.getMessage());
            instanceLogger.trace("Could not marshal message for description modification report", e);
        } catch (TransportException e) {
            instanceLogger.info(
                    "Failed to deliver notification for description modification report with version {}: {}",
                    modificationMessage.getMdibAccess().getMdibVersion(), e.getMessage());
            instanceLogger.trace("Failed to deliver notification for description modification report", e);
        }

        dispatchStateEvents(modificationMessage.getMdibAccess().getMdibVersion(),
                modificationMessage.getInsertedEntities(),
                modificationMessage.getUpdatedEntities());
    }

    @Subscribe
    void onWaveformChange(WaveformStateModificationMessage modificationMessage) {
        sendWaveformChange(
                modificationMessage.getMdibAccess().getMdibVersion(),
                modificationMessage.getStates()
        );
    }

    private void dispatchStateEvents(MdibVersion mdibVersion, List<MdibEntity> insertedEntities,
                                     List<MdibEntity> updatedEntities) {
        // expectedKeys (which pertains to the key size of the multimap below) = 6 because of the following event types
        // - alert changes
        // - component change
        // - context changes
        // - metric changes
        // - operation changes
        // - waveform changes
        final Map<Class<? extends AbstractReport>, Map<String, List<AbstractState>>> classifiedStates =
            new HashMap<>(6);

        collectStates(classifiedStates, insertedEntities);
        collectStates(classifiedStates, updatedEntities);

        for (Class<? extends AbstractReport> aClass : classifiedStates.keySet()) {
            sendStateChange(
                    mdibVersion,
                    classifiedStates.get(aClass),
                    aClass);
        }
    }

    private void collectStates(
        Map<Class<? extends AbstractReport>, Map<String, List<AbstractState>>> classifiedStates,
        List<MdibEntity> entities
    ) {
        for (MdibEntity entity : entities) {
            var stateMap = classifiedStates.computeIfAbsent(
                reportMappings.getEpisodicReportClass(entity.getStateClass()), s -> new HashMap<>()
            );
            stateMap.computeIfAbsent(entity.getParentMds(), s -> new ArrayList<>()).addAll(entity.getStates());
        }
    }


    private void appendReport(DescriptionModificationReport report,
                              DescriptionModificationType modType,
                              List<MdibEntity> entities) {
        for (MdibEntity entity : entities) {
            final DescriptionModificationReport.ReportPart reportPart =
                    bicepsMessageFactory.createDescriptionModificationReportReportPart();
            reportPart.getDescriptor().add(entity.getDescriptor());
            reportPart.getState().addAll(entity.getStates());
            reportPart.setParentDescriptor(entity.getParent().orElse(null));
            reportPart.setModificationType(modType);
            // todo DGr add source MDS here if available
            report.getReportPart().add(reportPart);
        }
    }

    private void sendWaveformChange(MdibVersion mdibVersion, Map<String, List<RealTimeSampleArrayMetricState>> states) {
        if (states.isEmpty()) {
            return;
        }

        // flatten states
        List<RealTimeSampleArrayMetricState> flatStates = states.values()
            .stream()
            .flatMap(it -> it.stream())
            .collect(Collectors.toList());

        try {
            WaveformStream waveformStream = new WaveformStream();
            mdibVersionUtil.setMdibVersion(mdibVersion, waveformStream);
            waveformStream.setState(flatStates);
            eventSourceAccess.sendNotification(ActionConstants.ACTION_WAVEFORM_STREAM, waveformStream);
        } catch (MarshallingException e) {
            instanceLogger.warn("Could not marshal message for state action {} with version: {}. {}",
                    ActionConstants.ACTION_WAVEFORM_STREAM, mdibVersion, e.getMessage());
            instanceLogger.trace("Could not marshal message for state action {}",
                    ActionConstants.ACTION_WAVEFORM_STREAM, e);
        } catch (TransportException e) {
            instanceLogger.info("Failed to deliver notification for state action {} with version: {}. {}",
                    ActionConstants.ACTION_WAVEFORM_STREAM, mdibVersion, e.getMessage());
            instanceLogger.trace("Failed to deliver notification for state action {}",
                    ActionConstants.ACTION_WAVEFORM_STREAM, e);
        }
    }

    private <T, V extends AbstractReport> void sendStateChange(
        MdibVersion mdibVersion,
        Map<String, List<T>> states,
        Class<V> reportClass
    ) {
        if (states.isEmpty()) {
            return;
        }

        V report;
        try {
            final Constructor<V> reportCtor = reportClass.getConstructor();
            report = reportCtor.newInstance();

            mdibVersionUtil.setMdibVersion(mdibVersion, report);

            final Object reportParts = findGetReportPartMethod(reportClass).invoke(report);
            if (!List.class.isAssignableFrom(reportParts.getClass())) {
                throw new NoSuchMethodException(String.format("Returned report parts was not a list, it was of type %s",
                    reportParts.getClass()));
            }

            for (var entry : states.entrySet()) {
                final Class<?> reportPartClass = findReportPartClass(reportClass);
                final Constructor<?> reportPartCtor = reportPartClass.getConstructor();
                final AbstractReportPart reportPart = (AbstractReportPart) reportPartCtor.newInstance();

                findSetStateMethod(reportPartClass).invoke(reportPart, entry.getValue());
                reportPart.setSourceMds(entry.getKey());
                ((List) reportParts).add(reportPart);
            }
        } catch (ReflectiveOperationException e) {
            instanceLogger.warn(REFLECTION_ERROR_STRING, e);
            return;
        }

        var action = reportMappings.getAction(reportClass);
        try {
            eventSourceAccess.sendNotification(action, report);
        } catch (MarshallingException e) {
            instanceLogger.warn("Could not marshal message for state action {} with version: {}. {}",
                    action, mdibVersion, e.getMessage());
            instanceLogger.trace("Could not marshal message for state action {}",
                    action, e);
        } catch (TransportException e) {
            instanceLogger.info("Failed to deliver notification for state action {} with version: {}. {}",
                    action, mdibVersion, e.getMessage());
            instanceLogger.trace("Failed to deliver notification for state action {}", action, e);
        }
    }

    private Class<?> findReportPartClass(Class<?> reportClass) throws NoSuchFieldException {
        final Class<?>[] classes = reportClass.getClasses();
        return Arrays.stream(classes).filter(it -> it.getName().endsWith("$ReportPart")).findFirst()
            .orElseThrow(() -> new NoSuchFieldException(
                String.format("ReportPart inner class not found in %s", reportClass.getName())
            ));
    }

    private Method findGetReportPartMethod(Class<?> reportPartClass) throws NoSuchMethodException {
        return reportPartClass.getMethod("getReportPart");
    }

    private Method findSetStateMethod(Class<?> reportPartClass) throws NoSuchMethodException {
        for (Method method : reportPartClass.getMethods()) {
            final Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length == 1 && List.class.isAssignableFrom(parameters[0])) {
                return method;
            }
        }
        throw new NoSuchMethodException(String.format("No get-states function found on report part class %s",
                reportPartClass.getName()));
    }
}
