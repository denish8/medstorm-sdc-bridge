package org.somda.sdc.glue.provider.localization.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.glue.provider.localization.LocalizationService;
import org.somda.sdc.glue.provider.localization.LocalizationStorage;

/**
 * Factory to create {@linkplain LocalizationService} instances.
 */
public interface LocalizationServiceFactory {
    /**
     * Creates a new {@linkplain LocalizationService} instance.
     *
     * @param localizationStorage to use by {@link LocalizationService}
     * @return a new {@link LocalizationService} instance.
     */
    LocalizationService createLocalizationService(@Assisted LocalizationStorage localizationStorage);
}
