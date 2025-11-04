package org.somda.sdc.glue.provider.localization;

import org.somda.sdc.biceps.model.participant.LocalizedText;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.List;

/**
 * Localization storage.
 */
public interface LocalizationStorage {

    /**
     * Gets a list of all supported languages.
     *
     * @return a list of supported language identifiers.
     */
    List<String> getSupportedLanguages();

    /**
     * Gets a localized text that is referenced in the MDIB.
     *
     * @param references a zero or more reference names of the texts that are requested.
     *                   If empty list is provided localized texts are not filtered by reference.
     * @param version    of the referenced text that is requested. The latest version is used if parameter is null.
     * @param languages  a zero or more language identifiers to get different translations of the requested text.
     *                   If empty list is provided localized texts are not filtered by language.
     * @return a list of {@link LocalizedText} texts that matches search criteria.
     */
    List<LocalizedText> getLocalizedText(List<String> references,
                                         @Nullable BigInteger version,
                                         List<String> languages);

}
