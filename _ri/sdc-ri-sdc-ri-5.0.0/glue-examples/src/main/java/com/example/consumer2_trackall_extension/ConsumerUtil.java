package com.example.consumer2_trackall_extension;

import com.example.BaseUtil;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.util.Modules;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.somda.sdc.biceps.common.CommonConstants;
import org.somda.sdc.biceps.guice.DefaultBicepsConfigModule;
import org.somda.sdc.biceps.guice.DefaultBicepsModule;
import org.somda.sdc.common.guice.DefaultCommonConfigModule;
import org.somda.sdc.common.guice.DefaultCommonModule;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.CommunicationLogImpl;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.crypto.CryptoConfig;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.sdc.dpws.factory.CommunicationLogFactory;
import org.somda.sdc.dpws.guice.DefaultDpwsModule;
import org.somda.sdc.dpws.soap.SoapConfig;
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

import static org.somda.sdc.glue.common.CommonConstants.*;

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
                Modules.override(new DefaultDpwsModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        install(new FactoryModuleBuilder()
                                .implement(CommunicationLog.class, CommunicationLogImpl.class)
                                .build(CommunicationLogFactory.class));
                    }
                }),
                new GlueDpwsConfigModule() {
                    @Override
                    protected void customConfigure() {
                        bind(SoapConfig.JAXB_CONTEXT_PATH,
                                String.class,
                                CommonConstants.BICEPS_JAXB_CONTEXT_PATH +
                                        ":org.somda.sdc.glue.examples.extension");
                        bind(SoapConfig.JAXB_SCHEMA_PATH,
                                String.class,
                                GlueConstants.SCHEMA_PATH + ":provider2_extension/JaxbCompiledExtension.xsd");
                        bind(SoapConfig.NAMESPACE_MAPPINGS,
                                String.class,
                                NAMESPACE_PREFIX_MAPPINGS_MDPWS +
                                        NAMESPACE_PREFIX_MAPPINGS_BICEPS +
                                        NAMESPACE_PREFIX_MAPPINGS_GLUE);

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
