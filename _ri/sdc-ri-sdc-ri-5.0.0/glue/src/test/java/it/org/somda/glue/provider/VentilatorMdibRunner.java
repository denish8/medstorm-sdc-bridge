package it.org.somda.glue.provider;

import com.google.common.util.concurrent.Service;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.access.ReadTransaction;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.glue.common.FallbackInstanceIdentifier;
import org.somda.sdc.glue.common.MdibXmlIo;
import org.somda.sdc.glue.common.factory.ModificationsBuilderFactory;
import org.somda.sdc.glue.provider.SdcDeviceContext;
import org.somda.sdc.glue.provider.SdcDevicePlugin;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;

public class VentilatorMdibRunner implements SdcDevicePlugin {
    public static final String HANDLE_MDC_DEV_SYS_PT_VENT_MDS = "handle_MDC_DEV_SYS_PT_VENT_MDS";
    public static final String HANDLE_CLOCK = "handle_Clock";
    public static final String HANDLE_SYSTEMCONTEXT = "handle_SystemContext";
    public static final String HANDLE_LOCATIONCONTEXT = "handle_LocationContext";
    public static final String HANDLE_MDC_DEV_SYS_PT_VENT_VMD = "handle_MDC_DEV_SYS_PT_VENT_VMD";
    public static final String HANDLE_MDC_DEV_SYS_PT_VENT_CHAN = "handle_MDC_DEV_SYS_PT_VENT_CHAN";
    public static final String HANDLE_MDC_VENT_MODE = "handle_MDC_VENT_MODE";
    public static final String HANDLE_MDC_PRESS_AWAY_END_EXP_POS = "handle_MDC_PRESS_AWAY_END_EXP_POS";
    public static final String HANDLE_BAD_MDC_DEV_SYS_PT_VENT_VMD = "handle_bad_MDC_DEV_SYS_PT_VENT_VMD";
    public static final String HANDLE_VIS_BAD_MDC_DEV_SYS_PT_VENT_VMD = "handle_vis_bad_MDC_DEV_SYS_PT_VENT_VMD";
    public static final String HANDLE_SET_MDC_DEV_SYS_PT_VENT_VMD = "handle_set_MDC_DEV_SYS_PT_VENT_VMD";
    public static final String HANDLE_ALERT_SYSTEM = "handle_AlertSystem";
    public static final String HANDLE_SCO = "handle_Sco";

    private final MdibXmlIo mdibXmlIo;
    private final ModificationsBuilderFactory modificationsBuilderFactory;
    private LocalMdibAccess mdibAccess;
    private SdcDeviceContext sdcDeviceContext;

    private int locationContextHandleSuffixCounter;

    public VentilatorMdibRunner(MdibXmlIo mdibXmlIo,
                                ModificationsBuilderFactory modificationsBuilderFactory) {
        this.mdibXmlIo = mdibXmlIo;
        this.modificationsBuilderFactory = modificationsBuilderFactory;
        this.locationContextHandleSuffixCounter = 0;
    }

    @Override
    public void beforeStartUp(SdcDeviceContext context) throws Exception {
        sdcDeviceContext = context;
        mdibAccess = context.getLocalMdibAccess();

        var classLoader = getClass().getClassLoader();
        var mdibAsStream = classLoader.getResourceAsStream("it/org/somda/sdc/glue/VentilatorMdib.xml");
        assert mdibAsStream != null;
        var mdib = mdibXmlIo.readMdib(mdibAsStream);
        var modifications = modificationsBuilderFactory.createModificationsBuilder(mdib).get();

        mdibAccess.writeDescription(modifications);
    }

    public void changeLocation(LocationDetail newLocation) throws PreprocessingException {
        final InstanceIdentifier identifier = FallbackInstanceIdentifier.create(newLocation).orElseThrow(() ->
                new IllegalStateException(String.format("Could not create fallback instance identifier from location %s",
                        newLocation.toString())));

        final MdibStateModifications modifications = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT);
        for (AbstractContextState contextState : mdibAccess.getContextStates(HANDLE_LOCATIONCONTEXT)) {
            if (contextState.getContextAssociation().equals(ContextAssociation.ASSOC)) {
                contextState.setContextAssociation(ContextAssociation.DIS);
                modifications.add(contextState);
            }
        }
        mdibAccess.writeStates(modifications);

        modifications.clear();
        for (AbstractContextState contextState : mdibAccess.getContextStates(HANDLE_LOCATIONCONTEXT)) {
            if (contextState.getContextAssociation().equals(ContextAssociation.DIS)) {
                contextState.setContextAssociation(ContextAssociation.NO);
            }
            modifications.add(contextState);
        }

        LocationContextState locationContextState = new LocationContextState();
        locationContextState.setContextAssociation(ContextAssociation.ASSOC);
        locationContextState.setLocationDetail(newLocation);
        locationContextState.getIdentification().add(identifier);
        locationContextState.setDescriptorHandle(HANDLE_LOCATIONCONTEXT);
        locationContextState.setHandle(HANDLE_LOCATIONCONTEXT + "_state" + locationContextHandleSuffixCounter++);
        InstanceIdentifier validator = new InstanceIdentifier();
        validator.setRootName("urn:validator:demo");
        locationContextState.getValidator().add(validator);
        modifications.add(locationContextState);
        mdibAccess.writeStates(modifications);
    }

    public void changeAlertsPresence(@Nullable Boolean ventilatorModeAlarm) throws PreprocessingException {
        final MdibStateModifications modifications = MdibStateModifications.create(MdibStateModifications.Type.ALERT);
        try (ReadTransaction readTransaction = mdibAccess.startTransaction()) {
            if (ventilatorModeAlarm != null) {
                changeVentilatorModeAlarm(readTransaction, modifications, ventilatorModeAlarm);
            }
        }
        mdibAccess.writeStates(modifications);
    }

    private void changeVentilatorModeAlarm(ReadTransaction readTransaction, MdibStateModifications modifications, Boolean ventilatorModeAlarm) {
        final AlertConditionState conditionState = readTransaction.getState(HANDLE_BAD_MDC_DEV_SYS_PT_VENT_VMD, AlertConditionState.class)
                .orElseThrow(() ->
                        new RuntimeException(String.format("Could not find state for handle %s", HANDLE_BAD_MDC_DEV_SYS_PT_VENT_VMD)));
        final AlertSignalState signalState = readTransaction.getState(HANDLE_VIS_BAD_MDC_DEV_SYS_PT_VENT_VMD, AlertSignalState.class)
                .orElseThrow(() ->
                        new RuntimeException(String.format("Could not find state for handle %s", HANDLE_VIS_BAD_MDC_DEV_SYS_PT_VENT_VMD)));

        conditionState.setPresence(ventilatorModeAlarm);
        conditionState.setDeterminationTime(Instant.now());
        if (ventilatorModeAlarm) {
            signalState.setPresence(AlertSignalPresence.ON);
        } else {
            signalState.setPresence(AlertSignalPresence.OFF);
        }

        modifications.addAll(Arrays.asList(conditionState, signalState));
    }

    public void changeMetrics(@Nullable VentilatorMode ventilatorMode,
                              @Nullable BigDecimal peep) throws PreprocessingException {
        if (!(isRunning())) {
            return;
        }

        final MdibStateModifications modifications = MdibStateModifications.create(MdibStateModifications.Type.METRIC);

        try (ReadTransaction readTransaction = mdibAccess.startTransaction()) {
            if (ventilatorMode != null) {
                changeVentilatorModeValue(readTransaction, modifications, ventilatorMode);
            }
            if (peep != null) {
                changePeepValue(readTransaction, modifications, peep);
            }
        }

        mdibAccess.writeStates(modifications);
    }

    private boolean isRunning() {
        return sdcDeviceContext != null && sdcDeviceContext.getServiceState().equals(Service.State.RUNNING);
    }

    private void changeVentilatorModeValue(ReadTransaction readTransaction, MdibStateModifications modifications, VentilatorMode ventilatorMode) {
        final EnumStringMetricState state = readTransaction.getState(HANDLE_MDC_VENT_MODE, EnumStringMetricState.class)
                .orElseThrow(() ->
                        new RuntimeException(String.format("Could not find state for handle %s", HANDLE_MDC_VENT_MODE)));

        final AbstractMetricValue.MetricQuality metricQuality = new AbstractMetricValue.MetricQuality();
        metricQuality.setMode(GenerationMode.DEMO);
        metricQuality.setValidity(MeasurementValidity.INV);
        StringMetricValue metricValue = state.getMetricValue();
        if (metricValue == null) {
            metricValue = new StringMetricValue();
        }
        metricValue.setValue(ventilatorMode.getModeValue());
        metricValue.setDeterminationTime(Instant.now());
        metricValue.setMetricQuality(metricQuality);
        state.setMetricValue(metricValue);
        modifications.add(state);
    }

    public static void changePeepValue(ReadTransaction readTransaction, MdibStateModifications modifications, BigDecimal peep) {
        final NumericMetricState state = readTransaction.getState(HANDLE_MDC_PRESS_AWAY_END_EXP_POS, NumericMetricState.class)
                .orElseThrow(() ->
                        new RuntimeException(String.format("Could not find state for handle %s", HANDLE_MDC_VENT_MODE)));

        final AbstractMetricValue.MetricQuality metricQuality = new AbstractMetricValue.MetricQuality();
        metricQuality.setMode(GenerationMode.DEMO);
        metricQuality.setValidity(MeasurementValidity.INV);
        NumericMetricValue metricValue = state.getMetricValue();
        if (metricValue == null) {
            metricValue = new NumericMetricValue();
        }
        metricValue.setValue(peep);
        metricValue.setDeterminationTime(Instant.now());
        metricValue.setMetricQuality(metricQuality);
        state.setMetricValue(metricValue);
        modifications.add(state);
    }

    public enum VentilatorMode {
        VENT_MODE_CPAP("vent-mode-cpap"),
        VENT_MODE_SIMV("vent-mode-simv"),
        VENT_MODE_INSPASSIST("vent-mode-inspassist");

        private String modeValue;

        VentilatorMode(String modeValue) {
            this.modeValue = modeValue;
        }

        public String getModeValue() {
            return modeValue;
        }
    }
}
