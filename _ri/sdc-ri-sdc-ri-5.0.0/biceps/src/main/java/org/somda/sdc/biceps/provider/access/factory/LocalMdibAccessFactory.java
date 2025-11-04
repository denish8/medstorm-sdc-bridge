package org.somda.sdc.biceps.provider.access.factory;

import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.provider.preprocessing.DuplicateChecker;
import org.somda.sdc.biceps.provider.preprocessing.TypeConsistencyChecker;
import org.somda.sdc.biceps.provider.preprocessing.VersionHandler;

/**
 * Factory to create {@linkplain LocalMdibAccess} instances.
 */
public interface LocalMdibAccessFactory {
    /**
     * Creates a local mdib access with an MDIB version that has a random sequence id.
     * <p>
     * The following preprocessing steps are visited in the following order:
     * <ol>
     * <li>{@link DuplicateChecker}
     * <li>{@link TypeConsistencyChecker}
     * <li>{@link VersionHandler}
     * </ol>
     *
     * @return the local mdib access instance.
     */
    LocalMdibAccess createLocalMdibAccess();
}
