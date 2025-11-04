package org.somda.sdc.biceps.provider.preprocessing;

import com.google.inject.Inject;
import org.somda.sdc.biceps.common.MdibDescriptionModification;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.storage.DescriptionPreprocessingSegment;
import org.somda.sdc.biceps.common.storage.MdibStorage;

import java.util.HashSet;
import java.util.Set;

/**
 * Preprocessing segment that checks for handle duplicates on inserted entities during description modifications.
 */
public class DuplicateChecker implements DescriptionPreprocessingSegment {
    private final Set<String> handleCache;

    @Inject
    DuplicateChecker() {
        handleCache = new HashSet<>();
    }

    @Override
    public void beforeFirstModification(MdibDescriptionModifications modifications, MdibStorage mdibStorage) {
        handleCache.clear();
    }

    @Override
    public void process(MdibDescriptionModifications allModifications,
                        MdibDescriptionModification currentModification,
                        MdibStorage storage) throws Exception {
        if (currentModification.getModificationType() == MdibDescriptionModification.Type.INSERT) {
            if (storage.getEntity(currentModification.getHandle()).isPresent()) {
                throw new HandleDuplicatedException(String.format("Inserted handle is a duplicate: %s",
                        currentModification.getHandle()));
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
