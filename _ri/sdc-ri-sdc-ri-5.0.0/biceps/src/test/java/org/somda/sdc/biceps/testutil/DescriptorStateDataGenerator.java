package org.somda.sdc.biceps.testutil;

import org.somda.sdc.biceps.model.message.RetrievabilityMethod;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.common.model.AnyDateTime;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

public class DescriptorStateDataGenerator {
    private final ObjectFactory participantFactory;
    private final org.somda.sdc.biceps.model.extension.ObjectFactory extensionFactory;
    private final BaseTypeDataGenerator baseTypes;

    public DescriptorStateDataGenerator() {
        this.participantFactory = new ObjectFactory();
        this.extensionFactory = new org.somda.sdc.biceps.model.extension.ObjectFactory();
        this.baseTypes = new BaseTypeDataGenerator();
    }

    public MdsDescriptor mdsDescriptor(String handle) {
        final MdsDescriptor mdsDescriptor = participantFactory.createMdsDescriptor();
        descriptor(mdsDescriptor, handle);
        deviceComponentDescriptor(mdsDescriptor);

        mdsDescriptor.setApprovedJurisdictions(baseTypes.approvedJurisdictions());
        final MdsDescriptor.MetaData mdsDescriptorMetaData = participantFactory.createMdsDescriptorMetaData();
        mdsDescriptorMetaData.setExpirationDate(AnyDateTime.create(baseTypes.localDateTime()));
        mdsDescriptorMetaData.setLotNumber("lot-number");
        mdsDescriptorMetaData.setManufactureDate(AnyDateTime.create(baseTypes.localDateTime()));
        mdsDescriptorMetaData.setManufacturer(baseTypes.localizedTexts());
        mdsDescriptorMetaData.setModelName(baseTypes.localizedTexts());
        mdsDescriptorMetaData.setModelNumber("model-number");
        mdsDescriptorMetaData.setSerialNumber(Arrays.asList("serial-number1", "serial-number2"));
        final MdsDescriptor.MetaData.Udi udi = new MdsDescriptor.MetaData.Udi();
        udi.setDeviceIdentifier("udi-device-identifier");
        udi.setHumanReadableForm("udi-human-readable-form");
        udi.setJurisdiction(baseTypes.instanceIdentifier("udi-jurisdiction"));
        udi.setIssuer(baseTypes.instanceIdentifier("udi-issuer"));
        mdsDescriptorMetaData.setUdi(Arrays.asList(udi));

        return mdsDescriptor;
    }

    public MdsState mdsState() {
        final MdsState mdsState = participantFactory.createMdsState();
        mdsState.setLang("en");
        mdsState.setOperatingJurisdiction(baseTypes.instanceIdentifier("operating-jurisdiction", OperatingJurisdiction.class));
        mdsState.setOperatingMode(MdsOperatingMode.DMO);
        deviceComponentState(mdsState);

        return mdsState;
    }

    public ClockDescriptor clockDescriptor(String handle) {
        final ClockDescriptor clockDescriptor = participantFactory.createClockDescriptor();
        descriptor(clockDescriptor, handle);
        deviceComponentDescriptor(clockDescriptor);
        clockDescriptor.setResolution(Duration.ofMillis(1));
        clockDescriptor.setTimeProtocol(Arrays.asList(baseTypes.codedValue("time-protocol1"), baseTypes.codedValue("time-protocol2")));
        return clockDescriptor;
    }

    public ClockState clockState() {
        final ClockState clockState = participantFactory.createClockState();
        deviceComponentState(clockState);
        clockState.setCriticalUse(false);
        clockState.setActiveSyncProtocol(baseTypes.codedValue("time-protocol1"));
        clockState.setAccuracy(BigDecimal.ONE);
        clockState.setDateAndTime(Instant.ofEpochMilli(1580152377910L));
        clockState.setLastSet(Instant.ofEpochMilli(1580152377910L).minus(Duration.ofHours(5)));
        clockState.setReferenceSource(Arrays.asList("0.de.pool.ntp.org"));
        clockState.setRemoteSync(true);
        clockState.setTimeZone("CST6CDT,M3.2.0/2:00:00,M11.1.0/2:00:00");
        return clockState;
    }

    public BatteryDescriptor batteryDescriptor(String handle) {
        final BatteryDescriptor batteryDescriptor = participantFactory.createBatteryDescriptor();
        descriptor(batteryDescriptor, handle);
        deviceComponentDescriptor(batteryDescriptor);
        batteryDescriptor.setCapacityFullCharge(baseTypes.measurement(BigDecimal.valueOf(100)));
        batteryDescriptor.setCapacitySpecified(baseTypes.measurement(BigDecimal.valueOf(120)));
        batteryDescriptor.setVoltageSpecified(baseTypes.measurement(BigDecimal.TEN));
        return batteryDescriptor;
    }

    public BatteryState batteryState() {
        final BatteryState batteryState = participantFactory.createBatteryState();
        deviceComponentState(batteryState);
        batteryState.setCapacityRemaining(baseTypes.measurement(BigDecimal.valueOf(80)));
        batteryState.setChargeCycles(Long.valueOf(100));
        batteryState.setChargeStatus(BatteryState.ChargeStatus.CH_B);
        batteryState.setCurrent(baseTypes.measurement(BigDecimal.TEN));
        batteryState.setTemperature(baseTypes.measurement(BigDecimal.valueOf(45)));
        batteryState.setVoltage(baseTypes.measurement(BigDecimal.valueOf(8)));
        batteryState.setRemainingBatteryTime(baseTypes.measurement(BigDecimal.ONE));
        return batteryState;
    }

    public SystemContextDescriptor systemContextDescriptor(String handle) {
        final SystemContextDescriptor systemContextDescriptor = participantFactory.createSystemContextDescriptor();
        descriptor(systemContextDescriptor, handle);
        deviceComponentDescriptor(systemContextDescriptor);
        return systemContextDescriptor;
    }

    public SystemContextState systemContextState() {
        final SystemContextState systemContextState = participantFactory.createSystemContextState();
        deviceComponentState(systemContextState);
        return systemContextState;
    }

    public PatientContextDescriptor patientContextDescriptor(String handle) {
        final PatientContextDescriptor patientContextDescriptor = participantFactory.createPatientContextDescriptor();
        descriptor(patientContextDescriptor, handle);
        return patientContextDescriptor;
    }

    public PatientContextState patientContextState(String handle) {
        final PatientContextState patientContextState = participantFactory.createPatientContextState();
        contextState(patientContextState, handle);
        patientContextState.setCoreData(baseTypes.patientDemographicsCoreData());
        return patientContextState;
    }

    public LocationContextDescriptor locationContextDescriptor(String handle) {
        final LocationContextDescriptor locationContextDescriptor = participantFactory.createLocationContextDescriptor();
        descriptor(locationContextDescriptor, handle);
        return locationContextDescriptor;
    }

    public LocationContextState locationContextState(String handle) {
        final LocationContextState locationContextState = participantFactory.createLocationContextState();
        contextState(locationContextState, handle);
        locationContextState.setLocationDetail(baseTypes.locationDetail());
        return locationContextState;
    }

    public EnsembleContextDescriptor ensembleContextDescriptor(String handle) {
        final EnsembleContextDescriptor ensembleContextDescriptor = participantFactory.createEnsembleContextDescriptor();
        descriptor(ensembleContextDescriptor, handle);
        return ensembleContextDescriptor;
    }

    public EnsembleContextState ensembleContextState(String handle) {
        final EnsembleContextState ensembleContextState = participantFactory.createEnsembleContextState();
        contextState(ensembleContextState, handle);
        return ensembleContextState;
    }

    public AlertSystemDescriptor alertSystemDescriptor(String handle) {
        final AlertSystemDescriptor alertSystemDescriptor = participantFactory.createAlertSystemDescriptor();
        descriptor(alertSystemDescriptor, handle);
        alertSystemDescriptor.setMaxPhysiologicalParallelAlarms(Long.valueOf(10));
        alertSystemDescriptor.setMaxTechnicalParallelAlarms(Long.valueOf(5));
        alertSystemDescriptor.setSelfCheckPeriod(Duration.ofMillis(5000));
        return alertSystemDescriptor;
    }

    public AlertSystemState alertSystemState() {
        AlertSystemState alertSystemState = participantFactory.createAlertSystemState();
        alertState(alertSystemState);
        alertSystemState.setLastSelfCheck(Instant.ofEpochMilli(1580152377910L).minus(Duration.ofHours(2)));
        alertSystemState.setSelfCheckCount(Long.valueOf(1234));
        alertSystemState.setSystemSignalActivation(Arrays.asList(baseTypes.systemSignalActivation(AlertSignalManifestation.AUD),
                baseTypes.systemSignalActivation(AlertSignalManifestation.VIS)));
        return alertSystemState;
    }

    public AlertConditionDescriptor alertConditionDescriptor(String handle, String sourceHandle) {
        final AlertConditionDescriptor alertConditionDescriptor = participantFactory.createAlertConditionDescriptor();
        descriptor(alertConditionDescriptor, handle);
        alertConditionDescriptor.setCanDeescalate(AlertConditionPriority.NONE);
        alertConditionDescriptor.setCanEscalate(AlertConditionPriority.HI);
        alertConditionDescriptor.setCauseInfo(Arrays.asList(baseTypes.causeInfo()));
        alertConditionDescriptor.setDefaultConditionGenerationDelay(Duration.ofMillis(100));
        alertConditionDescriptor.setKind(AlertConditionKind.TEC);
        alertConditionDescriptor.setPriority(AlertConditionPriority.ME);
        alertConditionDescriptor.setSource(Arrays.asList(sourceHandle));
        return alertConditionDescriptor;
    }

    public AlertConditionState alertConditionState() {
        final AlertConditionState alertConditionState = participantFactory.createAlertConditionState();
        alertState(alertConditionState);
        alertConditionState.setActualConditionGenerationDelay(Duration.ofMillis(50));
        alertConditionState.setActualPriority(AlertConditionPriority.ME);
        alertConditionState.setDeterminationTime(Instant.ofEpochMilli(1580152377910L));
        alertConditionState.setPresence(false);
        alertConditionState.setRank(5);
        return alertConditionState;
    }

    public LimitAlertConditionDescriptor limitAlertConditionDescriptor(String handle, String sourceHandle) {
        LimitAlertConditionDescriptor limitAlertConditionDescriptor = participantFactory.createLimitAlertConditionDescriptor();
        descriptor(limitAlertConditionDescriptor, handle);
        limitAlertConditionDescriptor.setCanDeescalate(AlertConditionPriority.ME);
        limitAlertConditionDescriptor.setCanEscalate(AlertConditionPriority.HI);
        limitAlertConditionDescriptor.setCauseInfo(Arrays.asList(baseTypes.causeInfo()));
        limitAlertConditionDescriptor.setDefaultConditionGenerationDelay(Duration.ofMillis(10));
        limitAlertConditionDescriptor.setKind(AlertConditionKind.PHY);
        limitAlertConditionDescriptor.setPriority(AlertConditionPriority.HI);
        limitAlertConditionDescriptor.setSource(Arrays.asList(sourceHandle));
        limitAlertConditionDescriptor.setAutoLimitSupported(false);
        limitAlertConditionDescriptor.setMaxLimits(baseTypes.range());
        return limitAlertConditionDescriptor;
    }

    public LimitAlertConditionState limitAlertConditionState() {
        final LimitAlertConditionState limitAlertConditionState = participantFactory.createLimitAlertConditionState();
        alertState(limitAlertConditionState);
        limitAlertConditionState.setActualConditionGenerationDelay(Duration.ofMillis(10));
        limitAlertConditionState.setActualPriority(AlertConditionPriority.NONE);
        limitAlertConditionState.setDeterminationTime(Instant.ofEpochMilli(1580152377910L));
        limitAlertConditionState.setPresence(false);
        limitAlertConditionState.setRank(3);
        limitAlertConditionState.setAutoLimitActivationState(AlertActivation.PSD);
        limitAlertConditionState.setLimits(baseTypes.range());
        limitAlertConditionState.setMonitoredAlertLimits(AlertConditionMonitoredLimits.ALL);
        return limitAlertConditionState;
    }

    public AlertSignalDescriptor alertSignalDescriptor(String handle, String conditionSignaledHandle, AlertSignalManifestation manifestation) {
        final AlertSignalDescriptor alertSignalDescriptor = participantFactory.createAlertSignalDescriptor();
        descriptor(alertSignalDescriptor, handle);
        alertSignalDescriptor.setAcknowledgementSupported(false);
        alertSignalDescriptor.setAcknowledgeTimeout(Duration.ofMillis(1000));
        alertSignalDescriptor.setConditionSignaled(conditionSignaledHandle);
        alertSignalDescriptor.setDefaultSignalGenerationDelay(Duration.ofMillis(500));
        alertSignalDescriptor.setLatching(true);
        alertSignalDescriptor.setManifestation(manifestation);
        alertSignalDescriptor.setMaxSignalGenerationDelay(Duration.ofMillis(100));
        alertSignalDescriptor.setMinSignalGenerationDelay(Duration.ofMillis(10));
        alertSignalDescriptor.setSignalDelegationSupported(false);
        return alertSignalDescriptor;
    }

    public AlertSignalState alertSignalState() {
        final AlertSignalState alertSignalState = participantFactory.createAlertSignalState();
        alertSignalState.setActualSignalGenerationDelay(Duration.ofMillis(30));
        alertSignalState.setLocation(AlertSignalPrimaryLocation.LOC);
        alertSignalState.setPresence(AlertSignalPresence.OFF);
        alertSignalState.setSlot(2L);
        alertSignalState.setActivationState(AlertActivation.OFF);
        return alertSignalState;
    }

    public VmdDescriptor vmdDescriptor(String handle) {
        final VmdDescriptor vmdDescriptor = participantFactory.createVmdDescriptor();
        descriptor(vmdDescriptor, handle);
        deviceComponentDescriptor(vmdDescriptor);
        vmdDescriptor.setApprovedJurisdictions(baseTypes.approvedJurisdictions());
        return vmdDescriptor;
    }

    public VmdState vmdState() {
        final VmdState vmdState = participantFactory.createVmdState();
        deviceComponentState(vmdState);
        vmdState.setOperatingJurisdiction(baseTypes.instanceIdentifier("operating-jurisdiction", OperatingJurisdiction.class));
        return vmdState;
    }

    public ChannelDescriptor channelDescriptor(String handle) {
        final ChannelDescriptor channelDescriptor = participantFactory.createChannelDescriptor();
        descriptor(channelDescriptor, handle);
        deviceComponentDescriptor(channelDescriptor);
        return channelDescriptor;
    }

    public ChannelState channelState() {
        final ChannelState channelState = participantFactory.createChannelState();
        deviceComponentState(channelState);
        return channelState;
    }

    public NumericMetricDescriptor numericMetricDescriptor(String handle) {
        final NumericMetricDescriptor numericMetricDescriptor = participantFactory.createNumericMetricDescriptor();
        descriptor(numericMetricDescriptor, handle);
        metricDescriptor(numericMetricDescriptor);
        numericMetricDescriptor.setAveragingPeriod(Duration.ofMillis(10000));
        numericMetricDescriptor.setResolution(BigDecimal.ONE);
        numericMetricDescriptor.setTechnicalRange(baseTypes.ranges());
        return numericMetricDescriptor;
    }

    public NumericMetricState numericMetricState() {
        final NumericMetricState numericMetricState = participantFactory.createNumericMetricState();
        metricState(numericMetricState);
        numericMetricState.setActiveAveragingPeriod(Duration.ofMillis(15000));
        numericMetricState.setPhysiologicalRange(baseTypes.ranges());
        NumericMetricValue numericMetricValue = participantFactory.createNumericMetricValue();
        metricValue(numericMetricValue);
        numericMetricValue.setValue(BigDecimal.TEN);
        numericMetricState.setMetricValue(numericMetricValue);
        return numericMetricState;
    }

    public StringMetricDescriptor stringMetricDescriptor(String handle) {
        final StringMetricDescriptor stringMetricDescriptor = participantFactory.createStringMetricDescriptor();
        descriptor(stringMetricDescriptor, handle);
        metricDescriptor(stringMetricDescriptor);
        return stringMetricDescriptor;
    }

    public StringMetricState stringMetricState() {
        final StringMetricState stringMetricState = participantFactory.createStringMetricState();
        metricState(stringMetricState);
        StringMetricValue stringMetricValue = participantFactory.createStringMetricValue();
        metricValue(stringMetricValue);
        stringMetricValue.setValue("string-metric-value");
        stringMetricState.setMetricValue(stringMetricValue);
        return stringMetricState;
    }

    public EnumStringMetricDescriptor enumStringMetricDescriptor(String handle) {
        final EnumStringMetricDescriptor enumStringMetricDescriptor = participantFactory.createEnumStringMetricDescriptor();
        descriptor(enumStringMetricDescriptor, handle);
        metricDescriptor(enumStringMetricDescriptor);
        enumStringMetricDescriptor.setAllowedValue(Arrays.asList(baseTypes.allowedValue("enum-value1"), baseTypes.allowedValue("enum-value2")));
        return enumStringMetricDescriptor;
    }

    public EnumStringMetricState enumStringMetricState() {
        final EnumStringMetricState enumStringMetricState = participantFactory.createEnumStringMetricState();
        metricState(enumStringMetricState);
        StringMetricValue stringMetricValue = participantFactory.createStringMetricValue();
        metricValue(stringMetricValue);
        stringMetricValue.setValue("enum-value1");
        enumStringMetricState.setMetricValue(stringMetricValue);
        return enumStringMetricState;
    }

    public RealTimeSampleArrayMetricDescriptor realTimeSampleArrayMetricDescriptor(String handle) {
        final RealTimeSampleArrayMetricDescriptor realTimeSampleArrayMetricDescriptor = participantFactory.createRealTimeSampleArrayMetricDescriptor();
        descriptor(realTimeSampleArrayMetricDescriptor, handle);
        metricDescriptor(realTimeSampleArrayMetricDescriptor);
        realTimeSampleArrayMetricDescriptor.setResolution(BigDecimal.ONE);
        realTimeSampleArrayMetricDescriptor.setSamplePeriod(Duration.ofMillis(10));
        realTimeSampleArrayMetricDescriptor.setTechnicalRange(baseTypes.ranges());
        return realTimeSampleArrayMetricDescriptor;
    }

    public RealTimeSampleArrayMetricState realTimeSampleArrayMetricState() {
        final RealTimeSampleArrayMetricState realTimeSampleArrayMetricState = participantFactory.createRealTimeSampleArrayMetricState();
        metricState(realTimeSampleArrayMetricState);
        SampleArrayValue sampleArrayValue = participantFactory.createSampleArrayValue();
        metricValue(sampleArrayValue);
        sampleArrayValue.setSamples(Arrays.asList(BigDecimal.valueOf(1), BigDecimal.valueOf(2), BigDecimal.valueOf(3)));
        sampleArrayValue.setApplyAnnotation(baseTypes.applyAnnotations());
        realTimeSampleArrayMetricState.setMetricValue(sampleArrayValue);
        return realTimeSampleArrayMetricState;
    }

    public DistributionSampleArrayMetricDescriptor distributionSampleArrayMetricDescriptor(String handle) {
        final DistributionSampleArrayMetricDescriptor distributionSampleArrayMetricDescriptor = participantFactory.createDistributionSampleArrayMetricDescriptor();
        descriptor(distributionSampleArrayMetricDescriptor, handle);
        metricDescriptor(distributionSampleArrayMetricDescriptor);
        distributionSampleArrayMetricDescriptor.setDistributionRange(baseTypes.range());
        distributionSampleArrayMetricDescriptor.setDomainUnit(baseTypes.codedValue("domain-unit"));
        distributionSampleArrayMetricDescriptor.setResolution(BigDecimal.ONE);
        distributionSampleArrayMetricDescriptor.setTechnicalRange(baseTypes.ranges());
        return distributionSampleArrayMetricDescriptor;
    }

    public DistributionSampleArrayMetricState distributionSampleArrayMetricState() {
        final DistributionSampleArrayMetricState distributionSampleArrayMetricState = participantFactory.createDistributionSampleArrayMetricState();
        metricState(distributionSampleArrayMetricState);
        SampleArrayValue sampleArrayValue = participantFactory.createSampleArrayValue();
        metricValue(sampleArrayValue);
        sampleArrayValue.setSamples(Arrays.asList(BigDecimal.valueOf(1), BigDecimal.valueOf(2), BigDecimal.valueOf(3)));
        sampleArrayValue.setApplyAnnotation(baseTypes.applyAnnotations());
        distributionSampleArrayMetricState.setMetricValue(sampleArrayValue);
        return distributionSampleArrayMetricState;
    }

    public ScoDescriptor scoDescriptor(String handle) {
        final ScoDescriptor scoDescriptor = participantFactory.createScoDescriptor();
        descriptor(scoDescriptor, handle);
        deviceComponentDescriptor(scoDescriptor);
        return scoDescriptor;
    }

    public ScoState scoState() {
        final ScoState scoState = participantFactory.createScoState();
        deviceComponentState(scoState);
        scoState.setInvocationRequested(Arrays.asList(Handles.OPERATION_0));
        scoState.setInvocationRequired(Arrays.asList(Handles.OPERATION_1));
        scoState.setOperationGroup(Arrays.asList(baseTypes.operationGroup("operation-group")));
        return scoState;
    }

    public ActivateOperationDescriptor activateOperationDescriptor(String handle, String targetHandle) {
        final ActivateOperationDescriptor activateOperationDescriptor = participantFactory.createActivateOperationDescriptor();
        descriptor(activateOperationDescriptor, handle);
        operationDescriptor(activateOperationDescriptor, targetHandle);
        activateOperationDescriptor.setArgument(Arrays.asList(baseTypes.argument("argument1"), baseTypes.argument("argument2")));
        return activateOperationDescriptor;
    }

    public ActivateOperationState activateOperationState() {
        final ActivateOperationState activateOperationState = participantFactory.createActivateOperationState();
        operationState(activateOperationState);
        return activateOperationState;
    }

    public SetStringOperationDescriptor setStringOperationDescriptor(String handle, String targetHandle) {
        final SetStringOperationDescriptor setStringOperationDescriptor = participantFactory.createSetStringOperationDescriptor();
        descriptor(setStringOperationDescriptor, handle);
        operationDescriptor(setStringOperationDescriptor, targetHandle);
        setStringOperationDescriptor.setMaxLength(BigInteger.valueOf(255));
        return setStringOperationDescriptor;
    }

    public SetStringOperationState setStringOperationState() {
        final SetStringOperationState setStringOperationState = participantFactory.createSetStringOperationState();
        operationState(setStringOperationState);
        SetStringOperationState.AllowedValues allowedValues = participantFactory.createSetStringOperationStateAllowedValues();
        allowedValues.setValue(Arrays.asList("allowed-value1", "allowed-value2"));
        setStringOperationState.setAllowedValues(allowedValues);
        return setStringOperationState;
    }

    public SetValueOperationDescriptor setValueOperationDescriptor(String handle, String targetHandle) {
        final SetValueOperationDescriptor setValueOperationDescriptor = participantFactory.createSetValueOperationDescriptor();
        descriptor(setValueOperationDescriptor, handle);
        operationDescriptor(setValueOperationDescriptor, targetHandle);
        return setValueOperationDescriptor;
    }

    public SetValueOperationState setValueOperationState() {
        final SetValueOperationState setValueOperationState = participantFactory.createSetValueOperationState();
        operationState(setValueOperationState);
        setValueOperationState.setAllowedRange(baseTypes.ranges());
        return setValueOperationState;
    }

    public SetComponentStateOperationDescriptor setComponentStateOperationDescriptor(String handle, String targetHandle) {
        final SetComponentStateOperationDescriptor setComponentStateOperationDescriptor = participantFactory.createSetComponentStateOperationDescriptor();
        descriptor(setComponentStateOperationDescriptor, handle);
        operationDescriptor(setComponentStateOperationDescriptor, targetHandle);
        return setComponentStateOperationDescriptor;
    }

    public SetComponentStateOperationState setComponentStateOperationState() {
        final SetComponentStateOperationState setComponentStateOperationState = participantFactory.createSetComponentStateOperationState();
        operationState(setComponentStateOperationState);
        return setComponentStateOperationState;
    }

    public SetMetricStateOperationDescriptor setMetricStateOperationDescriptor(String handle, String targetHandle) {
        final SetMetricStateOperationDescriptor setMetricStateOperationDescriptor = participantFactory.createSetMetricStateOperationDescriptor();
        descriptor(setMetricStateOperationDescriptor, handle);
        operationDescriptor(setMetricStateOperationDescriptor, targetHandle);
        return setMetricStateOperationDescriptor;
    }

    public SetMetricStateOperationState setMetricStateOperationState() {
        final SetMetricStateOperationState setMetricStateOperationState = participantFactory.createSetMetricStateOperationState();
        operationState(setMetricStateOperationState);
        return setMetricStateOperationState;
    }

    public SetAlertStateOperationDescriptor setAlertStateOperationDescriptor(String handle, String targetHandle) {
        final SetAlertStateOperationDescriptor setAlertStateOperationDescriptor = participantFactory.createSetAlertStateOperationDescriptor();
        descriptor(setAlertStateOperationDescriptor, handle);
        operationDescriptor(setAlertStateOperationDescriptor, targetHandle);
        return setAlertStateOperationDescriptor;
    }

    public SetAlertStateOperationState setAlertStateOperationState() {
        final SetAlertStateOperationState setAlertStateOperationState = participantFactory.createSetAlertStateOperationState();
        operationState(setAlertStateOperationState);
        return setAlertStateOperationState;
    }

    public SetContextStateOperationDescriptor setContextStateOperationDescriptor(String handle, String targetHandle) {
        final SetContextStateOperationDescriptor setContextStateOperationDescriptor = participantFactory.createSetContextStateOperationDescriptor();
        descriptor(setContextStateOperationDescriptor, handle);
        operationDescriptor(setContextStateOperationDescriptor, targetHandle);
        return setContextStateOperationDescriptor;
    }

    public SetContextStateOperationState setContextStateOperationState() {
        final SetContextStateOperationState setContextStateOperationState = participantFactory.createSetContextStateOperationState();
        operationState(setContextStateOperationState);
        return setContextStateOperationState;
    }

    private void descriptor(AbstractDescriptor descriptor, String handle) {
        descriptor(descriptor, handle, RetrievabilityMethod.EP);
    }

    private void descriptor(AbstractDescriptor descriptor, String handle, RetrievabilityMethod retrievabilityMethod) {
        descriptor.setSafetyClassification(SafetyClassification.MED_A);
        descriptor.setHandle(handle);
        descriptor.setType(baseTypes.codedValue(handle + "-code"));
        descriptor.setExtension(extensionFactory.createExtensionType());
        descriptor.getExtension().getAny().add(baseTypes.retrievability(retrievabilityMethod));
    }

    private void deviceComponentDescriptor(AbstractDeviceComponentDescriptor descriptor) {
        descriptor.setProductionSpecification(baseTypes.productionSpecifications());
    }

    private void deviceComponentState(AbstractDeviceComponentState state) {
        state.setActivationState(ComponentActivation.ON);
        state.setCalibrationInfo(baseTypes.calibrationInfo());
        state.setNextCalibration(baseTypes.calibrationInfo());
        state.setOperatingCycles(Integer.valueOf(100));
        state.setOperatingHours(Long.valueOf(1000));
        state.setPhysicalConnector(baseTypes.physicalConnectorInfo());
    }

    private void contextState(AbstractContextState state, String handle) {
        state.setHandle(handle);
        state.setBindingStartTime(Instant.ofEpochMilli(1580152377910L));
        state.setBindingMdibVersion(BigInteger.ZERO);
        state.setContextAssociation(ContextAssociation.ASSOC);
        state.setIdentification(Arrays.asList(baseTypes.instanceIdentifier(handle + "id0"),
                baseTypes.instanceIdentifier(handle + "id1")));
        state.setValidator(Arrays.asList(baseTypes.instanceIdentifier(handle + "validator0"),
                baseTypes.instanceIdentifier(handle + "validator1")));
    }

    private void alertState(AbstractAlertState state) {
        state.setActivationState(AlertActivation.ON);
    }

    private void metricDescriptor(AbstractMetricDescriptor descriptor) {
        descriptor.setActivationDuration(Duration.ofMillis(2000));
        descriptor.setBodySite(baseTypes.codedValues("body-site"));
        descriptor.setDerivationMethod(DerivationMethod.AUTO);
        descriptor.setDeterminationPeriod(Duration.ofMillis(1000));
        descriptor.setMaxDelayTime(Duration.ofMillis(100));
        descriptor.setLifeTimePeriod(Duration.ofMillis(2000));
        descriptor.setMaxMeasurementTime(Duration.ofMillis(200));
        descriptor.setMetricCategory(MetricCategory.MSRMT);
        descriptor.setMetricAvailability(MetricAvailability.CONT);
        descriptor.setUnit(baseTypes.codedValue("unit"));
        descriptor.setRelation(Arrays.asList(baseTypes.relation(descriptor.getHandle())));
    }

    private void metricState(AbstractMetricState state) {
        state.setActivationState(ComponentActivation.ON);
        state.setActiveDeterminationPeriod(Duration.ofMillis(2000));
        state.setBodySite(baseTypes.codedValues("state-body-site"));
        state.setPhysicalConnector(baseTypes.physicalConnectorInfo());
        state.setLifeTimePeriod(Duration.ofMillis(3000));
    }

    private void metricValue(AbstractMetricValue value) {
        value.setAnnotation(baseTypes.annotations("metric-value-annotation"));
        value.setDeterminationTime(Instant.ofEpochMilli(1580152377910L));
        value.setMetricQuality(baseTypes.metricQuality());
        value.setStartTime(Instant.ofEpochMilli(1580152377910L).minus(Duration.ofSeconds(10)));
        value.setStopTime(Instant.ofEpochMilli(1580152377910L));
    }

    private void operationDescriptor(AbstractOperationDescriptor descriptor, String targetHandle) {
        descriptor.setAccessLevel(AbstractOperationDescriptor.AccessLevel.OTH);
        descriptor.setInvocationEffectiveTimeout(Duration.ofMillis(10000));
        descriptor.setMaxTimeToFinish(Duration.ofMillis(500));
        descriptor.setOperationTarget(targetHandle);
        descriptor.setRetriggerable(false);
    }

    private void operationState(AbstractOperationState state) {
        state.setOperatingMode(OperatingMode.EN);
    }
}
