package com.example.consumer3_localization;

import com.example.BaseUtil;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.somda.sdc.biceps.guice.DefaultBicepsConfigModule;
import org.somda.sdc.biceps.guice.DefaultBicepsModule;
import org.somda.sdc.common.guice.DefaultCommonConfigModule;
import org.somda.sdc.common.guice.DefaultCommonModule;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.crypto.CryptoConfig;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.sdc.dpws.guice.DefaultDpwsModule;
import org.somda.sdc.glue.GlueConstants;
import org.somda.sdc.glue.consumer.ConsumerConfig;
import org.somda.sdc.glue.guice.DefaultGlueConfigModule;
import org.somda.sdc.glue.guice.DefaultGlueModule;
import org.somda.sdc.glue.guice.GlueDpwsConfigModule;

import javax.net.ssl.HostnameVerifier;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;

/**
 * This class provides the configuration used for the consumer instance.
 * <p>
 * Overwriting configuration steps allows customizing the behavior of the framework through injection.
 */
class ConsumerUtil extends BaseUtil {
    private static final Logger LOG = LogManager.getLogger(ConsumerUtil.class);

    private final Injector injector;

    ConsumerUtil(String[] args) {
        super(args);
        Configurator.reconfigure(localLoggerConfig(Level.INFO));

        injector = Guice.createInjector(
                new DefaultCommonConfigModule(),
                new DefaultGlueModule(),
                new DefaultGlueConfigModule() {
                    @Override
                    protected void customConfigure() {
                        super.customConfigure();
                        bind(ConsumerConfig.WATCHDOG_PERIOD,
                                Duration.class,
                                Duration.ofMinutes(1));
                    }
                },
                new DefaultBicepsModule(),
                new DefaultBicepsConfigModule(),
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
                        bind(CryptoConfig.CRYPTO_CLIENT_HOSTNAME_VERIFIER,
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
                                                        GlueConstants.OID_KEY_PURPOSE_SDC_SERVICE_PROVIDER))) {
                                                    LOG.debug("SDC Service Provider PKP found");
                                                    return true;
                                                }
                                            } catch (IllegalArgumentException e) {
                                                // don't care, was no uri
                                            }
                                        }
                                        return true;
                                    } catch (Exception e) {
                                        LOG.error("Error while validating provider certificate: {}", e.getMessage());
                                        LOG.trace("Error while validating provider certificate", e);
                                    }
                                    return false;
                                });
                    }
                });
    }

    Injector getInjector() {
        return injector;
    }
}
