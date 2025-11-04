package org.somda.sdc.glue.provider.localization;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.biceps.model.participant.LocalizedTextWidth;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.List;

/**
 * Default implementation of {@linkplain LocalizationService}
 */
public class LocalizationServiceImpl extends AbstractIdleService implements LocalizationService {
    private final LocalizationStorage localizationStorage;

    @Inject
    public LocalizationServiceImpl(@Assisted LocalizationStorage localizationStorage) {
        this.localizationStorage = localizationStorage;
    }

    @Override
    protected void startUp() throws Exception {
    }

    @Override
    protected void shutDown() throws Exception {
    }

    @Override
    public List<LocalizedText> getLocalizedText(List<String> ref,
                                                @Nullable BigInteger version,
                                                List<String> lang,
                                                List<LocalizedTextWidth> textWidth,
                                                List<BigInteger> numberOfLines) {
        // textWidth and numberOfLines parameters are currently ignored.
        return localizationStorage.getLocalizedText(ref, version, lang);
    }

    @Override
    public List<String> getSupportedLanguages() {
        return localizationStorage.getSupportedLanguages();
    }
}
