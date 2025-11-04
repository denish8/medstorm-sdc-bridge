package test.org.somda.common;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

import java.util.List;

/**
 * Utility class with static logging configuration for tests.
 */
public class TestLogging {

    /**
     * @see org.somda.sdc.common.logging.InstanceLogger#INSTANCE_ID
     */
    private static final String CONTEXT_INSTANCE_ID = "instanceId";

    /**
     * @see org.somda.sdc.glue.consumer.helper.HostingServiceLogger#HOSTING_SERVICE_INFO
     */
    private static final String HOSTING_SERVICE_INFO = "hostingServiceInfo";

    private static final List<String> CHATTY_LOGGERS = List.of(
            "org.apache.http.wire",
            "org.apache.http.headers",
            "org.eclipse.jetty"
    );

    private static final String CUSTOM_PATTERN = "%d{HH:mm:ss.SSS}"
            + " [%thread]"
            // only include the space if we have a variable
            + " %notEmpty{[%X{" + CONTEXT_INSTANCE_ID + "}] }"
            + " %notEmpty{[%X{" + HOSTING_SERVICE_INFO + "}] }"
            + "%-5level"
            + " %logger{36}"
            + " - %msg%n";

    private static BuiltConfiguration ciConfiguration() {
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        builder.setStatusLevel(Level.ERROR);
        builder.setConfigurationName("CILogging");

        var layoutBuilder = builder
                .newLayout("PatternLayout")
                .addAttribute("pattern", CUSTOM_PATTERN);

        var rootLogger = builder.newRootLogger(Level.DEBUG);

        {
            // create a console appender
            var appenderBuilder = builder
                    .newAppender("console_logger", ConsoleAppender.PLUGIN_NAME)
                    .addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT);
            appenderBuilder.add(layoutBuilder);
            // only log WARN or worse to console
            appenderBuilder.addComponent(
                    builder.newFilter("ThresholdFilter", Filter.Result.ACCEPT, Filter.Result.DENY)
                            .addAttribute("level", Level.WARN)
            );
            builder.add(appenderBuilder);

            rootLogger.add(builder.newAppenderRef(appenderBuilder.getName()));
        }
        {
            // create a file appender
            var appenderBuilder = builder.newAppender("file", "File")
                    .addAttribute("fileName", "target/test.log")
                    .addAttribute("append", true)
                    .add(layoutBuilder);
            builder.add(appenderBuilder);

            rootLogger.add(builder.newAppenderRef(appenderBuilder.getName()));
        }
        {
            // quiet down chatty loggers
            CHATTY_LOGGERS.forEach(logger -> {
                builder.add(builder.newLogger(logger, Level.INFO)
                        .addAttribute("additivity", true));
            });
        }

        builder.add(rootLogger);
        return builder.build();
    }

    private static BuiltConfiguration localConfig(Level consoleLevel) {
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        builder.setStatusLevel(Level.ERROR);
        builder.setConfigurationName("LocalLogging");

        var layoutBuilder = builder
                .newLayout("PatternLayout")
                .addAttribute("pattern", CUSTOM_PATTERN);

        var rootLogger = builder.newRootLogger(Level.DEBUG);

        {
            // create a console appender
            var appenderBuilder = builder
                    .newAppender("console_logger", ConsoleAppender.PLUGIN_NAME)
                    .addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT);
            appenderBuilder.add(layoutBuilder);
            // only log WARN or worse to console
            appenderBuilder.addComponent(
                    builder.newFilter("ThresholdFilter", Filter.Result.ACCEPT, Filter.Result.DENY)
                            .addAttribute("level", consoleLevel)
            );
            builder.add(appenderBuilder);

            rootLogger.add(builder.newAppenderRef(appenderBuilder.getName()));
        }
        {
            // quiet down chatty loggers
            CHATTY_LOGGERS.forEach(logger -> {
                builder.add(builder.newLogger(logger, Level.INFO)
                        .addAttribute("additivity", true));
            });
        }

        builder.add(rootLogger);
        return builder.build();
    }


    /**
     * Configures a default logging valid for all tests.
     */
    public static void configure() {
        if (CIDetector.isRunningInCi()) {
            Configurator.initialize(ciConfiguration());
        } else {
            // no file appender when not running in ci
            Configurator.initialize(localConfig(Level.DEBUG));
        }
    }
}
