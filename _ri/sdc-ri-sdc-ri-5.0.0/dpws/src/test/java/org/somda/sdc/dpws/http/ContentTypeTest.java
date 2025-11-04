package org.somda.sdc.dpws.http;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.http.HttpHeaders;
import org.apache.http.message.BasicHeader;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContentTypeTest {

    @Test
    void testFromListMultimap() {
        ListMultimap<String, String> data = ArrayListMultimap.create();
        // these are transformed to lower case for HttpApplicationContext
        var contentType = HttpHeaders.CONTENT_TYPE.toLowerCase();
        {
            data.put(contentType, "text/xml");
            var contentOpt = ContentType.fromListMultimap(data);
            assertTrue(contentOpt.isPresent());
            var content = contentOpt.get();
            assertEquals(ContentType.ContentTypes.TEXT_XML, content.getContentType());
            assertEquals(StandardCharsets.ISO_8859_1, content.getCharset());
        }
        data.clear();
        {
            var boundary = "stuff";
            data.put(contentType, "text/xml; charset=us-ascii; boundary=" + boundary);
            var contentOpt = ContentType.fromListMultimap(data);
            assertTrue(contentOpt.isPresent());
            var content = contentOpt.get();
            assertEquals(ContentType.ContentTypes.TEXT_XML, content.getContentType());
            assertEquals(StandardCharsets.US_ASCII, content.getCharset());
            assertEquals(boundary, content.getBoundary());
        }
    }

    @Test
    void testFromApache() {
        {
            var header = new BasicHeader(HttpHeaders.CONTENT_TYPE, "text/xml");
            var contentOpt = ContentType.fromApache(header);
            assertTrue(contentOpt.isPresent());
            var content = contentOpt.get();
            assertEquals(ContentType.ContentTypes.TEXT_XML, content.getContentType());
            assertEquals(StandardCharsets.ISO_8859_1, content.getCharset());
        }
        {
            var boundary = "stuff";
            var header = new BasicHeader(HttpHeaders.CONTENT_TYPE, "text/xml; charset=us-ascii; boundary=" + boundary);
            var contentOpt = ContentType.fromApache(header);
            assertTrue(contentOpt.isPresent());
            var content = contentOpt.get();
            assertEquals(ContentType.ContentTypes.TEXT_XML, content.getContentType());
            assertEquals(StandardCharsets.US_ASCII, content.getCharset());
            assertEquals(boundary, content.getBoundary());
        }
    }

}
