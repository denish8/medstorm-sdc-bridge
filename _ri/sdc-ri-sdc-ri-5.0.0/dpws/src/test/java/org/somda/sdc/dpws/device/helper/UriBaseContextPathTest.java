package org.somda.sdc.dpws.device.helper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import test.org.somda.common.LoggingTestWatcher;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(LoggingTestWatcher.class)
class UriBaseContextPathTest {
    final String expectedUrlPath = "/context/path";
    final String expectedUrnUuidSpecificPart = "uuid:550e8400-e29b-11d4-a716-446655440000";
    final String expectedUrnOidSpecificPart = "oid:1.3.6.1.4.1";

    final URI testUrl = URI.create("http://www.examp.le" + expectedUrlPath);
    final URI testUuid = URI.create("urn:" + expectedUrnUuidSpecificPart);
    final URI testOid = URI.create("urn:" + expectedUrnOidSpecificPart);

    @Test
    void uriParts() {
        assertEquals(expectedUrlPath, testUrl.getPath());
        assertEquals(expectedUrnUuidSpecificPart, testUuid.getSchemeSpecificPart());
        assertEquals(expectedUrnOidSpecificPart, testOid.getSchemeSpecificPart());
    }

    @Test
    void basePathDerivation() {
        final String expectedUrlBasePath = expectedUrlPath.substring(1);
        final String expectedUuidBasePath = expectedUrnUuidSpecificPart.substring("uuid:".length());
        final String expectedOidBasePath = expectedUrnOidSpecificPart.substring("oid:".length());

        assertEquals(expectedUrlBasePath, new UriBaseContextPath(testUrl.toString()).get());
        assertEquals(expectedUuidBasePath, new UriBaseContextPath(testUuid.toString()).get());
        assertEquals(expectedOidBasePath, new UriBaseContextPath(testOid.toString()).get());

        assertEquals("", new UriBaseContextPath("urn:foo:bar:1234").get());
    }
}
