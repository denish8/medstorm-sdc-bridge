package org.somda.sdc.glue.consumer.report.helper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.somda.sdc.biceps.common.MdibDescriptionModification;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.MdibTypeValidator;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.message.ObjectFactory;
import org.somda.sdc.biceps.model.message.*;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.biceps.testutil.MockEntryFactory;
import org.somda.sdc.glue.UnitTestUtil;
import org.somda.sdc.glue.common.MdibVersionUtil;
import org.somda.sdc.glue.consumer.report.ReportProcessingException;
import test.org.somda.common.LoggingTestWatcher;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(LoggingTestWatcher.class)
class ReportWriterTest {
    private static final UnitTestUtil UT = new UnitTestUtil();
    private ReportWriter reportWriter;
    private RemoteMdibAccess mdibAccess;

    private ArgumentCaptor<MdibStateModifications> stateCaptor;
    private ArgumentCaptor<MdibDescriptionModifications> descriptionCaptor;
    private ObjectFactory messageFactory;
    private MockEntryFactory mockEntryFactory;
    private ArgumentCaptor<MdibVersion> mdibVersionCaptor;
    private MdibVersionUtil mdibVersionUtil;
    private ArgumentCaptor<BigInteger> nullCaptor;

    @BeforeEach
    void beforeEach() {
        reportWriter = UT.getInjector().getInstance(ReportWriter.class);
        messageFactory = new ObjectFactory();
        mdibAccess = mock(RemoteMdibAccess.class);
        stateCaptor = ArgumentCaptor.forClass(MdibStateModifications.class);
        mdibVersionCaptor = ArgumentCaptor.forClass(MdibVersion.class);
        descriptionCaptor = ArgumentCaptor.forClass(MdibDescriptionModifications.class);
        nullCaptor = ArgumentCaptor.forClass(BigInteger.class);
        mockEntryFactory = new MockEntryFactory(UT.getInjector().getInstance(MdibTypeValidator.class));
        mdibVersionUtil = UT.getInjector().getInstance(MdibVersionUtil.class);
    }

    @Test
    void writeDescription() throws Exception {
        final DescriptionModificationReport report = messageFactory.createDescriptionModificationReport();
        final DescriptionModificationReport.ReportPart reportPartInsert = messageFactory.createDescriptionModificationReportReportPart();
        final DescriptionModificationReport.ReportPart reportPartUpdate = messageFactory.createDescriptionModificationReportReportPart();
        final DescriptionModificationReport.ReportPart reportPartDelete = messageFactory.createDescriptionModificationReportReportPart();

        final MdibVersion expectedMdibVersion = MdibVersion.create();
        mdibVersionUtil.setMdibVersion(expectedMdibVersion, report);

        reportPartInsert.setModificationType(DescriptionModificationType.CRT);
        addEntry(reportPartInsert, Handles.MDS_0, MdsDescriptor.class, false);
        addEntry(reportPartInsert, Handles.MDS_1, MdsDescriptor.class, false);
        addEntry(reportPartInsert, Handles.MDS_2, MdsDescriptor.class, false);

        final String expectedUpdateParent = "update-parent";
        reportPartUpdate.setParentDescriptor(expectedUpdateParent);
        addContextEntry(reportPartUpdate, Handles.CONTEXTDESCRIPTOR_0, Arrays.asList(Handles.CONTEXT_0, Handles.CONTEXT_1), LocationContextDescriptor.class);
        addContextEntry(reportPartUpdate, Handles.CONTEXTDESCRIPTOR_1, Arrays.asList(Handles.CONTEXT_2, Handles.CONTEXT_3), PatientContextDescriptor.class);
        addContextEntry(reportPartUpdate, Handles.CONTEXTDESCRIPTOR_2, Arrays.asList(Handles.CONTEXT_4, Handles.CONTEXT_5), EnsembleContextDescriptor.class);

        final String expectedDeleteParent = "delete-parent";
        reportPartDelete.setModificationType(DescriptionModificationType.DEL);
        reportPartDelete.setParentDescriptor(expectedDeleteParent);
        addEntryDescriptor(reportPartDelete, Handles.METRIC_0, NumericMetricDescriptor.class);
        addEntryDescriptor(reportPartDelete, Handles.METRIC_1, StringMetricDescriptor.class);
        addEntryDescriptor(reportPartDelete, Handles.METRIC_2, EnumStringMetricDescriptor.class);
        addEntryDescriptor(reportPartDelete, Handles.METRIC_3, RealTimeSampleArrayMetricDescriptor.class);

        report.getReportPart().addAll(Arrays.asList(reportPartInsert, reportPartUpdate, reportPartDelete));

        reportWriter.write(report, mdibAccess);

        verify(mdibAccess).writeDescription(mdibVersionCaptor.capture(), nullCaptor.capture(), nullCaptor.capture(), descriptionCaptor.capture());

        assertEquals(expectedMdibVersion, mdibVersionCaptor.getValue());
        nullCaptor.getAllValues().forEach(Assertions::assertNull);

        final MdibDescriptionModifications modifications = descriptionCaptor.getValue();
        final List<MdibDescriptionModification> modificationsList = modifications.getModifications();
        assertEquals(10, modificationsList.size());

        testSingleState(modificationsList.get(0), Handles.MDS_0, MdibDescriptionModification.Type.INSERT, Optional.empty());
        testSingleState(modificationsList.get(1), Handles.MDS_1, MdibDescriptionModification.Type.INSERT, Optional.empty());
        testSingleState(modificationsList.get(2), Handles.MDS_2, MdibDescriptionModification.Type.INSERT, Optional.empty());

        testMultiState(modificationsList.get(3), Handles.CONTEXTDESCRIPTOR_0, Arrays.asList(Handles.CONTEXT_0, Handles.CONTEXT_1),
                MdibDescriptionModification.Type.UPDATE, Optional.of(expectedUpdateParent));
        testMultiState(modificationsList.get(4), Handles.CONTEXTDESCRIPTOR_1, Arrays.asList(Handles.CONTEXT_2, Handles.CONTEXT_3),
                MdibDescriptionModification.Type.UPDATE, Optional.of(expectedUpdateParent));
        testMultiState(modificationsList.get(5), Handles.CONTEXTDESCRIPTOR_2, Arrays.asList(Handles.CONTEXT_4, Handles.CONTEXT_5),
                MdibDescriptionModification.Type.UPDATE, Optional.of(expectedUpdateParent));

        testSingleState(modificationsList.get(6), Handles.METRIC_0, MdibDescriptionModification.Type.DELETE, Optional.of(expectedDeleteParent));
        testSingleState(modificationsList.get(7), Handles.METRIC_1, MdibDescriptionModification.Type.DELETE, Optional.of(expectedDeleteParent));
        testSingleState(modificationsList.get(8), Handles.METRIC_2, MdibDescriptionModification.Type.DELETE, Optional.of(expectedDeleteParent));
        testSingleState(modificationsList.get(9), Handles.METRIC_3, MdibDescriptionModification.Type.DELETE, Optional.of(expectedDeleteParent));
    }

    @Test
    void writeDescriptionStateWithoutDescriptor() throws Exception {
        final DescriptionModificationReport report = messageFactory.createDescriptionModificationReport();
        final DescriptionModificationReport.ReportPart reportPartInsert = messageFactory.createDescriptionModificationReportReportPart();

        final MdibVersion expectedMdibVersion = MdibVersion.create();
        mdibVersionUtil.setMdibVersion(expectedMdibVersion, report);

        reportPartInsert.setModificationType(DescriptionModificationType.CRT);
        addEntry(reportPartInsert, Handles.MDS_0, MdsDescriptor.class, false);
        addEntry(reportPartInsert, Handles.MDS_1, MdsDescriptor.class, true);
        addEntry(reportPartInsert, Handles.MDS_2, MdsDescriptor.class, false);

        report.getReportPart().addAll(Collections.singletonList(reportPartInsert));

        assertThrows(ReportProcessingException.class, () -> reportWriter.write(report, mdibAccess));
    }

    private void testSingleState(MdibDescriptionModification modification, String handle, MdibDescriptionModification.Type type, Optional<String> parentHandle) {
        assertEquals(handle, modification.getDescriptor().getHandle());
        assertEquals(parentHandle, modification.getParentHandle());
        assertEquals(type, modification.getModificationType());
        if (type != MdibDescriptionModification.Type.DELETE) {
            assertEquals(1, modification.getStates().size());
            assertEquals(handle, modification.getStates().get(0).getDescriptorHandle());
        } else {
            assertEquals(0, modification.getStates().size());
        }
    }

    private void testMultiState(MdibDescriptionModification modification, String handle, List<String> stateHandles, MdibDescriptionModification.Type type, Optional<String> parentHandle) {
        assertEquals(handle, modification.getDescriptor().getHandle());
        assertEquals(stateHandles.size(), modification.getStates().size());

        for (int i = 0; i < stateHandles.size(); ++i) {
            assertEquals(handle, modification.getStates().get(i).getDescriptorHandle());
            assertTrue(modification.getStates().get(i) instanceof AbstractMultiState);
            assertEquals(stateHandles.get(i), ((AbstractMultiState) modification.getStates().get(i)).getHandle());
        }

        assertEquals(type, modification.getModificationType());
        assertEquals(parentHandle, modification.getParentHandle());
    }

    private void addEntryDescriptor(DescriptionModificationReport.ReportPart reportPart, String handle, Class<? extends AbstractDescriptor> type) throws Exception {
        final MdibDescriptionModifications.Entry entry = mockEntryFactory.entry(handle, type);
        reportPart.getDescriptor().add(entry.getDescriptor());
    }

    private void addEntry(DescriptionModificationReport.ReportPart reportPart, String handle, Class<? extends AbstractDescriptor> type, boolean addStateWithoutDescriptor) throws Exception {
        final MdibDescriptionModifications.Entry entry = mockEntryFactory.entry(handle, type);
        if (!addStateWithoutDescriptor) {
            reportPart.getDescriptor().add(entry.getDescriptor());
        }
        reportPart.getState().add(entry.getState());
    }

    private void addContextEntry(DescriptionModificationReport.ReportPart reportPart, String handle, List<String> stateHandles, Class<? extends AbstractContextDescriptor> type) throws Exception {
        final MdibDescriptionModifications.MultiStateEntry entry = mockEntryFactory.contextEntry(handle, stateHandles, type, "parent");
        reportPart.getDescriptor().add(entry.getDescriptor());
        reportPart.getState().addAll(entry.getStates());
    }

    @Test
    void writeComponent() throws Exception {
        final EpisodicComponentReport report = messageFactory.createEpisodicComponentReport();
        final EpisodicComponentReport.ReportPart reportPart = messageFactory.createAbstractComponentReportReportPart();
        final MdibVersion expectedMdibVersion = MdibVersion.create();
        mdibVersionUtil.setMdibVersion(expectedMdibVersion, report);
        report.getReportPart().add(reportPart);

        final List<AbstractDeviceComponentState> expectedStates = Arrays.asList(
                mockEntryFactory.state("1", MdsState.class),
                mockEntryFactory.state("2", VmdState.class),
                mockEntryFactory.state("3", ChannelState.class));

        reportPart.getComponentState().addAll(expectedStates);

        reportWriter.write(report, mdibAccess);
        testStateModifications(expectedMdibVersion, expectedStates, MdibStateModifications.Type.COMPONENT);
    }

    @Test
    void writeAlert() throws Exception {
        final EpisodicAlertReport report = messageFactory.createEpisodicAlertReport();
        final EpisodicAlertReport.ReportPart reportPart = messageFactory.createAbstractAlertReportReportPart();
        final MdibVersion expectedMdibVersion = MdibVersion.create();
        mdibVersionUtil.setMdibVersion(expectedMdibVersion, report);
        report.getReportPart().add(reportPart);

        final List<AbstractAlertState> expectedStates = Arrays.asList(
                mockEntryFactory.state("1", AlertSystemState.class),
                mockEntryFactory.state("2", AlertConditionState.class),
                mockEntryFactory.state("3", LimitAlertConditionState.class));

        reportPart.getAlertState().addAll(expectedStates);

        reportWriter.write(report, mdibAccess);
        testStateModifications(expectedMdibVersion, expectedStates, MdibStateModifications.Type.ALERT);
    }

    @Test
    void writeMetric() throws Exception {
        final EpisodicMetricReport report = messageFactory.createEpisodicMetricReport();
        final EpisodicMetricReport.ReportPart reportPart = messageFactory.createAbstractMetricReportReportPart();
        final MdibVersion expectedMdibVersion = MdibVersion.create();
        mdibVersionUtil.setMdibVersion(expectedMdibVersion, report);
        report.getReportPart().add(reportPart);

        final List<AbstractMetricState> expectedStates = Arrays.asList(
                mockEntryFactory.state("1", NumericMetricState.class),
                mockEntryFactory.state("2", EnumStringMetricState.class),
                mockEntryFactory.state("3", RealTimeSampleArrayMetricState.class));

        reportPart.getMetricState().addAll(expectedStates);

        reportWriter.write(report, mdibAccess);
        testStateModifications(expectedMdibVersion, expectedStates, MdibStateModifications.Type.METRIC);
    }

    @Test
    void writeOperation() throws Exception {
        final EpisodicOperationalStateReport report = messageFactory.createEpisodicOperationalStateReport();
        final EpisodicOperationalStateReport.ReportPart reportPart = messageFactory.createAbstractOperationalStateReportReportPart();
        final MdibVersion expectedMdibVersion = MdibVersion.create();
        mdibVersionUtil.setMdibVersion(expectedMdibVersion, report);
        report.getReportPart().add(reportPart);

        final List<AbstractOperationState> expectedStates = Arrays.asList(
                mockEntryFactory.state("1", ActivateOperationState.class),
                mockEntryFactory.state("2", SetStringOperationState.class),
                mockEntryFactory.state("3", SetContextStateOperationState.class));

        reportPart.getOperationState().addAll(expectedStates);

        reportWriter.write(report, mdibAccess);
        testStateModifications(expectedMdibVersion, expectedStates, MdibStateModifications.Type.OPERATION);
    }

    @Test
    void writeContext() throws Exception {
        final EpisodicContextReport report = messageFactory.createEpisodicContextReport();
        final EpisodicContextReport.ReportPart reportPart = messageFactory.createAbstractContextReportReportPart();
        final MdibVersion expectedMdibVersion = MdibVersion.create();
        mdibVersionUtil.setMdibVersion(expectedMdibVersion, report);
        report.getReportPart().add(reportPart);

        final List<AbstractContextState> expectedStates = Arrays.asList(
                mockEntryFactory.state("1", LocationContextState.class),
                mockEntryFactory.state("2", PatientContextState.class),
                mockEntryFactory.state("3", EnsembleContextState.class));

        reportPart.getContextState().addAll(expectedStates);

        reportWriter.write(report, mdibAccess);
        testStateModifications(expectedMdibVersion, expectedStates, MdibStateModifications.Type.CONTEXT);
    }

    @Test
    void writeWaveform() throws Exception {
        final WaveformStream report = messageFactory.createWaveformStream();
        final MdibVersion expectedMdibVersion = MdibVersion.create();
        mdibVersionUtil.setMdibVersion(expectedMdibVersion, report);

        final List<RealTimeSampleArrayMetricState> expectedStates = Arrays.asList(
                mockEntryFactory.state("1", RealTimeSampleArrayMetricState.class),
                mockEntryFactory.state("2", RealTimeSampleArrayMetricState.class),
                mockEntryFactory.state("3", RealTimeSampleArrayMetricState.class));

        report.getState().addAll(expectedStates);

        reportWriter.write(report, mdibAccess);
        testStateModifications(expectedMdibVersion, expectedStates, MdibStateModifications.Type.WAVEFORM);
    }

    void testStateModifications(MdibVersion expectedMdibVersion, List<? extends AbstractState> expectedStates, MdibStateModifications.Type expectedType) throws PreprocessingException {
        verify(mdibAccess).writeStates(mdibVersionCaptor.capture(), stateCaptor.capture());

        assertEquals(expectedMdibVersion, mdibVersionCaptor.getValue());

        final MdibStateModifications modifications = stateCaptor.getValue();
        assertEquals(expectedType, modifications.getChangeType());
        assertEquals(expectedStates.size(), modifications.getStates().size());
        for (int i = 0; i < expectedStates.size(); ++i) {
            assertEquals(expectedStates.get(i).getDescriptorHandle(), modifications.getStates().get(i).getDescriptorHandle());
        }
    }
}