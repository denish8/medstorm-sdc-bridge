package org.somda.sdc.biceps.common;

import org.somda.sdc.biceps.common.storage.MdibStorage;

import java.util.List;

/**
 * General configuration of the BICEPS common package.
 *
 * @see org.somda.sdc.biceps.guice.DefaultBicepsConfigModule
 */
public class CommonConfig {
    /**
     * If true any input to an MDIB is copied before stored in the {@link MdibStorage}.
     * <p>
     * This inhibits the user from changing the data stored in the {@link MdibStorage}.
     * <em>Not being able to change the data after writing is at the expense of copying memory!</em>
     * <ul>
     * <li>Data type: {@linkplain Boolean}
     * <li>Use: optional
     * </ul>
     */
    public static final String COPY_MDIB_INPUT = "Biceps.Common.CopyMdibInput";

    /**
     * If true any output from an MDIB is copied before exposed to the user.
     * <p>
     * This inhibits the user from changing the data stored in the {@link MdibStorage}.
     * <em>Not being able to change the data while reading is at the expense of copying memory!</em>
     * <ul>
     * <li>Data type: {@linkplain Boolean}
     * <li>Use: optional
     * </ul>
     */
    public static final String COPY_MDIB_OUTPUT = "Biceps.Common.CopyMdibInput";

    /**
     * If true context states which are not associated are not stored.
     * <p>
     * Since there is no mechanism which allows discarding context states, enabling this feature
     * automatically prunes likely irrelevant data from the storage.
     * <ul>
     * <li>Data type: {@linkplain Boolean}
     * <li>Use: optional
     * </ul>
     */
    public static final String STORE_NOT_ASSOCIATED_CONTEXT_STATES = "Biceps.Common.StoreNotAssociatedContextStates";

    /**
     * A list of all {@linkplain org.somda.sdc.biceps.common.storage.DescriptionPreprocessingSegment}, which are applied
     * during description modifications.
     * <p>
     * A consumer can specify which {@linkplain org.somda.sdc.biceps.common.storage.DescriptionPreprocessingSegment}
     * should be used, by adding them to the list.
     * <em>If a preprocessing segment is used for descriptors and states the same instance will be shared!</em>
     * <ul>
     * <li>Data type: {@link List} of classes that implement
     * {@link org.somda.sdc.biceps.common.storage.DescriptionPreprocessingSegment}
     * <li>Use: optional
     * </ul>
     */
    public static final String CONSUMER_DESCRIPTION_PREPROCESSING_SEGMENTS =
            "Biceps.Common.ConsumerDescriptionPreprocessingSegments";

    /**
     * A list of all {@linkplain org.somda.sdc.biceps.common.storage.StatePreprocessingSegment}, which are applied
     * during state modifications.
     * <p>
     * A consumer can specify which {@linkplain org.somda.sdc.biceps.common.storage.StatePreprocessingSegment} should
     * be used, by adding them to the list.
     * <em>If a preprocessing segment is used for descriptors and states the same instance will be shared!</em>
     * <ul>
     * <li>Data type: {@link List} of classes that implement
     * {@link org.somda.sdc.biceps.common.storage.StatePreprocessingSegment}
     * <li>Use: optional
     * </ul>
     */
    public static final String CONSUMER_STATE_PREPROCESSING_SEGMENTS =
            "Biceps.Common.ConsumerStatePreprocessingSegments";

    /**
     * A list of all {@linkplain org.somda.sdc.biceps.common.storage.DescriptionPreprocessingSegment}, which are applied
     * during description modifications.
     * <p>
     * A provider can specify which {@linkplain org.somda.sdc.biceps.common.storage.DescriptionPreprocessingSegment}
     * should be used, by adding them to the list.
     * <em>If a preprocessing segment is used for descriptors and states the same instance will be shared!</em>
     * <ul>
     * <li>Data type: {@link List} of classes that implement
     * {@link org.somda.sdc.biceps.common.storage.DescriptionPreprocessingSegment}
     * <li>Use: optional
     * </ul>
     */
    public static final String PROVIDER_DESCRIPTION_PREPROCESSING_SEGMENTS =
            "Biceps.Common.ProviderDescriptionPreprocessingSegments";

    /**
     * A list of all {@linkplain org.somda.sdc.biceps.common.storage.StatePreprocessingSegment}, which are applied
     * during state modifications.
     * <p>
     * A provider can specify which {@linkplain org.somda.sdc.biceps.common.storage.StatePreprocessingSegment} should
     * be used, by adding them to the list.
     * <em>If a preprocessing segment is used for descriptors and states the same instance will be shared!</em>
     * <ul>
     * <li>Data type: {@link List} of classes that implement
     * {@link org.somda.sdc.biceps.common.storage.StatePreprocessingSegment}
     * <li>Use: optional
     * </ul>
     */
    public static final String PROVIDER_STATE_PREPROCESSING_SEGMENTS =
            "Biceps.Common.ProviderStatePreprocessingSegments";
}
