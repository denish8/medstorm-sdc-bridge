package org.somda.sdc.dpws.soap.wsdiscovery;

/**
 * Set of supported MatchBy rules for the WS-Discovery Scopes type in accordance with dpws:R1019.
 *
 * @see <a href="http://docs.oasis-open.org/ws-dd/dpws/1.1/os/wsdd-dpws-1.1-spec-os.html#_Toc228672091">3 Discovery</a>
 */
public enum MatchBy {
    RFC3986("http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01/rfc3986"),
    STRCMP0("http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01/strcmp0");

    private final String matchByUri;

    MatchBy(String matchByUri) {
        this.matchByUri = matchByUri;
    }

    public String getUri() {
        return matchByUri;
    }

}
