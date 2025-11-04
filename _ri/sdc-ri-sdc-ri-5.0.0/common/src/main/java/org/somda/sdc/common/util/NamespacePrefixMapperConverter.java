package org.somda.sdc.common.util;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

import java.util.Map;

/**
 * Utility to create {@linkplain NamespacePrefixMapper} instances for JAXB marshallers.
 */
public class NamespacePrefixMapperConverter {

    /**
     * JAXB marshaller property key.
     * <p>
     * Use that string as a key for calls to {@link javax.xml.bind.Marshaller#setProperty(String, Object)} after
     * generated a {@link NamespacePrefixMapper} with {@link #convert(Map)}.
     */
    public static final String JAXB_MARSHALLER_PROPERTY_KEY = "com.sun.xml.bind.namespacePrefixMapper";

    NamespacePrefixMapperConverter() {
    }

    /**
     * Converts from a {@linkplain org.somda.sdc.common.util.PrefixNamespaceMappingParser.PrefixNamespacePair} map to a
     * {@linkplain NamespacePrefixMapper}.
     * <p>
     * To be used with JAXB marshallers to optimize namespace usage.
     *
     * @param mappings a prefix-to-namespace mapping map.
     * @return a {@link NamespacePrefixMapper} instance, configured according to the given mappings.
     */
    public NamespacePrefixMapper convert(Map<String, PrefixNamespaceMappingParser.PrefixNamespacePair> mappings) {
        final String[] declaredNamespaces = mappings.keySet().toArray(new String[mappings.size()]);
        return new NamespacePrefixMapper() {
            @Override
            public String[] getPreDeclaredNamespaceUris() {
                return declaredNamespaces;
            }

            @Override
            public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
                final PrefixNamespaceMappingParser.PrefixNamespacePair prefixNamespacePair = mappings.get(namespaceUri);
                if (prefixNamespacePair == null) {
                    return suggestion;
                }

                return prefixNamespacePair.getPrefix();
            }
        };
    }
}
