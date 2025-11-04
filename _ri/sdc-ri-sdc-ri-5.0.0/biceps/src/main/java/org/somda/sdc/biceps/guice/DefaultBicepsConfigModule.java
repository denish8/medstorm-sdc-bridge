package org.somda.sdc.biceps.guice;

import com.google.inject.TypeLiteral;
import org.somda.sdc.biceps.common.CommonConfig;
import org.somda.sdc.biceps.common.MdibTypeValidator;
import org.somda.sdc.biceps.common.access.CopyManager;
import org.somda.sdc.biceps.common.preprocessing.DescriptorChildRemover;
import org.somda.sdc.biceps.common.storage.DescriptionPreprocessingSegment;
import org.somda.sdc.biceps.common.storage.StatePreprocessingSegment;
import org.somda.sdc.biceps.consumer.preprocessing.DuplicateContextStateHandleHandler;
import org.somda.sdc.biceps.consumer.preprocessing.VersionDuplicateHandler;
import org.somda.sdc.biceps.provider.preprocessing.DuplicateChecker;
import org.somda.sdc.biceps.provider.preprocessing.HandleReferenceHandler;
import org.somda.sdc.biceps.provider.preprocessing.TypeConsistencyChecker;
import org.somda.sdc.biceps.provider.preprocessing.VersionHandler;
import org.somda.sdc.common.guice.AbstractConfigurationModule;

import java.util.List;

/**
 * Guice module that determines default values for the BICEPS package configuration.
 */
public class DefaultBicepsConfigModule extends AbstractConfigurationModule {
    @Override
    public void defaultConfigure() {
        configureCommon();
    }

    private void configureCommon() {
        bind(CommonConfig.COPY_MDIB_INPUT,
                Boolean.class,
                true);

        bind(CommonConfig.COPY_MDIB_OUTPUT,
                Boolean.class,
                true);

        bind(CommonConfig.STORE_NOT_ASSOCIATED_CONTEXT_STATES,
                Boolean.class,
                false);

        bind(CommonConfig.CONSUMER_STATE_PREPROCESSING_SEGMENTS,
                new TypeLiteral<List<Class<? extends StatePreprocessingSegment>>>() {
                },
                List.of(DuplicateContextStateHandleHandler.class, VersionDuplicateHandler.class));

        bind(CommonConfig.CONSUMER_DESCRIPTION_PREPROCESSING_SEGMENTS,
                new TypeLiteral<List<Class<? extends DescriptionPreprocessingSegment>>>() {
                },
                List.of(DescriptorChildRemover.class));

        bind(CommonConfig.PROVIDER_STATE_PREPROCESSING_SEGMENTS,
                new TypeLiteral<List<Class<? extends StatePreprocessingSegment>>>() {
                },
                List.of(DuplicateContextStateHandleHandler.class, VersionHandler.class));

        bind(CommonConfig.PROVIDER_DESCRIPTION_PREPROCESSING_SEGMENTS,
                new TypeLiteral<List<Class<? extends DescriptionPreprocessingSegment>>>() {
                },
                List.of(DuplicateChecker.class, TypeConsistencyChecker.class, VersionHandler.class,
                        HandleReferenceHandler.class, DescriptorChildRemover.class));

        bind(CopyManager.class).asEagerSingleton();
        bind(MdibTypeValidator.class).asEagerSingleton();
    }
}