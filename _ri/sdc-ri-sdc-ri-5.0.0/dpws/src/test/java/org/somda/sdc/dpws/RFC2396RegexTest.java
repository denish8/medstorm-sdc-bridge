package org.somda.sdc.dpws;

import jregex.Matcher;
import jregex.Pattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RFC2396RegexTest {
    private static final Pattern URI_REFERENCE_PATTERN = new Pattern(RFC2396Patterns.URI_REFERENCE);
    private static final Pattern RELATIVE_URI_PATTERN = new Pattern(RFC2396Patterns.RELATIVE_URI);
    private static final Pattern ABSOLUTE_URI_PATTERN = new Pattern(RFC2396Patterns.ABSOLUTE_URI);
    private static final Pattern AUTHORITY_PATTERN = new Pattern(RFC2396Patterns.AUTHORITY);
    private static final Pattern ABS_PATH_PATTERN = new Pattern(RFC2396Patterns.ABS_PATH);

    @Test
    void uriReference() {
        {
            Matcher matcher = URI_REFERENCE_PATTERN.matcher("scheme://user@%C3%A4:123/path?query?query#fragment");
            assertTrue(matcher.matches());
            assertEquals("scheme://user@%C3%A4:123/path?query?query", matcher.group("absoluteUri"));
            assertEquals("query?query", matcher.group("absoluteUriQuery"));
            assertEquals("fragment", matcher.group("fragment"));
            assertNull(matcher.group("relativeUri"));
        }
        {
            Matcher matcher = URI_REFERENCE_PATTERN.matcher("scheme://user@host:123/path?query?query#fragment");
            assertTrue(matcher.matches());
            assertEquals("scheme://user@host:123/path?query?query", matcher.group("absoluteUri"));
            assertEquals("query?query", matcher.group("absoluteUriQuery"));
            assertEquals("fragment", matcher.group("fragment"));
            assertNull(matcher.group("relativeUri"));
        }
        {
            Matcher matcher = URI_REFERENCE_PATTERN.matcher("scheme:aaahhh??");
            assertTrue(matcher.matches());
            assertEquals("scheme:aaahhh??", matcher.group("absoluteUri"));
            assertNull(matcher.group("relativeUri"));
        }
        {
            Matcher matcher = URI_REFERENCE_PATTERN.matcher("/path?query?query#fragment");
            assertTrue(matcher.matches());
            assertEquals("/path?query?query", matcher.group("relativeUri"));
            assertEquals("query?query", matcher.group("relativeUriQuery"));
            assertEquals("fragment", matcher.group("fragment"));
            assertNull(matcher.group("absoluteUri"));
        }
        {
            Matcher matcher = URI_REFERENCE_PATTERN.matcher("//user@C3A4:123/path?query?query#fragment");
            assertTrue(matcher.matches());
            assertEquals("//user@C3A4:123/path?query?query", matcher.group("relativeUri"));
            assertEquals("query?query", matcher.group("relativeUriQuery"));
            assertEquals("fragment", matcher.group("fragment"));
            assertNull(matcher.group("absoluteUri"));
        }
    }

    @Test
    void absoluteUri() {
        {
            Matcher matcher = ABSOLUTE_URI_PATTERN.matcher("scheme://user@C3A4:123/path?query?query");
            assertTrue(matcher.matches());
            assertEquals("scheme://user@C3A4:123/path?query?query", matcher.group("absoluteUri"));
            assertEquals("query?query", matcher.group("absoluteUriQuery"));
        }
        {
            Matcher matcher = ABSOLUTE_URI_PATTERN.matcher("scheme:aaahhh??");
            assertTrue(matcher.matches());
            assertEquals("scheme:aaahhh??", matcher.group("absoluteUri"));
        }
        {
            Matcher matcher = ABSOLUTE_URI_PATTERN.matcher("scheme://@@");
            matcher.matches();
            assertEquals("scheme://@@", matcher.group("absoluteUri"));
        }
        {
            Matcher matcher = ABSOLUTE_URI_PATTERN.matcher("scheme://user@C3A4:123/path?#?query#fragment");
            assertFalse(matcher.matches());
        }
        {
            Matcher matcher = ABSOLUTE_URI_PATTERN.matcher("urn:example:animal:ferret:nose");
            assertTrue(matcher.matches());
            assertEquals("urn:example:animal:ferret:nose", matcher.group("absoluteUri"));
            assertEquals("urn", matcher.group("scheme"));
        }
        {
            Matcher matcher = ABSOLUTE_URI_PATTERN.matcher("urn:%2A-");
            assertTrue(matcher.matches());
            assertEquals("urn:%2A-", matcher.group("absoluteUri"));
            assertEquals("urn", matcher.group("scheme"));
        }
    }

    @Test
    void relativeUri() {
        {
            Matcher matcher = RELATIVE_URI_PATTERN.matcher("/path?query?query");
            assertTrue(matcher.matches());
            assertEquals("/path?query?query", matcher.group("relativeUri"));
            assertEquals("query?query", matcher.group("relativeUriQuery"));
        }
        {
            Matcher matcher = RELATIVE_URI_PATTERN.matcher("//user@C3A4:123/path?query?query");
            assertTrue(matcher.matches());
            assertEquals("//user@C3A4:123/path?query?query", matcher.group("relativeUri"));
            assertEquals("query?query", matcher.group("relativeUriQuery"));
        }
        {
            Matcher matcher = RELATIVE_URI_PATTERN.matcher("*");
            assertTrue(matcher.matches());
            assertEquals("*", matcher.group("relativeUri"));
        }
    }

    @Test
    void authority() {
        {
            Matcher matcher = AUTHORITY_PATTERN.matcher("user@%C3%A4:123");
            assertTrue(matcher.matches());
            assertEquals("user@%C3%A4:123", matcher.group(0));
        }
        {
            Matcher matcher = AUTHORITY_PATTERN.matcher("user@host:123");
            assertTrue(matcher.matches());
            assertEquals("user@host:123", matcher.group(0));
        }
        {
            Matcher matcher = AUTHORITY_PATTERN.matcher("@@");
            assertTrue(matcher.matches());
            assertEquals("@@", matcher.group(0));
        }
        {
            Matcher matcher = AUTHORITY_PATTERN.matcher("scheme://user@C3A4:123/path?query?query");
            assertFalse(matcher.matches());
        }
        {
            Matcher matcher = AUTHORITY_PATTERN.matcher("/path");
            assertFalse(matcher.matches());
        }
    }

    @Test
    void absPath() {
        {
            Matcher matcher = ABS_PATH_PATTERN.matcher("/path");
            assertTrue(matcher.matches());
            assertEquals("/path", matcher.group(0));
        }
        {
            Matcher matcher = ABS_PATH_PATTERN.matcher("/path/path/path");
            assertTrue(matcher.matches());
            assertEquals("/path/path/path", matcher.group(0));
        }
        {
            Matcher matcher = ABS_PATH_PATTERN.matcher("/path/path/path?query");
            assertFalse(matcher.matches());
        }
        {
            Matcher matcher = ABS_PATH_PATTERN.matcher("/path/path/path#fragment");
            assertFalse(matcher.matches());
        }
        {
            Matcher matcher = ABS_PATH_PATTERN.matcher("scheme://user@C3A4:123/path");
            assertFalse(matcher.matches());
        }
    }
}
