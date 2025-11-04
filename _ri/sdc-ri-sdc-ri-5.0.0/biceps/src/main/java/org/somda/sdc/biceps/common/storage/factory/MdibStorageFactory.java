package org.somda.sdc.biceps.common.storage.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.biceps.common.storage.MdibStorage;
import org.somda.sdc.biceps.model.participant.MdibVersion;

import java.math.BigInteger;

/**
 * Factory to create {@linkplain MdibStorage} instances.
 */
public interface MdibStorageFactory {
    /**
     * Creates a storage initialized with the given MDIB version.
     * <p>
     * Initializes MD description and state version with -1.
     *
     * @param mdibVersion the initial MDIB version.
     * @return an {@link MdibStorage} instance.
     */
    MdibStorage createMdibStorage(@Assisted MdibVersion mdibVersion);

    /**
     * Creates a storage initialized with the given MDIB version, MD description and MD state version.
     *
     * @param mdibVersion          the initial MDIB version.
     * @param mdDescriptionVersion the initial MD description version.
     * @param mdStateVersion       the initial MD state version.
     * @return an {@link MdibStorage} instance.
     */
    MdibStorage createMdibStorage(@Assisted MdibVersion mdibVersion,
                                  @Assisted("mdDescriptionVersion") BigInteger mdDescriptionVersion,
                                  @Assisted("mdStateVersion") BigInteger mdStateVersion);

    /**
     * Creates a storage initialized with a random MDIB version.
     *
     * @return an {@link MdibStorage} instance. The random MDIB version consists of a random sequence id and zeroed
     * instance id and version counter.
     */
    MdibStorage createMdibStorage();
}
