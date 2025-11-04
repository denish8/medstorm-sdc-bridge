package org.somda.sdc.glue.common.uri;

import jregex.Matcher;
import jregex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.glue.GlueConstants;
import test.org.somda.common.LoggingTestWatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(LoggingTestWatcher.class)
class RegexTest {

    private static final Pattern AUTHORITY_PATTERN = new Pattern("^" + GlueConstants.AUTHORITY + "$");
    private static final Pattern URI_PATTERN = new Pattern(GlueConstants.URI_REGEX);

    private static final Pattern SEGMENT_PATTERN = new Pattern(GlueConstants.AUTHORITY);
    private static final Pattern IPV4_PATTERN = new Pattern(GlueConstants.IPV4_ADDRESS);

    @Test
    void segment() {
        {
            Matcher matcher = SEGMENT_PATTERN.matcher("a%2A-&+");
            assertTrue(matcher.matches());
        }
    }

    @Test
    void ipv4() {
        {
            Matcher matcher = IPV4_PATTERN.matcher("192x168y15z42");
            assertFalse(matcher.matches());
        }
        {
            Matcher matcher = IPV4_PATTERN.matcher("192.168.15.42");
            assertTrue(matcher.matches());
        }
    }

    @Test
    void authority() {
        {
            Matcher matcher = AUTHORITY_PATTERN.matcher("@host:");
            assertTrue(matcher.matches());
            assertEquals("", matcher.group("userInfo"));
            assertEquals("", matcher.group("port"));
            assertEquals("host", matcher.group("host"));
        }
        {
            Matcher matcher = AUTHORITY_PATTERN.matcher("user@host:123");
            assertTrue(matcher.matches());
            assertEquals("user", matcher.group("userInfo"));
            assertEquals("123", matcher.group("port"));
            assertEquals("host", matcher.group("host"));
        }
        {
            Matcher matcher = AUTHORITY_PATTERN.matcher("user@[10೬0:0:0:0:8:800:200C:417A]:123");
            assertFalse(matcher.matches());
        }
        {
            Matcher matcher = AUTHORITY_PATTERN.matcher("user@[1060:0:0:0:8:800:200C:417A]:123");
            assertTrue(matcher.matches());
            assertEquals("user", matcher.group("userInfo"));
            assertEquals("123", matcher.group("port"));
            assertEquals("[1060:0:0:0:8:800:200C:417A]", matcher.group("host"));
        }
        {
            Matcher matcher = AUTHORITY_PATTERN.matcher("user@%C3%A4:123");
            assertTrue(matcher.matches());
            assertEquals("user", matcher.group("userInfo"));
            assertEquals("123", matcher.group("port"));
            assertEquals("%C3%A4", matcher.group("host"));
        }
        {
            Matcher matcher = AUTHORITY_PATTERN.matcher("user@@:123");
            assertFalse(matcher.matches());
        }
    }

    @Test
    void uri() {
        {
            Matcher matcher = URI_PATTERN.matcher("scheme://@@");
            matcher.matches();
            assertFalse(matcher.matches());
        }
        {
            Matcher matcher = URI_PATTERN.matcher("scheme://user@%C3%A4:123/path?query?query#fragment");
            assertTrue(matcher.matches());
            assertEquals("user", matcher.group("userInfo"));
            assertEquals("123", matcher.group("port"));
            assertEquals("%C3%A4", matcher.group("host"));
            assertEquals("/path", matcher.group("path"));
            assertEquals("query?query", matcher.group("query"));
            assertEquals("fragment", matcher.group("fragment"));
        }
        {
            Matcher matcher = URI_PATTERN.matcher("scheme://user@%C3%A4:123/path?#?query#fragment");
            assertFalse(matcher.matches());
        }
        {
            Matcher matcher = URI_PATTERN.matcher("urn:example:animal:ferret:nose");
            assertTrue(matcher.matches());
            assertEquals("example:animal:ferret:nose", matcher.group("path"));
        }
        {
            Matcher matcher = URI_PATTERN.matcher("urn:%2A-");
            assertTrue(matcher.matches());
            assertEquals("%2A-", matcher.group("path"));
        }
    }
}
