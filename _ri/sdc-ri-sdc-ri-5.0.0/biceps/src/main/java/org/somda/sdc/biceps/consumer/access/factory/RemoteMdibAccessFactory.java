package org.somda.sdc.biceps.consumer.access.factory;

import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.provider.preprocessing.DuplicateChecker;
import org.somda.sdc.biceps.provider.preprocessing.TypeConsistencyChecker;

/**
 * Factory to create {@linkplain RemoteMdibAccess} instances.
 */
public interface RemoteMdibAccessFactory {
    /**
     * Creates a local mdib access with an MDIB version that has a random sequence id.
     * <p>
     * The following preprocessing steps are visited in the following order:
     * <ol>
     * <li>{@link DuplicateChecker}
     * <li>{@link TypeConsistencyChecker}
     * <li>{@link org.somda.sdc.biceps.consumer.preprocessing.VersionDuplicateHandler}
     * </ol>
     *
     * @return the remote mdib access instance.
     */
    RemoteMdibAccess createRemoteMdibAccess();
}
