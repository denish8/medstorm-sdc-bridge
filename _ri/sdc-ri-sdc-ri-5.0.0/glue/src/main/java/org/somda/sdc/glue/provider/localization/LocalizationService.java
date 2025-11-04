package org.somda.sdc.glue.provider.localization;

import com.google.common.util.concurrent.Service;
import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.biceps.model.participant.LocalizedTextWidth;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.List;

/**
 * Localization service provider side.
 */
public interface LocalizationService extends Service {

    /**
     * Gets a localized text that is referenced in the MDIB.
     *
     * @param ref           a zero or more reference names of the texts that are requested.
     * @param version       of the referenced text that is requested. The latest version is used if parameter is null.
     * @param lang          a zero or more language identifiers to get different translations of the requested text.
     * @param textWidth     a zero or more {@linkplain LocalizedTextWidth} identifiers to filter for different text
     *                      widths.
     * @param numberOfLines a zero or more {@linkplain BigInteger} to filter for number of lines.
     * @return a list of {@linkplain LocalizedText} texts that matches search criteria.
     */
    List<LocalizedText> getLocalizedText(List<String> ref,
                                         @Nullable BigInteger version,
                                         List<String> lang,
                                         List<LocalizedTextWidth> textWidth,
                                         List<BigInteger> numberOfLines);

    /**
     * Gets a list of all supported languages.
     *
     * @return a list of supported language identifiers.
     */
    List<String> getSupportedLanguages();
}
