package com.example.provider3_localization;

import com.example.BaseUtil;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.somda.sdc.biceps.guice.DefaultBicepsConfigModule;
import org.somda.sdc.biceps.guice.DefaultBicepsModule;
import org.somda.sdc.biceps.model.participant.LocalizedText;
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
import org.somda.sdc.glue.provider.localization.LocalizationStorage;
import org.somda.sdc.glue.provider.localization.helper.HeapBasedLocalizationStorage;

import javax.net.ssl.HostnameVerifier;
import java.math.BigInteger;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.List;

class ProviderUtil extends BaseUtil {
    private static final Logger LOG = LogManager.getLogger(ProviderUtil.class);

    private final Injector injector;

    public ProviderUtil(String[] args) {
        super(args);
        Configurator.reconfigure(localLoggerConfig(Level.INFO));

        injector = Guice.createInjector(
                new DefaultCommonConfigModule(),
                new DefaultGlueModule(),
                new DefaultGlueConfigModule(),
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

    Injector getInjector() {
        return injector;
    }

    LocalizationStorage getLocalizationStorage() {
        var storage = new HeapBasedLocalizationStorage();
        storage.addAllLocalizedTexts(populateLocalizationData());
        return storage;
    }

    private List<LocalizedText> populateLocalizationData() {
        var codingSystemNameEn = new LocalizedText();
        codingSystemNameEn.setRef("codingSystemName");
        codingSystemNameEn.setLang("en");
        codingSystemNameEn.setVersion(BigInteger.ONE);
        codingSystemNameEn.setValue("Common Parameter Nomenclature");

        var codingSystemNameDe = new LocalizedText();
        codingSystemNameDe.setRef("codingSystemName");
        codingSystemNameDe.setLang("de");
        codingSystemNameDe.setVersion(BigInteger.ONE);
        codingSystemNameDe.setValue("Allgemeine Parameternomenklatur");

        var conceptDescriptionEn = new LocalizedText();
        conceptDescriptionEn.setRef("conceptDescription");
        conceptDescriptionEn.setLang("en");
        conceptDescriptionEn.setVersion(BigInteger.ONE);
        conceptDescriptionEn.setValue("Concept Description");

        var conceptDescriptionDe = new LocalizedText();
        conceptDescriptionDe.setRef("conceptDescription");
        conceptDescriptionDe.setLang("de");
        conceptDescriptionDe.setVersion(BigInteger.ONE);
        conceptDescriptionDe.setValue("Konzeptbeschreibung");

        return List.of(codingSystemNameEn, codingSystemNameDe, conceptDescriptionEn, conceptDescriptionDe);
    }
}
