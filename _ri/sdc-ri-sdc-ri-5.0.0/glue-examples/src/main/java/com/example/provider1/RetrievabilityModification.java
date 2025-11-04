package com.example.provider1;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.common.MdibDescriptionModification;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.storage.DescriptionPreprocessingSegment;
import org.somda.sdc.biceps.common.storage.MdibStorage;
import org.somda.sdc.biceps.model.extension.ExtensionType;
import org.somda.sdc.biceps.model.message.Retrievability;
import org.somda.sdc.biceps.model.message.RetrievabilityInfo;
import org.somda.sdc.biceps.model.message.RetrievabilityMethod;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.RealTimeSampleArrayMetricDescriptor;

import java.util.List;

/**
 * Adds the retrievability extension to all descriptors in the mdib.
 * <p>
 * Descriptors will be assigned {@link RetrievabilityMethod#GET} and {@link RetrievabilityMethod#EP} retrievability,
 * except for {@link RealTimeSampleArrayMetricDescriptor} elements, which
 * are set to {@link RetrievabilityMethod#GET} and {@link RetrievabilityMethod#STRM}
 */
public class RetrievabilityModification implements DescriptionPreprocessingSegment {

    private static Logger LOG = LogManager.getLogger(RetrievabilityModification.class);
    @Override
    public void beforeFirstModification(MdibDescriptionModifications modifications, MdibStorage mdibStorage) {
        DescriptionPreprocessingSegment.super.beforeFirstModification(modifications, mdibStorage);
    }

    @Override
    public void afterLastModification(MdibDescriptionModifications modifications, MdibStorage mdibStorage) {
        DescriptionPreprocessingSegment.super.afterLastModification(modifications, mdibStorage);
    }

    @Override
    public void process(MdibDescriptionModifications allModifications, MdibDescriptionModification currentModification, MdibStorage storage) throws Exception {
        if (currentModification.getModificationType() == MdibDescriptionModification.Type.DELETE) {
            // don't mess with deletes, no point
            return;
        }

        var modificationDescriptor = currentModification.getDescriptor();
        // find retrievability
        var extension = modificationDescriptor
                .getExtension();
        if (extension == null) {
            extension = new ExtensionType();
            modificationDescriptor.setExtension(extension);
        }
        Retrievability retrievability = extension
                .getAny()
                .stream()
                .filter(it -> it instanceof Retrievability)
                .map(it -> (Retrievability) it)
                .findFirst()
                .orElseGet(() -> {
                    var it = new Retrievability();
                    LOG.debug("Creating new retrievability for {}", modificationDescriptor.getHandle());
                    modificationDescriptor.getExtension().getAny().add(it);
                    return it;
                });

        var infoGet = new RetrievabilityInfo();
        infoGet.setMethod(RetrievabilityMethod.GET);
        if (modificationDescriptor instanceof RealTimeSampleArrayMetricDescriptor) {
            // set retrievability to STRM
            var infoStrm = new RetrievabilityInfo();
            infoStrm.setMethod(RetrievabilityMethod.STRM);
            retrievability.setBy(List.of(infoGet, infoStrm));
        } else if (modificationDescriptor instanceof AbstractDescriptor) {
            // set retrievability to EP
            var infoStrm = new RetrievabilityInfo();
            infoStrm.setMethod(RetrievabilityMethod.EP);
            retrievability.setBy(List.of(infoGet, infoStrm));
        }
        LOG.debug("Retrievability for {} set to {}" , modificationDescriptor.getHandle(), retrievability);
    }
}
