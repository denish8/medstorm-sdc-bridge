package org.somda.sdc.biceps.common.preprocessing;

import org.somda.sdc.biceps.common.MdibDescriptionModification;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.storage.DescriptionPreprocessingSegment;
import org.somda.sdc.biceps.common.storage.MdibStorage;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.AlertSystemDescriptor;
import org.somda.sdc.biceps.model.participant.ChannelDescriptor;
import org.somda.sdc.biceps.model.participant.MdsDescriptor;
import org.somda.sdc.biceps.model.participant.ScoDescriptor;
import org.somda.sdc.biceps.model.participant.SystemContextDescriptor;
import org.somda.sdc.biceps.model.participant.VmdDescriptor;

/**
 * Removes children from descriptors in order to avoid redundant information in the MDIB storage.
 */
public class DescriptorChildRemover implements DescriptionPreprocessingSegment {

    @Override
    public void process(MdibDescriptionModifications allModifications,
                        MdibDescriptionModification currentModification,
                        MdibStorage storage) throws Exception {
        final AbstractDescriptor descriptor = currentModification.getDescriptor();
        if (descriptor instanceof MdsDescriptor) {
            removeChildren((MdsDescriptor) descriptor);
        } else if (descriptor instanceof VmdDescriptor) {
            removeChildren((VmdDescriptor) descriptor);
        } else if (descriptor instanceof ChannelDescriptor) {
            removeChildren((ChannelDescriptor) descriptor);
        } else if (descriptor instanceof ScoDescriptor) {
            removeChildren((ScoDescriptor) descriptor);
        } else if (descriptor instanceof SystemContextDescriptor) {
            removeChildren((SystemContextDescriptor) descriptor);
        } else if (descriptor instanceof AlertSystemDescriptor) {
            removeChildren((AlertSystemDescriptor) descriptor);
        }
    }

    private void removeChildren(AlertSystemDescriptor descriptor) {
        descriptor.setAlertSignal(null);
        descriptor.setAlertCondition(null);
    }

    private void removeChildren(SystemContextDescriptor descriptor) {
        descriptor.setLocationContext(null);
        descriptor.setPatientContext(null);
        descriptor.setMeansContext(null);
        descriptor.setOperatorContext(null);
        descriptor.setWorkflowContext(null);
        descriptor.setEnsembleContext(null);
    }

    private void removeChildren(ScoDescriptor descriptor) {
        descriptor.setOperation(null);
    }

    private void removeChildren(ChannelDescriptor descriptor) {
        descriptor.setMetric(null);
    }

    private void removeChildren(VmdDescriptor descriptor) {
        descriptor.setChannel(null);
        descriptor.setAlertSystem(null);
    }

    /**
     * Removes the children from a provided mds.
     * <p>
     * Removes the battery, clock, system context, vmd, alert system and sco
     * @param mds without the children
     */
    private void removeChildren(MdsDescriptor mds) {
        mds.setBattery(null);
        mds.setClock(null);
        mds.setSystemContext(null);
        mds.setVmd(null);
        mds.setAlertSystem(null);
        mds.setSco(null);
    }
}
