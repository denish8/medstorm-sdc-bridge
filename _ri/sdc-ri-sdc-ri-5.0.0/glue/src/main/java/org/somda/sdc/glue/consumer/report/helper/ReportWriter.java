package org.somda.sdc.glue.consumer.report.helper;

import com.google.common.collect.ArrayListMultimap;
import com.google.inject.Inject;
import org.somda.sdc.biceps.common.MdibDescriptionModification;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.MdibTypeValidator;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.message.AbstractAlertReport;
import org.somda.sdc.biceps.model.message.AbstractComponentReport;
import org.somda.sdc.biceps.model.message.AbstractContextReport;
import org.somda.sdc.biceps.model.message.AbstractMetricReport;
import org.somda.sdc.biceps.model.message.AbstractOperationalStateReport;
import org.somda.sdc.biceps.model.message.AbstractReport;
import org.somda.sdc.biceps.model.message.DescriptionModificationReport;
import org.somda.sdc.biceps.model.message.DescriptionModificationType;
import org.somda.sdc.biceps.model.message.WaveformStream;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractMultiState;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.glue.common.MdibVersionUtil;
import org.somda.sdc.glue.consumer.report.ReportProcessingException;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper class that accepts any state reports and writes them to a {@linkplain RemoteMdibAccess} instance.
 * <p>
 * The {@linkplain ReportWriter} acts as a dispatcher for all reports SDC generates and which have to be transformed
 * to modifications a {@link RemoteMdibAccess} instance understands.
 */
public class ReportWriter {
    private final MdibTypeValidator typeValidator;
    private final MdibVersionUtil mdibVersionUtil;

    @Inject
    ReportWriter(MdibTypeValidator typeValidator,
                 MdibVersionUtil mdibVersionUtil) {
        this.typeValidator = typeValidator;
        this.mdibVersionUtil = mdibVersionUtil;
    }

    /**
     * Transforms the given report to a modifications set and writes it to the {@linkplain RemoteMdibAccess} instance.
     *
     * @param report     the report to write.
     * @param mdibAccess the MDIB access to write to.
     * @throws ReportProcessingException in case the report cannot be transformed or dispatched correctly.
     * @throws PreprocessingException    corresponds to the exception that {@link RemoteMdibAccess#writeDescription(
     *                                   MdibVersion, BigInteger, BigInteger, MdibDescriptionModifications)} or
     *                                   {@link RemoteMdibAccess#writeStates(MdibVersion, MdibStateModifications)}
     *                                   throws.
     */
    public void write(AbstractReport report, RemoteMdibAccess mdibAccess)
            throws ReportProcessingException, PreprocessingException {
        if (report instanceof WaveformStream) {
            write(report, makeModifications((WaveformStream) report), mdibAccess);
        } else if (report instanceof AbstractMetricReport) {
            write(report, makeModifications((AbstractMetricReport) report), mdibAccess);
        } else if (report instanceof AbstractAlertReport) {
            write(report, makeModifications((AbstractAlertReport) report), mdibAccess);
        } else if (report instanceof AbstractOperationalStateReport) {
            write(report, makeModifications((AbstractOperationalStateReport) report), mdibAccess);
        } else if (report instanceof AbstractComponentReport) {
            write(report, makeModifications((AbstractComponentReport) report), mdibAccess);
        } else if (report instanceof AbstractContextReport) {
            write(report, makeModifications((AbstractContextReport) report), mdibAccess);
        } else if (report instanceof DescriptionModificationReport) {
            write((DescriptionModificationReport) report, mdibAccess);
        } else {
            throw new ReportProcessingException(String.format("Unexpected report type: %s",
                    report.getClass().getSimpleName()));
        }
    }

    private void write(AbstractReport report, MdibStateModifications modifications, RemoteMdibAccess mdibAccess)
            throws PreprocessingException {
        mdibAccess.writeStates(mdibVersionUtil.getMdibVersion(report), modifications);
    }

    private void write(DescriptionModificationReport report, RemoteMdibAccess mdibAccess)
            throws ReportProcessingException, PreprocessingException {
        mdibAccess.writeDescription(mdibVersionUtil.getMdibVersion(report),
                null,
                null,
                mdibDescriptionModifications(report));
    }

    private MdibDescriptionModifications mdibDescriptionModifications(DescriptionModificationReport report)
            throws ReportProcessingException {
        final MdibDescriptionModifications modifications = MdibDescriptionModifications.create();
        for (DescriptionModificationReport.ReportPart reportPart : report.getReportPart()) {
            final MdibDescriptionModification.Type modType = mapModType(reportPart.getModificationType());
            final ArrayListMultimap<String, AbstractState> stateMap =
                    ArrayListMultimap.create(reportPart.getState().size(), 1);

            reportPart.getState().forEach(state -> stateMap.put(state.getDescriptorHandle(), state));
            final var stateHandles = reportPart.getState().stream().map(AbstractState::getDescriptorHandle).collect(Collectors.toSet());
            final var descriptorHandles = reportPart.getDescriptor().stream().map(AbstractDescriptor::getHandle).collect(Collectors.toSet());
            for (var handle: stateHandles) {
                if (!descriptorHandles.contains(handle)) {
                    throw new ReportProcessingException(String.format("The state %s belongs to an " +
                            "unknown descriptor", handle));
                }
            }
            for (AbstractDescriptor descriptor : reportPart.getDescriptor()) {
                final List<AbstractState> stateList = stateMap.get(descriptor.getHandle());
                if (modType != MdibDescriptionModification.Type.DELETE) {
                    if (typeValidator.isSingleStateDescriptor(descriptor)) {
                        if (stateList.size() != 1) {
                            throw new ReportProcessingException(String.format("Change of single state descriptor %s " +
                                            "comes with unexpected number of states: %s",
                                    descriptor.getHandle(), stateList.size()));
                        }

                        modifications.add(modType, descriptor, stateList.get(0), reportPart.getParentDescriptor());
                    } else {
                        try {
                            List<AbstractMultiState> multiStates = new ArrayList<>(stateList.size());
                            stateList.forEach(state -> {
                                if (state instanceof AbstractMultiState) {
                                    multiStates.add((AbstractMultiState) state);
                                } else {
                                    throw new RuntimeException(String.format("Data type mismatch. " +
                                                    "Expected an AbstractMultiState, got %s",
                                            state.getClass().getName()));
                                }
                            });
                            modifications.add(modType, descriptor, multiStates, reportPart.getParentDescriptor());
                        } catch (Exception e) {
                            throw new ReportProcessingException(String.format(
                                    "Type mismatch between descriptor %s and state", descriptor.getHandle()));
                        }
                    }
                } else {
                    modifications.add(modType, descriptor, reportPart.getParentDescriptor());
                }
            }
        }

        return modifications;
    }

    private MdibStateModifications makeModifications(WaveformStream report) {
        return MdibStateModifications.create(MdibStateModifications.Type.WAVEFORM,
                report.getState().size()).addAll(report.getState());
    }

    private MdibStateModifications makeModifications(AbstractMetricReport report) {
        final int capacity = report.getReportPart().stream().mapToInt(rp -> rp.getMetricState().size()).sum();
        final MdibStateModifications modifications =
                MdibStateModifications.create(MdibStateModifications.Type.METRIC, capacity);

        for (AbstractMetricReport.ReportPart reportPart : report.getReportPart()) {
            modifications.addAll(reportPart.getMetricState());
        }

        return modifications;
    }

    private MdibStateModifications makeModifications(AbstractAlertReport report) {
        final int capacity = report.getReportPart().stream().mapToInt(rp -> rp.getAlertState().size()).sum();
        final MdibStateModifications modifications =
                MdibStateModifications.create(MdibStateModifications.Type.ALERT, capacity);

        for (AbstractAlertReport.ReportPart reportPart : report.getReportPart()) {
            modifications.addAll(reportPart.getAlertState());
        }

        return modifications;
    }

    private MdibStateModifications makeModifications(AbstractComponentReport report) {
        final int capacity = report.getReportPart().stream().mapToInt(rp -> rp.getComponentState().size()).sum();
        final MdibStateModifications modifications =
                MdibStateModifications.create(MdibStateModifications.Type.COMPONENT, capacity);

        for (AbstractComponentReport.ReportPart reportPart : report.getReportPart()) {
            modifications.addAll(reportPart.getComponentState());
        }

        return modifications;
    }

    private MdibStateModifications makeModifications(AbstractContextReport report) {
        final int capacity = report.getReportPart().stream().mapToInt(rp -> rp.getContextState().size()).sum();
        final MdibStateModifications modifications =
                MdibStateModifications.create(MdibStateModifications.Type.CONTEXT, capacity);

        for (AbstractContextReport.ReportPart reportPart : report.getReportPart()) {
            modifications.addAll(reportPart.getContextState());
        }

        return modifications;
    }

    private MdibStateModifications makeModifications(AbstractOperationalStateReport report) {
        final int capacity = report.getReportPart().stream().mapToInt(rp -> rp.getOperationState().size()).sum();
        final MdibStateModifications modifications =
                MdibStateModifications.create(MdibStateModifications.Type.OPERATION, capacity);

        for (AbstractOperationalStateReport.ReportPart reportPart : report.getReportPart()) {
            modifications.addAll(reportPart.getOperationState());
        }

        return modifications;
    }

    private static MdibDescriptionModification.Type mapModType(@Nullable DescriptionModificationType modificationType) {
        if (modificationType == null) {
            return MdibDescriptionModification.Type.UPDATE;
        }

        switch (modificationType) {
            case CRT:
                return MdibDescriptionModification.Type.INSERT;
            case UPT:
                return MdibDescriptionModification.Type.UPDATE;
            case DEL:
                return MdibDescriptionModification.Type.DELETE;
            default:
                throw new RuntimeException(String.format("Unexpected description modification type detected. " +
                                "Processing branch is missing: %s",
                        modificationType));
        }
    }
}
