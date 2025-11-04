package org.somda.sdc.glue.guice;

import org.somda.sdc.biceps.common.CommonConstants;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.soap.SoapConfig;
import org.somda.sdc.glue.GlueConstants;

import static org.somda.sdc.glue.common.CommonConstants.NAMESPACE_PREFIX_MAPPINGS_BICEPS;
import static org.somda.sdc.glue.common.CommonConstants.NAMESPACE_PREFIX_MAPPINGS_GLUE;
import static org.somda.sdc.glue.common.CommonConstants.NAMESPACE_PREFIX_MAPPINGS_MDPWS;

public class GlueDpwsConfigModule extends DefaultDpwsConfigModule {
    @Override
    protected void customConfigure() {
        bind(SoapConfig.JAXB_CONTEXT_PATH,
                String.class,
                CommonConstants.BICEPS_JAXB_CONTEXT_PATH);
        bind(SoapConfig.JAXB_SCHEMA_PATH,
                String.class,
                GlueConstants.SCHEMA_PATH);
        bind(SoapConfig.NAMESPACE_MAPPINGS,
                String.class,
                NAMESPACE_PREFIX_MAPPINGS_MDPWS +
                        NAMESPACE_PREFIX_MAPPINGS_BICEPS +
                        NAMESPACE_PREFIX_MAPPINGS_GLUE);
    }
}
