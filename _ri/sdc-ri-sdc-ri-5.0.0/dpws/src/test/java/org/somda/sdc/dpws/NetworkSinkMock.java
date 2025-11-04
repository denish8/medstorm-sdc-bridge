package org.somda.sdc.dpws;

import com.google.inject.Inject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A dummy network sink to write output streams and read them from a message cache as input streams.
 */
public class NetworkSinkMock {
    private final List<InputStream> writtenMessages;

    @Inject
    NetworkSinkMock() {
        this.writtenMessages = new ArrayList<>();
    }

    public synchronized OutputStream createOutputStream() {
        return new ByteArrayOutputStream() {
            @Override
            public void close() {
                writtenMessages.add(new ByteArrayInputStream(toByteArray()));
            }
        };
    }

    public synchronized List<InputStream> getWrittenMessages() {
        return writtenMessages;
    }

    public synchronized InputStream getLatest() {
        return writtenMessages.get(writtenMessages.size() - 1);
    }
}
