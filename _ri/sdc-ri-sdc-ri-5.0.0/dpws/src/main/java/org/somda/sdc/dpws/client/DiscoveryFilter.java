package org.somda.sdc.dpws.client;

import org.somda.sdc.dpws.soap.wsdiscovery.MatchBy;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filter information to be used by {@link Client#probe(DiscoveryFilter)}.
 */
public class DiscoveryFilter {

    private static final AtomicInteger DISCOVERY_ID_COUNTER = new AtomicInteger(0);

    private final Collection<QName> types;
    private final Collection<String> scopes;

    @Nullable
    private final MatchBy matchBy;

    private final String discoveryId;


    /**
     * Creates a new discovery filter with a discovery id that is unique across one application instance.
     *
     * @param types  the types to match.
     * @param scopes the scopes to match.
     * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231831"
     * >WS-Discovery Probe</a>
     */
    public DiscoveryFilter(Collection<QName> types, Collection<String> scopes) {
        this(types, scopes, null);
    }

    /**
     * Creates a new discovery filter with a discovery id that is unique across one application instance.
     *
     * @param types  the types to match.
     * @param scopes the scopes to match.
     * @param matchBy the scopes matching rule, or null to use the default
     *                <code>http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01/rfc3986</code>.
     * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231831"
     * >WS-Discovery Probe</a>
     */
    public DiscoveryFilter(Collection<QName> types, Collection<String> scopes, @Nullable MatchBy matchBy) {
        this.types = types;
        this.scopes = scopes;
        this.matchBy = matchBy;
        this.discoveryId = Integer.toString(DISCOVERY_ID_COUNTER.incrementAndGet());
    }

    public Collection<QName> getTypes() {
        return types;
    }

    public Collection<String> getScopes() {
        return scopes;
    }

    public MatchBy getMatchBy() {
        return matchBy;
    }

    /**
     * Gets the discovery id.
     * <p>
     * The discovery id is used to distinguish between probe requests.
     *
     * @return the discover id of this filter.
     */
    public String getDiscoveryId() {
        return discoveryId;
    }
}
