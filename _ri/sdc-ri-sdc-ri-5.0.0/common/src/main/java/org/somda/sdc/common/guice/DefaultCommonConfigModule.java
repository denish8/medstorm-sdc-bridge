package org.somda.sdc.common.guice;

import org.somda.sdc.common.CommonConfig;

import java.util.UUID;

/**
 * Configuration for the common module.
 */
public class DefaultCommonConfigModule extends AbstractConfigurationModule {

    @Override
    protected void defaultConfigure() {
        configureLogging();
    }

    private void configureLogging() {
        var generatedUuid = UUID.randomUUID().toString();
        bind(CommonConfig.INSTANCE_IDENTIFIER,
                String.class,
                generatedUuid.substring(generatedUuid.length() - 4));
    }
}
