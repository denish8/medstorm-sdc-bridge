package org.somda.sdc.glue;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.somda.sdc.biceps.guice.DefaultBicepsConfigModule;
import org.somda.sdc.biceps.guice.DefaultBicepsModule;
import org.somda.sdc.common.guice.DefaultCommonConfigModule;
import org.somda.sdc.common.guice.DefaultCommonModule;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.guice.DefaultDpwsModule;
import org.somda.sdc.glue.guice.DefaultGlueConfigModule;
import org.somda.sdc.glue.guice.DefaultGlueModule;
import test.org.somda.common.TestLogging;

public class UnitTestUtil {
    private final Injector injector;

    public UnitTestUtil() {
        TestLogging.configure();
        injector = Guice.createInjector(
                new DefaultCommonConfigModule(),
                new DefaultGlueModule(),
                new DefaultGlueConfigModule(),
                new DefaultBicepsModule(),
                new DefaultBicepsConfigModule(),
                new DefaultCommonModule(),
                new DefaultDpwsModule(),
                new DefaultDpwsConfigModule());
    }

    public Injector getInjector() {
        return injector;
    }

    public Injector createInjectorWithOverrides(AbstractModule overridingModule) {
        return Guice.createInjector(Modules.override(
                new DefaultCommonConfigModule(),
                new DefaultGlueModule(),
                new DefaultGlueConfigModule(),
                new DefaultBicepsModule(),
                new DefaultBicepsConfigModule(),
                new DefaultCommonModule(),
                new DefaultDpwsModule(),
                new DefaultDpwsConfigModule()).with(overridingModule));
    }
}
