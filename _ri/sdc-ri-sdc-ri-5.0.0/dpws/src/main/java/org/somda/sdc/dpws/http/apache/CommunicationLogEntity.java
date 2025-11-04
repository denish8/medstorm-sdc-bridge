package org.somda.sdc.dpws.http.apache;

import org.apache.commons.io.input.TeeInputStream;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;
import org.somda.sdc.dpws.CommunicationLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Entity wrapper to enable {@linkplain CommunicationLog} capabilities in the http client.
 */
public class CommunicationLogEntity extends HttpEntityWrapper {
    private final OutputStream communicationLogStream;
    private InputStream content;

    /**
     * Creates a new entity wrapper.
     *
     * @param wrappedEntity          the entity to wrap.
     * @param communicationLogStream the stream to also write the content to.
     */
    public CommunicationLogEntity(HttpEntity wrappedEntity, OutputStream communicationLogStream) {
        super(wrappedEntity);
        this.communicationLogStream = communicationLogStream;
    }

    @Override
    public InputStream getContent() throws IOException {
        // From the Apache docs:
        // IMPORTANT: Please note all entity implementations must ensure that all allocated resources are properly
        // deallocated after the InputStream.close() method is invoked.

        if (wrappedEntity.isStreaming()) {
            // From the Apache docs:
            // Entities that are not repeatable are expected to return the same InputStream instance and therefore may
            // not be consumed more than once.
            if (content == null) {
                content = getWrappedStream();
            }
            return content;
        } else {
            // From the Apache docs:
            // Repeatable entities are expected to create a new instance of InputStream for each invocation of this
            // method and therefore can be consumed multiple times.
            return getWrappedStream();
        }
    }

    @Override
    public void writeTo(OutputStream outStream) throws IOException {
        var splitOutputStream = new TeeOutputStream(outStream, communicationLogStream);
        // From the Apache docs:
        // IMPORTANT: Please note all entity implementations must ensure that all allocated resources are properly
        // deallocated when this method returns.
        super.writeTo(splitOutputStream);
        communicationLogStream.close();
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    private InputStream getWrappedStream() throws IOException {
        // Always close the log stream together with the input stream (third param)
        return new TeeInputStream(wrappedEntity.getContent(), communicationLogStream, true);
    }
}
