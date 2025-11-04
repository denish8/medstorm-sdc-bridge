package org.somda.sdc.dpws;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.common.guice.DefaultCommonConfigModule;
import org.somda.sdc.common.guice.DefaultCommonModule;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.guice.DefaultDpwsModule;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConfig;
import test.org.somda.common.CIDetector;
import test.org.somda.common.LoggingTestWatcher;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Test base class to provide common test functionality.
 */
@ExtendWith(LoggingTestWatcher.class)
public abstract class DpwsTest {
    private static final Logger LOG = LogManager.getLogger(DpwsTest.class);

    private Injector injector;
    private List<AbstractModule> overridingModules;

    protected void overrideBindings(AbstractModule module) {
        this.overrideBindings(List.of(module));
    }

    protected void overrideBindings(List<AbstractModule> module) {
        this.overridingModules = module;
    }

    protected void setUp() throws Exception {
        injector = configureInjector(Optional.ofNullable(overridingModules).orElse(Collections.emptyList()));
    }

    protected static Injector configureInjector(List<AbstractModule> overridingModules) {
        var dpwsConfigOverride = new DefaultDpwsConfigModule() {
            @Override
            protected void customConfigure() {
                super.customConfigure();
                if (CIDetector.isRunningInCi()) {
                    var httpTimeouts = Duration.ofSeconds(120);
                    var futureTimeouts = Duration.ofSeconds(30);
                    LOG.info("CI detected, setting relaxed HTTP client timeouts of {}s",
                            httpTimeouts.toSeconds());
                    bind(DpwsConfig.HTTP_CLIENT_CONNECT_TIMEOUT,
                            Duration.class,
                            httpTimeouts);

                    bind(DpwsConfig.HTTP_CLIENT_READ_TIMEOUT,
                            Duration.class,
                            httpTimeouts);

                    List<String> increasedTimeouts = List.of(
                            DpwsConfig.MAX_WAIT_FOR_FUTURES,
                            WsDiscoveryConfig.MAX_WAIT_FOR_PROBE_MATCHES,
                            WsDiscoveryConfig.MAX_WAIT_FOR_RESOLVE_MATCHES
                    );
                    increasedTimeouts.forEach(item -> {
                        LOG.info("CI detected, setting {} to {}s", item, futureTimeouts.toSeconds());
                        bind(item,
                                Duration.class,
                                futureTimeouts);
                    });

                }
            }
        };
        final Injector injector;
        if (!overridingModules.isEmpty()) {
            injector = Guice.createInjector(
                    Modules.override(
                            new DefaultCommonConfigModule(),
                            new DefaultDpwsModule(),
                            new DefaultCommonModule(),
                            dpwsConfigOverride
                    ).with(
                            overridingModules
                    )
            );
        } else {
            injector = Guice.createInjector(
                    new DefaultCommonConfigModule(), new DefaultDpwsModule(),
                    new DefaultCommonModule(), dpwsConfigOverride
            );
        }
        return injector;
    }

    protected Injector getInjector() {
        return injector;
    }
}
