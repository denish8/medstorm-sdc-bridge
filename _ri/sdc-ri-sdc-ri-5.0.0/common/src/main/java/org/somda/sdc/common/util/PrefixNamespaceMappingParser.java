package org.somda.sdc.common.util;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to parse prefix-to-namespace string representation used by configuration values.
 */
public class PrefixNamespaceMappingParser {
    private static final Logger LOG = LogManager.getLogger(PrefixNamespaceMappingParser.class);

    PrefixNamespaceMappingParser() {
    }

    /**
     * Parses the given prefix-to-namespace mapping string.
     * <p>
     * Format: namespaces and their prefixes will be parsed as key-value pairs, separated by curly parenthesis.
     * <p>
     * Example: {prefix1:http://namespace-uri1}{prefix2:http://namespace-uri2}{prefix3:http://namespace-uri3}
     *
     * @param prefixNamespaces the string to parse.
     * @return a map of namespace keys to prefix-namespace elements.
     */
    public Map<String, PrefixNamespacePair> parse(String prefixNamespaces) {
        final Map<String, PrefixNamespacePair> mapping = new HashMap<>();
        final Pattern pattern = Pattern.compile("\\{(.+?):(.+?)\\}");

        for (final Matcher matcher = pattern.matcher(prefixNamespaces); matcher.find();) {
            final String prefix = matcher.group(1);
            final String uri = matcher.group(2);
            try {
                // todo: DGr URI validation could be optimized; java.net.URI is too lax here
                mapping.put(uri, new PrefixNamespacePair(prefix, (new URI(uri)).toString()));
            } catch (URISyntaxException e) {
                LOG.warn("Given namespace in {} is not a valid URI: {}", prefixNamespaces, uri);
            }
        }

        return mapping;
    }

    /**
     * A prefix-namespace pair extracted from {@link #parse(String)}.
     */
    public static class PrefixNamespacePair {
        private final String prefix;
        private final String namespace;

        public PrefixNamespacePair(String prefix, String namespace) {
            this.prefix = prefix;
            this.namespace = namespace;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getNamespace() {
            return namespace;
        }

        @Override
        public String toString() {
            return String.format("{%s:%s}", prefix, namespace);
        }

        @Override
        public boolean equals(Object rhs) {
            if (this == rhs) {
                return true;
            }

            if (rhs instanceof PrefixNamespacePair) {
                PrefixNamespacePair castRhs = (PrefixNamespacePair) rhs;
                return prefix.equals(castRhs.prefix) && namespace.equals(castRhs.namespace);
            }

            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(prefix, namespace);
        }
    }
}
