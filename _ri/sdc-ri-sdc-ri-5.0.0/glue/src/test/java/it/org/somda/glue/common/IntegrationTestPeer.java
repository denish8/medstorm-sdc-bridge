package it.org.somda.glue.common;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.somda.sdc.biceps.guice.DefaultBicepsConfigModule;
import org.somda.sdc.biceps.guice.DefaultBicepsModule;
import org.somda.sdc.common.guice.DefaultCommonConfigModule;
import org.somda.sdc.common.guice.DefaultCommonModule;
import org.somda.sdc.dpws.guice.DefaultDpwsModule;
import org.somda.sdc.glue.guice.DefaultGlueConfigModule;
import org.somda.sdc.glue.guice.DefaultGlueModule;
import org.somda.sdc.glue.guice.GlueDpwsConfigModule;

import java.util.Collection;

public abstract class IntegrationTestPeer extends AbstractIdleService {
    private Injector injector;

    protected void setupInjector(Collection<AbstractModule> overridingModules) {
        if (injector != null) {
            throw new RuntimeException("Injector already set up");
        }
        if (overridingModules.isEmpty()) {
            injector = Guice.createInjector(
                    new DefaultCommonConfigModule(),
                    new DefaultGlueModule(),
                    new DefaultGlueConfigModule(),
                    new DefaultBicepsModule(),
                    new DefaultBicepsConfigModule(),
                    new DefaultCommonModule(),
                    new DefaultDpwsModule(),
                    new GlueDpwsConfigModule());
        } else {
            injector = Guice.createInjector(
                    Modules.override(
                            new DefaultCommonConfigModule(),
                            new DefaultGlueModule(),
                            new DefaultGlueConfigModule(),
                            new DefaultBicepsModule(),
                            new DefaultBicepsConfigModule(),
                            new DefaultCommonModule(),
                            new DefaultDpwsModule(),
                            new GlueDpwsConfigModule()
                    ).with(overridingModules));
        }
    }

    public Injector getInjector() {
        if (injector == null) {
            throw new RuntimeException("Call setupInjector() before getting injector");
        }
        return injector;
    }
}
