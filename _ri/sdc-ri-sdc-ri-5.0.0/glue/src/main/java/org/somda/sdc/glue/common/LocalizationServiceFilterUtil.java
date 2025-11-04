package org.somda.sdc.glue.common;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import org.somda.sdc.biceps.model.participant.LocalizedText;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility functions for Localization service.
 */
public class LocalizationServiceFilterUtil {

    /**
     * Filters localized texts by provided languages.
     *
     * @param localizedTextTable represents table of all available localized texts.
     * @param languages          list used as filter. If empty list is provided localized texts of all languages are
     *                           returned.
     * @return a map of reference as a key and localized text as a value.
     */
    public static Multimap<String, LocalizedText> filterByLanguage(
            Table<String, String, LocalizedText> localizedTextTable, List<String> languages) {

        Multimap<String, LocalizedText> referencesToTextMap = ArrayListMultimap.create();

        // if language list provided filter records by language, otherwise include all languages
        if (!languages.isEmpty()) {
            languages.forEach(language -> localizedTextTable.column(language).forEach(referencesToTextMap::put));
        } else {
            localizedTextTable.columnKeySet().forEach(
                    key -> localizedTextTable.column(key).forEach(referencesToTextMap::put));
        }
        return referencesToTextMap;
    }

    /**
     * Filter localized texts by provided references.
     *
     * @param referencesToTextMap represents a map of reference as a key and localized text as a value.
     * @param references          list used as filter.
     * @return a filtered list of localized.
     */
    public static List<LocalizedText> filterByReferences(Multimap<String, LocalizedText> referencesToTextMap,
                                                         List<String> references) {
        return references.stream()
                .filter(referencesToTextMap::containsKey)
                .map(referencesToTextMap::get)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
    }
}
