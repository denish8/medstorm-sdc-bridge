package org.somda.sdc.dpws;

import org.somda.sdc.dpws.model.LocalizedStringType;
import org.somda.sdc.dpws.model.ObjectFactory;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Convenient class to build localized string lists.
 */
public class LocalizedStringsBuilder {
    private static final String XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace";
    private static final String XML_LANG = "lang";

    private final ObjectFactory objectFactory;
    private final List<LocalizedStringType> localizedStringTypes;

    /**
     * Creates an instance with empty localized string list.
     */
    public LocalizedStringsBuilder() {
        objectFactory = new ObjectFactory();
        localizedStringTypes = new ArrayList<>(3);
    }

    /**
     * Creates an instance with one text in default language.
     *
     * @param text the text of the first element in the localized string list.
     */
    public LocalizedStringsBuilder(String text) {
        this(null, text);
    }

    /**
     * Creates an instance with one text in a specified language.
     *
     * @param locale the locale identifier in accordance to XML xml:lang specification. Default is used if null.
     * @param text   the text of the first element in the localized string list.
     */
    public LocalizedStringsBuilder(@Nullable String locale, String text) {
        this();
        add(locale, text);
    }

    /**
     * Adds a text in default language.
     *
     * @param text the text to add.
     * @return the object where the text was added to chain multiple calls.
     */
    public LocalizedStringsBuilder add(String text) {
        return add(null, text);
    }

    /**
     * Adds a text with specified language.
     *
     * @param lang locale identifier in accordance to XML xml:lang specification. Default is used if null.
     * @param text the text to add.
     * @return the object where the text was added to chain multiple calls.
     */
    public LocalizedStringsBuilder add(@Nullable String lang, String text) {
        LocalizedStringType localizedStringType = objectFactory.createLocalizedStringType();
        Optional.ofNullable(lang).ifPresent(s ->
                localizedStringType.getOtherAttributes().put(new QName(XML_NAMESPACE, XML_LANG), s));
        localizedStringType.setValue(text);
        localizedStringTypes.add(localizedStringType);
        return this;
    }

    /**
     * Gets the actual localized texts.
     *
     * @return copy of the current localized string list.
     */
    public List<LocalizedStringType> get() {
        return new ArrayList<>(localizedStringTypes);
    }

    /**
     * Resets current localized string buffer.
     *
     * @return this instance with reset localized string buffer.
     */
    public LocalizedStringsBuilder clear() {
        localizedStringTypes.clear();
        return this;
    }
}
