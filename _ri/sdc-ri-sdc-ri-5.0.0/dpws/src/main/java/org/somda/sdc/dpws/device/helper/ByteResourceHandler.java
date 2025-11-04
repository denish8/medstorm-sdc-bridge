package org.somda.sdc.dpws.device.helper;

import org.eclipse.jetty.http.HttpStatus;
import org.somda.sdc.dpws.http.HttpException;
import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.soap.CommunicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * HTTP handler that facilitates responding with arbitrary byte sequences.
 */
public class ByteResourceHandler implements HttpHandler {
    private final byte[] resourceBytes;

    /**
     * Constructor.
     *
     * @param resourceBytes the bytes that are supposed to be returned on any incoming network request.
     */
    public ByteResourceHandler(byte[] resourceBytes) {
        this.resourceBytes = Arrays.copyOf(resourceBytes, resourceBytes.length);
    }

    @Override
    public void handle(InputStream inStream, OutputStream outStream, CommunicationContext communicationContext)
            throws HttpException {
        try {
            outStream.write(resourceBytes);
        } catch (IOException e) {
            throw new HttpException(HttpStatus.NOT_FOUND_404, e.getMessage());
        }
    }
}