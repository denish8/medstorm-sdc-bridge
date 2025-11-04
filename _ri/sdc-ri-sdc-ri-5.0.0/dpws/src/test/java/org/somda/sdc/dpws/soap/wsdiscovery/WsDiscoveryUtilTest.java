package org.somda.sdc.dpws.soap.wsdiscovery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.dpws.DpwsTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WsDiscoveryUtilTest extends DpwsTest {
    private WsDiscoveryUtil wsDiscoveryUtil;

    private static final String DEFAULT_SUPERSET = "http://a.de/abc/d//e";

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        this.wsDiscoveryUtil = getInjector().getInstance(WsDiscoveryUtil.class);
    }

    @Test
    void isScopesMatchingStrcmpRegressionTest() {
        var superset = List.of("http://a.de", "http://b.de");
        var subset = List.of("http://b.de");

        assertTrue(wsDiscoveryUtil.isScopesMatching(superset, subset, MatchBy.STRCMP0));
    }

    @Test
    void isScopesMatchingDefaultSupersetRfc3986Test() {
        // all subsets should match default superset DEFAULT_SUPERSET
        var matchingSubsets = List.of(
                "http://a.de/abc/d//e",        // equals
                "HTTP://a.de/abc/d//e",        // case-insensitive schema
                "http://A.dE/abc/d//e",        // case-insensitive authority
                "http://a.de/abc",             // match by segment
                "http://a.de/abc/d",           // match by segment
                "http://a.de/abc/d/",          // match by segment, trailing slash ignored
                "http://a.de/abc/d//",         // match by segment, trailing slashes ignored
                "http://a.de",                 // match if subset has no segments
                "http://a.de/abc?a=x&b=y",     // query parameters are ignored during comparison
                "http://a.de/abc#fragement1"   // fragments are ignored
        );
        matchingSubsets.forEach(subset -> assertTrue(doesMatchDefault(subset), String.format("%s did not match %s", subset, DEFAULT_SUPERSET)));

        // all subsets should NOT match default superset DEFAULT_SUPERSET
        var notMatchingSubsets = List.of(
                "http://a.de/Abc/d//e",        // case sensitive segment
                "http%3A%2F%2Fa.de%2Fabc%2Fd%2F%2Fe", // encoded URI doesn't match
                "http://a.de/ab",              // doesn't match if only part of segment matches
                "http://a.de/abc/d//e/f",      // doesn't match if subset has more segment than superset
                "http://a.de/abc/./d",         // doesn't match if subset has '.' or '..' in path
                "http://a.de/abc/../d"         // doesn't match if subset has '.' or '..' in path
        );
        notMatchingSubsets.forEach(subset -> assertFalse(doesMatchDefault(subset), String.format("%s matched %s", subset, DEFAULT_SUPERSET)));
    }

    @Test
    void isScopesMatchingCustomSupersetRfc3986Test() {
        // test different protocol (not HTTP, HTTPS, etc)
        var matchingSubsets = Map.of(
                "https://127.0.0.1:46581", "https://127.0.0.1:46581",
                "https://127.0.0.1:46581/a/b", "https://127.0.0.1:46581/a",
                "sdc.mds.pkp:1.2.840.10004.20701.1.1", "sdc.mds.pkp:1.2.840.10004.20701.1.1",
                "urn:oid:2.5.4.3", "urn:oid:2.5.4.3",
                "urn:oid", "uRn:oid",
                "urn:uuid:6e4f8cf7-39a2-48eb-866b-0f92b9d17b97", "urn:uuid:6e4f8cf7-39a2-48eb-866b-0f92b9d17b97",
                "my-scheme:abc", "my-schemE:abc",
                "http://a/b", "http://a",
                "http://f:fifty-two/c", "http://f:fifty-two/c",
                "non-special://f:999999/c", "NoN-special://f:999999/c"
        );
        matchingSubsets.forEach((superset, subset) -> assertTrue(doesMatch(superset, subset)));

        var notMatchingSubsets = Map.of(
                "urn:oid", "urs:oid",                                 // not equal
                "http://a.de/abc/./d", "http://a.de/abc/./d",         // equals but contains "."
                "http://a.de", "http://a.de/a",                       // superset path is null
                "https://127.0.0.1:46581", "https://127.0.0.1:465",   // port doesn't match
                "sdc.mds.pkp:1.2.840", "sdc.mds.pkp:a.2.840",         // scheme specifics not equal
                "sdc.mds.pkp:2.2.840.10004.20701.1.1", "sdc.mds.pkp", // scheme specifics is null
                "sdc.mds.pkp:a.2.840", "sdc.mds.pkp:A.2.840",         // case-sensitive scheme specifics
                "http://user:pass@a.de/abc", "http://a.de/abc",       // authority includes user info
                "http://f:fifty-two/c", "http://f:fifty-two/C"

        );
        notMatchingSubsets.forEach((superset, subset) -> assertFalse(doesMatch(superset, subset)));
    }

    private boolean doesMatchDefault(String subset) {
        return doesMatch(DEFAULT_SUPERSET, subset);
    }

    private boolean doesMatch(String superset, String subset) {
        return wsDiscoveryUtil.isScopesMatching(List.of(superset), List.of(subset), MatchBy.RFC3986);
    }
}