package org.somda.sdc.glue.common.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.glue.common.MdibMapper;

/**
 * Factory to create {@linkplain MdibMapper} instances.
 */
public interface MdibMapperFactory {
    /**
     * Creates a new {@linkplain MdibMapper} that maps from {@linkplain MdibAccess}
     * to {@linkplain org.somda.sdc.biceps.model.participant.Mdib}.
     *
     * @param mdibAccess the MDIB access where to map data from.
     * @return a new instance to map to {@link org.somda.sdc.biceps.model.participant.Mdib}.
     */
    MdibMapper createMdibMapper(@Assisted MdibAccess mdibAccess);
}
