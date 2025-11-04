package org.somda.sdc.biceps;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.somda.sdc.biceps.guice.DefaultBicepsConfigModule;
import org.somda.sdc.biceps.guice.DefaultBicepsModule;
import org.somda.sdc.common.guice.AbstractConfigurationModule;
import org.somda.sdc.common.guice.DefaultCommonConfigModule;
import org.somda.sdc.common.guice.DefaultCommonModule;

public class UnitTestUtil {
    private final Injector injector;

    public UnitTestUtil(AbstractConfigurationModule configModule) {
        injector = Guice.createInjector(
                new DefaultCommonConfigModule(),
                new DefaultBicepsModule(),
                new DefaultCommonModule(),
                configModule);
    }

    public UnitTestUtil() {
        this(new DefaultBicepsConfigModule());
    }

    public Injector getInjector() {
        return injector;
    }

    public Injector createInjectorWithOverrides(AbstractModule overridingModule) {
        return Guice.createInjector(Modules.override(
                new DefaultBicepsModule(),
                new DefaultBicepsConfigModule(),
                new DefaultCommonModule()).with(overridingModule));
    }
}
