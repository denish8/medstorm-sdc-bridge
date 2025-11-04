package org.somda.sdc.glue.provider.localization.helper;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import org.apache.commons.lang3.StringUtils;
import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.glue.common.LocalizationServiceFilterUtil;
import org.somda.sdc.glue.provider.localization.LocalizationException;
import org.somda.sdc.glue.provider.localization.LocalizationStorage;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@linkplain LocalizationStorage}.
 * <p>
 * Localized texts are stored in heap ({@link Map}) and can be added during runtime.
 */
public class HeapBasedLocalizationStorage implements LocalizationStorage {

    private final List<String> supportedLanguages = new ArrayList<>();

    /**
     * Representation of Map<Version, Table<Row, Column, Value>>,
     * where row = ref, column = lang, value = LocalizedText
     */
    private final Map<BigInteger, Table<String, String, LocalizedText>> localizationStorage = new HashMap<>();

    @Override
    public synchronized List<String> getSupportedLanguages() {
        return supportedLanguages;
    }

    @Override
    public synchronized List<LocalizedText> getLocalizedText(List<String> references,
                                                @Nullable BigInteger version,
                                                List<String> languages) {

        // if version not provided, get latest version from storage
        if (version == null) {
            version = getLatestVersion();
        }

        Multimap<String, LocalizedText> refToValueMap =
                LocalizationServiceFilterUtil.filterByLanguage(localizationStorage.get(version), languages);

        // if references not provided, return all records, otherwise filter by reference
        return references.isEmpty() ? new ArrayList<>(refToValueMap.values()) :
                LocalizationServiceFilterUtil.filterByReferences(refToValueMap, references);
    }

    /**
     * Adds provided localized text to the {@linkplain LocalizationStorage}.
     *
     * @param text the {@link LocalizedText} to be added to the storage.
     */
    public synchronized void addLocalizedText(LocalizedText text) {
        // check if all mandatory data provided before processing
        checkRequiredAttributesNonEmpty(text);

        addToSupportedLanguages(text);
        addToStorage(text);
    }

    /**
     * Adds provided collection of localized texts to the {@linkplain LocalizationStorage}.
     *
     * @param texts a collection of {@link LocalizedText} to be added to the storage.
     */
    public synchronized void addAllLocalizedTexts(Collection<LocalizedText> texts) {
        texts.forEach(this::checkRequiredAttributesNonEmpty);
        texts.forEach(this::addLocalizedText);

    }

    private void checkRequiredAttributesNonEmpty(LocalizedText text) {
        if (text.getVersion() == null || StringUtils.isAnyBlank(text.getLang(), text.getRef())) {
            throw new LocalizationException(
                    "Localized text invalid, mandatory fields 'version', 'lang' or 'ref' are missing. " +
                            "Localized text: " + text);
        }
    }

    private void addToStorage(LocalizedText text) {
        var table = localizationStorage.getOrDefault(
                text.getVersion(),
                HashBasedTable.create());
        table.put(text.getRef(), text.getLang(), text);

        localizationStorage.put(text.getVersion(), table);
    }

    private void addToSupportedLanguages(LocalizedText text) {
        if (!supportedLanguages.contains(text.getLang())) {
            supportedLanguages.add(text.getLang());
        }
    }

    private BigInteger getLatestVersion() {
        return localizationStorage.entrySet()
                .stream()
                .max(Map.Entry.comparingByKey())
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new LocalizationException("Failed to determine latest translations version"));
    }
}
