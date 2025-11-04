package com.example.provider1;

import com.example.BaseUtil;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.somda.sdc.biceps.common.CommonConfig;
import org.somda.sdc.biceps.common.preprocessing.DescriptorChildRemover;
import org.somda.sdc.biceps.guice.DefaultBicepsConfigModule;
import org.somda.sdc.biceps.guice.DefaultBicepsModule;
import org.somda.sdc.biceps.model.participant.AbstractMetricValue;
import org.somda.sdc.biceps.model.participant.GenerationMode;
import org.somda.sdc.biceps.model.participant.MeasurementValidity;
import org.somda.sdc.biceps.provider.preprocessing.DuplicateChecker;
import org.somda.sdc.biceps.provider.preprocessing.HandleReferenceHandler;
import org.somda.sdc.biceps.provider.preprocessing.TypeConsistencyChecker;
import org.somda.sdc.biceps.provider.preprocessing.VersionHandler;
import org.somda.sdc.common.guice.DefaultCommonConfigModule;
import org.somda.sdc.common.guice.DefaultCommonModule;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.crypto.CryptoConfig;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.sdc.dpws.guice.DefaultDpwsModule;
import org.somda.sdc.glue.GlueConstants;
import org.somda.sdc.glue.guice.DefaultGlueConfigModule;
import org.somda.sdc.glue.guice.DefaultGlueModule;
import org.somda.sdc.glue.guice.GlueDpwsConfigModule;

import javax.net.ssl.HostnameVerifier;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;

/**
 * This class provides the configuration used for the provider instance.
 * <p>
 * Overwriting configuration steps allows customizing the behavior of the framework through
 * injection.
 */
public class ProviderUtil extends BaseUtil {
    public static final String OPT_REPORT_INTERVAL = "report_interval";
    public static final String OPT_WAVEFORMS_INTERVAL = "waveform_interval";

    private static final Logger LOG = LogManager.getLogger(ProviderUtil.class);

    private static final String DEFAULT_REPORT_INTERVAL = "5000"; // millis
    private static final String DEFAULT_WAVEFORM_INTERVAL = "100"; // millis

    private final Injector injector;
    private final Duration reportInterval;
    private final Duration waveformInterval;

    public ProviderUtil(String[] args) {
        super(args);
        Configurator.reconfigure(localLoggerConfig(Level.INFO));

        reportInterval = Duration.ofMillis(
                Long.parseLong(getParsedArgs().getOptionValue(OPT_REPORT_INTERVAL, DEFAULT_REPORT_INTERVAL))
        );

        waveformInterval = Duration.ofMillis(
                Long.parseLong(getParsedArgs().getOptionValue(OPT_WAVEFORMS_INTERVAL, DEFAULT_WAVEFORM_INTERVAL))
        );


        injector = Guice.createInjector(
                new DefaultCommonConfigModule(),
                new DefaultGlueModule(),
                new DefaultGlueConfigModule(),
                new DefaultBicepsModule(),
                new DefaultBicepsConfigModule() {
                    @Override
                    protected void customConfigure() {
                        bind(CommonConfig.PROVIDER_DESCRIPTION_PREPROCESSING_SEGMENTS,
                                new TypeLiteral<>() {
                                },
                                List.of(DuplicateChecker.class, TypeConsistencyChecker.class,
                                        VersionHandler.class,
                                        HandleReferenceHandler.class, DescriptorChildRemover.class,
                                        RetrievabilityModification.class));

                    }
                },
                new DefaultCommonModule(),
                new DefaultDpwsModule(),
                new GlueDpwsConfigModule() {
                    @Override
                    protected void customConfigure() {
                        super.customConfigure();
                        bind(CryptoConfig.CRYPTO_SETTINGS,
                                CryptoSettings.class,
                                createCustomCryptoSettings()
                        );
                        bind(DpwsConfig.HTTPS_SUPPORT, Boolean.class, isUseTls());
                        bind(DpwsConfig.HTTP_SUPPORT, Boolean.class, !isUseTls());
                        bind(CryptoConfig.CRYPTO_DEVICE_HOSTNAME_VERIFIER,
                                HostnameVerifier.class,
                                (hostname, session) -> {
                                    try {
                                        // since this is not a real implementation, we still want to allow all peers
                                        // which is why this doesn't really filter anything
                                        // returning false in this filter would reject an incoming request
                                        var peerCerts = session.getPeerCertificates();
                                        final X509Certificate x509 = (X509Certificate) peerCerts[0];
                                        List<String> extendedKeyUsage = x509.getExtendedKeyUsage();
                                        if (extendedKeyUsage == null || extendedKeyUsage.isEmpty()) {
                                            LOG.warn("No EKU in peer certificate");
                                            return true;
                                        }

                                        // find matching provider key purpose
                                        for (String key : extendedKeyUsage) {
                                            try {
                                                URI keyUri = URI.create(key);
                                                if (keyUri.equals(URI.create(
                                                        GlueConstants.OID_KEY_PURPOSE_SDC_SERVICE_CONSUMER))) {
                                                    LOG.debug("SDC Service Consumer PKP found");
                                                    return true;
                                                }
                                            } catch (IllegalArgumentException e) {
                                                // don't care, was no uri
                                            }
                                        }
                                        return true;
                                    } catch (Exception e) {
                                        LOG.error("Error while validating client certificate: {}", e.getMessage());
                                        LOG.trace("Error while validating client certificate", e);
                                    }
                                    return false;
                                });

                    }
                });
    }

    public Injector getInjector() {
        return injector;
    }

    public static void addMetricQualityDemo(AbstractMetricValue val) {
        if (val.getMetricQuality() == null) {
            var qual = new AbstractMetricValue.MetricQuality();
            qual.setMode(GenerationMode.DEMO);
            qual.setValidity(MeasurementValidity.VLD);
            val.setMetricQuality(qual);
        }
    }

    @Override
    protected Options configureOptions() {
        var options = super.configureOptions();

        var reportIntervalOpt = Option.builder(null)
                .desc("Interval in ms in which reports are being generated. Default: " + DEFAULT_REPORT_INTERVAL)
                .longOpt(OPT_REPORT_INTERVAL)
                .hasArg()
                .type(Long.class)
                .build();
        options.addOption(reportIntervalOpt);

        var waveformIntervalOpt = Option.builder(null)
                .desc("Interval in ms in which waveforms are being generated. Default: " + DEFAULT_WAVEFORM_INTERVAL)
                .longOpt(OPT_WAVEFORMS_INTERVAL)
                .hasArg()
                .type(Long.class)
                .build();
        options.addOption(waveformIntervalOpt);

        return options;
    }

    public Duration getReportInterval() {
        return reportInterval;
    }

    public Duration getWaveformInterval() {
        return waveformInterval;
    }
}
