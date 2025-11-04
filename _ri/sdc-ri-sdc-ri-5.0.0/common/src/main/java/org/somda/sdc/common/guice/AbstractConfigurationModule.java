package org.somda.sdc.common.guice;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility class for other modules to allow app configuration via Google Guice.
 * <p>
 * Derive any concrete configuration module in order to override default values.
 * Use {@link #bind(String, Class, Object)} to set values.
 */
public abstract class AbstractConfigurationModule extends AbstractModule {
    private static final Logger LOG = LogManager.getLogger(AbstractConfigurationModule.class);
    private final Map<String, ConfigurationValue> boundValues = new TreeMap<>();
    private boolean configureStarted = false;

    /**
     * Binds a configuration key to a value from outside.
     * <p>
     * This operation can only be performed once per key.
     * All unpopulated keys are supposed to be filled with a default value once {@link #configure()} is called by Guice.
     *
     * @param name     the configuration key.
     * @param dataType the data type bound by the key (should be defined in configuration class).
     * @param value    the configuration value to set.
     * @param <T>      type that is required by the given key.
     */
    public <T> void bind(String name, Class<T> dataType, @Nullable T value) {
        if (!boundValues.containsKey(name)) {
            // Wrap binding into closure and call later as Guice's bind() is only available during configure()
            Runnable runBind = () -> {
                if (value == null) {
                    bind(dataType)
                            .annotatedWith(Names.named(name))
                            .toProvider(Providers.of(null));
                } else {
                    bind(dataType)
                            .annotatedWith(Names.named(name))
                            .toInstance(value);
                }
            };
            ValueOrigin valueOrigin = configureStarted ? ValueOrigin.DEFAULTED : ValueOrigin.CUSTOMIZED;
            boundValues.put(name, new ConfigurationValue(valueOrigin, runBind, value));

        } else {
            if (!configureStarted) {
                LOG.warn("Try to populate configuration key '{}' twice. Attempt skipped.", name);
            }
        }
    }

    /**
     * Binds a configuration key to a value from outside.
     * <p>
     * This operation can only be performed once per key.
     * All unpopulated keys are supposed to be filled with a default value once {@link #configure()} is called by Guice.
     *
     * @param name        the configuration key.
     * @param typeLiteral the data type bound by the key (should be defined in configuration class).
     * @param value       the configuration value to set.
     * @param <T>         type that is required by the given key.
     */
    public <T> void bind(String name, TypeLiteral<T> typeLiteral, @Nullable T value) {
        if (!boundValues.containsKey(name)) {
            // Wrap binding into closure and call later as Guice's bind() is only available during configure()
            Runnable runBind = () -> {
                if (value == null) {
                    bind(typeLiteral)
                            .annotatedWith(Names.named(name))
                            .toProvider(Providers.of(null));
                } else {
                    bind(typeLiteral)
                            .annotatedWith(Names.named(name))
                            .toInstance(value);
                }
            };
            ValueOrigin valueOrigin = configureStarted ? ValueOrigin.DEFAULTED : ValueOrigin.CUSTOMIZED;
            boundValues.put(name, new ConfigurationValue(valueOrigin, runBind, value));

        } else {
            if (!configureStarted) {
                LOG.warn("Try to populate configuration key '{}' twice. Attempt skipped.", name);
            }
        }
    }

    /**
     * Processes the default configuration.
     * <p>
     * <em>This method is called by Guice to apply the configuration values.</em>
     */
    @Override
    @SuppressWarnings("Unchecked")
    protected final void configure() {
        customConfigure();
        configureStarted = true;
        defaultConfigure();
        logConfiguredValues();

        boundValues.forEach((key, value) -> value.getBinder().run());
    }

    /**
     * Implement this method to settle your default configuration.
     * <p>
     * <em>This is only relevant to the module that provides a certain configuration!</em>
     */
    protected abstract void defaultConfigure();

    /**
     * Implement this method to apply some custom configuration.
     * <p>
     * This method is relevant to users that want to override default configuration values.
     * Instead of overriding this function, it is also legit to bind values from outside the instance itself by using
     * {@link #bind(String, Class, Object)}.
     * Always to that <em>before</em> an instance is processed by Guice!
     */
    protected void customConfigure() {
        // Override is optional
    }

    private void logConfiguredValues() {
        boundValues.forEach((key, value) -> LOG.info("{} {} := {}",
                                                      value.getValueOrigin(),
                                                      key,
                                                      value.getValue()));
    }

    private enum ValueOrigin {
        DEFAULTED("[defaulted ]"),
        CUSTOMIZED("[customized]");

        private final String caption;

        ValueOrigin(String value) {
            caption = value;
        }

        @Override
        public String toString() {
            return caption;
        }
    }

    private static class ConfigurationValue {
        private final ValueOrigin valueOrigin;
        private final Runnable binder;
        private final Object value;

        ConfigurationValue(ValueOrigin valueOrigin, Runnable binder, Object value) {
            this.valueOrigin = valueOrigin;
            this.binder = binder;
            this.value = value;
        }

        ValueOrigin getValueOrigin() {
            return valueOrigin;
        }

        Runnable getBinder() {
            return binder;
        }

        Object getValue() {
            return value;
        }
    }
}
