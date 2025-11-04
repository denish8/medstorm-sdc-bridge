package it.org.somda.sdc.dpws;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.somda.sdc.common.guice.DefaultCommonConfigModule;
import org.somda.sdc.common.guice.DefaultCommonModule;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.guice.DefaultDpwsModule;

import javax.annotation.Nullable;
import java.util.List;

public abstract class IntegrationTestPeer extends AbstractIdleService {
    private Injector injector;

    protected void setupInjector(DefaultDpwsConfigModule configModule, @Nullable AbstractModule ... overridingModule) {
        if (overridingModule != null) {
            injector = Guice.createInjector(
                    Modules.override(
                            new DefaultCommonConfigModule(),
                            new DefaultDpwsModule(),
                            new DefaultCommonModule(),
                            configModule
                    ).with(List.of(overridingModule))
            );
        } else {
            injector = Guice.createInjector(new DefaultDpwsModule(), new DefaultCommonModule(), configModule);
        }
    }

    protected void setupInjector(DefaultDpwsConfigModule configModule) {
        setupInjector(configModule, null);
    }

    public Injector getInjector() {
        if (injector == null) {
            throw new RuntimeException("Call setupInjector() before getting injector");
        }
        return injector;
    }
}
