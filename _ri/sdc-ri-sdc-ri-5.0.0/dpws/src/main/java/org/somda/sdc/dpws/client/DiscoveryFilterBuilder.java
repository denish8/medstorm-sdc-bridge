package org.somda.sdc.dpws.client;

import javax.xml.namespace.QName;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link DiscoveryFilter} convenience builder with method chaining.
 */
public class DiscoveryFilterBuilder {
    private final Set<QName> types;
    private final Set<String> scopes;

    /**
     * Constructs a new object with empty types and scopes.
     */
    public DiscoveryFilterBuilder() {
        this.types = new HashSet<>();
        this.scopes = new HashSet<>();
    }

    /**
     * Adds a type.
     *
     * @param type the type as QName according to WS-Discovery.
     * @return this object.
     */
    public DiscoveryFilterBuilder addType(QName type) {
        types.add(type);
        return this;
    }

    /**
     * Adds a scope.
     *
     * @param scope the scope URI as string.
     * @return this object.
     */
    public DiscoveryFilterBuilder addScope(String scope) {
        scopes.add(scope);
        return this;
    }

    /**
     * Gets a discovery filter with all types and scopes added via {@link #addType(QName)} and
     * {@link #addScope(String)}.
     *
     * @return a {@linkplain DiscoveryFilter} instance.
     */
    public DiscoveryFilter get() {
        return new DiscoveryFilter(types, scopes);
    }
}
