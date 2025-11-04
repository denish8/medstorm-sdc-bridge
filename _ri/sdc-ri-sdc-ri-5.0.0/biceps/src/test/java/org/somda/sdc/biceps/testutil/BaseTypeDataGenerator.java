package org.somda.sdc.biceps.testutil;

import org.somda.sdc.biceps.model.message.Retrievability;
import org.somda.sdc.biceps.model.message.RetrievabilityInfo;
import org.somda.sdc.biceps.model.message.RetrievabilityMethod;
import org.somda.sdc.biceps.model.participant.*;

import javax.xml.namespace.QName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BaseTypeDataGenerator {
    private final ObjectFactory participantFactory;
    private final org.somda.sdc.biceps.model.message.ObjectFactory messageFactory;

    public BaseTypeDataGenerator() {
        this.participantFactory = new ObjectFactory();
        this.messageFactory = new org.somda.sdc.biceps.model.message.ObjectFactory();
    }

    public ApprovedJurisdictions approvedJurisdictions() {
        final ApprovedJurisdictions approvedJurisdictions = participantFactory.createApprovedJurisdictions();
        approvedJurisdictions.getApprovedJurisdiction().addAll(
                Arrays.asList(instanceIdentifier("approved-jurisdiction1"),
                        instanceIdentifier("approved-jurisdiction2")));
        return approvedJurisdictions;
    }

    public InstanceIdentifier instanceIdentifier(String extension) {
        final String root = "http://test-root";
        InstanceIdentifier instanceIdentifier = participantFactory.createInstanceIdentifier();
        instanceIdentifier.setRootName(root);
        instanceIdentifier.setExtensionName(extension);
        return instanceIdentifier;
    }

    public <T extends InstanceIdentifier> T instanceIdentifier(String extension, Class<T> type) {
        try {
            final String root = "http://test-root";
            T instanceIdentifier = type.getConstructor().newInstance();
            instanceIdentifier.setRootName(root);
            instanceIdentifier.setExtensionName(extension);
            return instanceIdentifier;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public LocalDateTime localDateTime() {
        final String isoDateTime = "2009-05-07T13:05:45.678-04:00";
        return LocalDateTime.parse(isoDateTime, DateTimeFormatter.ISO_DATE_TIME);
    }

    public List<LocalizedText> localizedTexts() {
        final LocalizedText localizedTextEn = participantFactory.createLocalizedText();
        final LocalizedText localizedTextDe = participantFactory.createLocalizedText();

        localizedTextEn.setLang("en");
        localizedTextEn.setTextWidth(LocalizedTextWidth.M);
        localizedTextEn.setValue("This is a sample LocalizedText with text width M");
        localizedTextDe.setLang("de");
        localizedTextDe.setTextWidth(LocalizedTextWidth.L);
        localizedTextDe.setValue("Dies ist ein Beispiel-LocalizedText mit Textbreite L");

        return Arrays.asList(localizedTextEn, localizedTextDe);
    }

    public List<AbstractDeviceComponentDescriptor.ProductionSpecification> productionSpecifications() {
        final AbstractDeviceComponentDescriptor.ProductionSpecification ps1 = participantFactory.createAbstractDeviceComponentDescriptorProductionSpecification();
        final AbstractDeviceComponentDescriptor.ProductionSpecification ps2 = participantFactory.createAbstractDeviceComponentDescriptorProductionSpecification();

        ps1.setComponentId(instanceIdentifier("component-id1"));
        ps1.setProductionSpec("production-specification1");
        ps1.setSpecType(codedValue("production-specification1"));

        ps2.setComponentId(instanceIdentifier("component-id2"));
        ps2.setProductionSpec("production-specification2");
        ps2.setSpecType(codedValue("production-specification2"));

        return Arrays.asList(ps1, ps2);
    }

    public CodedValue codedValue(String codeId) {
        final CodedValue codedValue = participantFactory.createCodedValue();
        codedValue.setCode(codeId);
        codedValue.setCodingSystem("http://test-coding-system");
        codedValue.setCodingSystemVersion("2019");
        codedValue.setCodingSystemName(localizedTexts());
        codedValue.setSymbolicCodeName("SYMBOLIC_NAME_" + codeId.toUpperCase());
        codedValue.setConceptDescription(localizedTexts());
        final CodedValue.Translation translation1 = participantFactory.createCodedValueTranslation();
        translation1.setCode(codeId + "-translation1");
        translation1.setCodingSystem("http://test-coding-system-translation1");
        translation1.setCodingSystemVersion("2018");
        final CodedValue.Translation translation2 = participantFactory.createCodedValueTranslation();
        translation2.setCode(codeId + "-translation2");
        translation2.setCodingSystem("http://test-coding-system-translation2");
        translation2.setCodingSystemVersion("2017");
        codedValue.setTranslation(Arrays.asList(translation1, translation2));
        return codedValue;
    }

    public List<CodedValue> codedValues(String codeId) {
        return Arrays.asList(codedValue(codeId + "1"), codedValue(codeId + "2"));
    }

    public CalibrationInfo calibrationInfo() {
        final CalibrationInfo calibrationInfo = participantFactory.createCalibrationInfo();

        final CalibrationInfo.CalibrationDocumentation calibrationDocumentation = participantFactory.createCalibrationInfoCalibrationDocumentation();
        calibrationDocumentation.setDocumentation(localizedTexts());

        final CalibrationInfo.CalibrationDocumentation.CalibrationResult calibrationResult1 = participantFactory.createCalibrationInfoCalibrationDocumentationCalibrationResult();
        calibrationResult1.setCode(codedValue("calibration-result1"));
        calibrationResult1.setValue(measurement(BigDecimal.ONE));
        final CalibrationInfo.CalibrationDocumentation.CalibrationResult calibrationResult2 = participantFactory.createCalibrationInfoCalibrationDocumentationCalibrationResult();
        calibrationResult2.setCode(codedValue("calibration-result2"));
        calibrationResult2.setValue(measurement(BigDecimal.TEN));

        calibrationDocumentation.setCalibrationResult(Arrays.asList(calibrationResult1, calibrationResult2));
        calibrationInfo.setCalibrationDocumentation(Arrays.asList(calibrationDocumentation));
        return calibrationInfo;
    }

    public Measurement measurement(BigDecimal value) {
        final Measurement measurement = participantFactory.createMeasurement();
        measurement.setMeasuredValue(value);
        measurement.setMeasurementUnit(codedValue("measurement"));
        return measurement;
    }

    public PhysicalConnectorInfo physicalConnectorInfo() {
        final PhysicalConnectorInfo physicalConnectorInfo = participantFactory.createPhysicalConnectorInfo();
        physicalConnectorInfo.setLabel(localizedTexts());
        physicalConnectorInfo.setNumber(Integer.valueOf(7));
        return physicalConnectorInfo;
    }

    public PatientDemographicsCoreData patientDemographicsCoreData() {
        final PatientDemographicsCoreData patientDemographicsCoreData = participantFactory.createPatientDemographicsCoreData();
        patientDemographicsCoreData.setDateOfBirth("1984-12-23");
        patientDemographicsCoreData.setHeight(measurement(BigDecimal.valueOf(180)));
        patientDemographicsCoreData.setPatientType(PatientType.AD);
        patientDemographicsCoreData.setRace(codedValue("race"));
        patientDemographicsCoreData.setBirthname("Birthname");
        patientDemographicsCoreData.setFamilyname("Familyname");
        patientDemographicsCoreData.setGivenname("Givenname");
        patientDemographicsCoreData.setMiddlename(Arrays.asList("Middlename"));
        patientDemographicsCoreData.setSex(Sex.M);
        patientDemographicsCoreData.setWeight(measurement(BigDecimal.valueOf(80)));
        patientDemographicsCoreData.setTitle("PhD");
        return patientDemographicsCoreData;
    }

    public LocationDetail locationDetail() {
        final LocationDetail locationDetail = participantFactory.createLocationDetail();
        locationDetail.setBed("bed1");
        locationDetail.setBuilding("building1");
        locationDetail.setFacility("facility1");
        locationDetail.setFloor("floor1");
        locationDetail.setPoC("poc1");
        locationDetail.setRoom("room1");
        return locationDetail;
    }

    public SystemSignalActivation systemSignalActivation(AlertSignalManifestation manifestation) {
        SystemSignalActivation systemSignalActivation = participantFactory.createSystemSignalActivation();
        systemSignalActivation.setManifestation(manifestation);
        systemSignalActivation.setState(AlertActivation.ON);
        return systemSignalActivation;
    }

    public CauseInfo causeInfo() {
        final CauseInfo causeInfo = participantFactory.createCauseInfo();
        causeInfo.setDescription(localizedTexts());
        causeInfo.setRemedyInfo(remedyInfo());
        return causeInfo;
    }

    public RemedyInfo remedyInfo() {
        final RemedyInfo remedyInfo = participantFactory.createRemedyInfo();
        remedyInfo.setDescription(localizedTexts());
        return remedyInfo;
    }

    public Range range() {
        return range(BigDecimal.ZERO, BigDecimal.TEN);
    }

    public Range range(BigDecimal lower, BigDecimal upper) {
        final Range range = participantFactory.createRange();
        range.setAbsoluteAccuracy(BigDecimal.ONE);
        range.setRelativeAccuracy(BigDecimal.ONE);
        range.setLower(lower);
        range.setUpper(upper);
        range.setStepWidth(BigDecimal.ONE);
        return range;
    }

    public AbstractMetricDescriptor.Relation relation(String handle) {
        AbstractMetricDescriptor.Relation relation = participantFactory.createAbstractMetricDescriptorRelation();
        relation.setCode(codedValue(handle));
        relation.setIdentification(instanceIdentifier(handle));
        relation.setKind(AbstractMetricDescriptor.Relation.Kind.OTH);
        relation.setEntries(Arrays.asList(handle));
        return relation;
    }

    public List<Range> ranges() {
        return Arrays.asList(range(BigDecimal.ZERO, BigDecimal.TEN), range(BigDecimal.valueOf(20), BigDecimal.valueOf(100)));
    }

    public AbstractMetricValue.Annotation annotation(String codeId) {
        final AbstractMetricValue.Annotation annotation = participantFactory.createAbstractMetricValueAnnotation();
        annotation.setType(codedValue(codeId));
        return annotation;
    }

    public List<AbstractMetricValue.Annotation> annotations(String codeId) {
        return Arrays.asList(annotation(codeId + "1"), annotation(codeId + "2"));
    }

    public AbstractMetricValue.MetricQuality metricQuality() {
        final AbstractMetricValue.MetricQuality metricQuality = participantFactory.createAbstractMetricValueMetricQuality();
        metricQuality.setMode(GenerationMode.DEMO);
        metricQuality.setQi(BigDecimal.ONE);
        metricQuality.setValidity(MeasurementValidity.VLD);
        return metricQuality;
    }

    public EnumStringMetricDescriptor.AllowedValue allowedValue(String value) {
        final EnumStringMetricDescriptor.AllowedValue allowedValue = participantFactory.createEnumStringMetricDescriptorAllowedValue();
        allowedValue.setCharacteristic(measurement(BigDecimal.ONE));
        allowedValue.setIdentification(instanceIdentifier(value + "-identifier"));
        allowedValue.setType(codedValue(value + "-code"));
        allowedValue.setValue(value);
        return allowedValue;
    }

    public List<SampleArrayValue.ApplyAnnotation> applyAnnotations() {
        final SampleArrayValue.ApplyAnnotation applyAnnotation = participantFactory.createSampleArrayValueApplyAnnotation();
        applyAnnotation.setAnnotationIndex(1);
        applyAnnotation.setSampleIndex(1);
        return Arrays.asList(applyAnnotation);
    }

    public ScoState.OperationGroup operationGroup(String code) {
        ScoState.OperationGroup operationGroup = participantFactory.createScoStateOperationGroup();
        operationGroup.setOperatingMode(OperatingMode.EN);
        operationGroup.setOperations(Arrays.asList(Handles.OPERATION_0, Handles.OPERATION_1));
        operationGroup.setType(codedValue(code));
        return operationGroup;
    }

    public ActivateOperationDescriptor.Argument argument(String code) {
        ActivateOperationDescriptor.Argument argument = participantFactory.createActivateOperationDescriptorArgument();
        argument.setArg(new QName("http://argument-uri", "a-type", "a"));
        argument.setArgName(codedValue(code));
        return argument;
    }

    public Retrievability retrievability(RetrievabilityMethod method) {
        Retrievability retrievability = messageFactory.createRetrievability();

        RetrievabilityInfo getMethod = messageFactory.createRetrievabilityInfo();
        getMethod.setMethod(RetrievabilityMethod.GET);
        RetrievabilityInfo additionalMethod = messageFactory.createRetrievabilityInfo();
        additionalMethod.setMethod(method);

        retrievability.setBy(Arrays.asList(getMethod, additionalMethod));
        return retrievability;
    }
}
