package org.somda.sdc.dpws;

import org.junit.jupiter.api.Test;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.TransportInfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunicationLogImplTest extends DpwsTest {

    static class DummyOutputStream extends OutputStream {

        private ByteArrayOutputStream baos;

        int closed;

        DummyOutputStream() {
            reset();
        }

        @Override
        public void write(int b) throws IOException {
            baos.write(b);
        }

        @Override
        public void close() {
            closed++;
        }

        public byte[] toByteArray() {
            return baos.toByteArray();
        }

        public int getClosed() {
            return closed;
        }

        void reset() {
            closed = 0;
            baos = new ByteArrayOutputStream();
        }
    }


    @Test
    void content() throws IOException {

        CommunicationLogSinkImpl communicationLogSinkImplMock = mock(CommunicationLogSinkImpl.class);

        byte[] content = UUID.randomUUID().toString().getBytes();

        try (DummyOutputStream mockOutputStream = new DummyOutputStream();
             ByteArrayInputStream inputTestInputStream = new ByteArrayInputStream(content);
             ByteArrayOutputStream outputTestOutputStream = new ByteArrayOutputStream()) {

            when(communicationLogSinkImplMock.createTargetStream(eq(CommunicationLog.TransportType.HTTP), any(), any(), any()))
                    .thenReturn(mockOutputStream);

            CommunicationLogImpl communicationLogImpl = new CommunicationLogImpl(communicationLogSinkImplMock, "abcd");

            var requestCommContext = new CommunicationContext(
                    null,
                    new TransportInfo(
                            "",
                            null, null,
                            "_", 0,
                            Collections.emptyList()
                    ),
                    null
            );

            InputStream resultingInputStream = communicationLogImpl
                    .logMessage(CommunicationLog.Direction.OUTBOUND, CommunicationLog.TransportType.HTTP,
                            CommunicationLog.MessageType.UNKNOWN,
                            requestCommContext, inputTestInputStream);

            assertArrayEquals(resultingInputStream.readAllBytes(), content);
            assertArrayEquals(mockOutputStream.toByteArray(), content);
            assertEquals(1, mockOutputStream.getClosed());

            mockOutputStream.reset();

            try (OutputStream resultingOutputStream = communicationLogImpl.logMessage(
                    CommunicationLog.Direction.OUTBOUND, CommunicationLog.TransportType.HTTP,
                    CommunicationLog.MessageType.UNKNOWN,
                    requestCommContext, outputTestOutputStream);) {

                resultingOutputStream.write(content);
                resultingOutputStream.flush();
            }

            assertArrayEquals(outputTestOutputStream.toByteArray(), content);
            assertArrayEquals(mockOutputStream.toByteArray(), content);
        }
    }

    @Test
    void branchPath() throws IOException {

        byte[] content = UUID.randomUUID().toString().getBytes();

        CommunicationLogSinkImpl communicationLogSinkImplMock = mock(CommunicationLogSinkImpl.class);

        CommunicationLogImpl communicationLogImpl = new CommunicationLogImpl(communicationLogSinkImplMock, "abcd");

        var requestCommContext = new CommunicationContext(
                null,
                new TransportInfo(
                        "",
                        null, null,
                        "_", 0,
                        Collections.emptyList()
                ),
                null
        );

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content);) {
            for (CommunicationLog.Direction dir : CommunicationLog.Direction.values()) {
                reset(communicationLogSinkImplMock);

                when(communicationLogSinkImplMock.createTargetStream(any(CommunicationLog.TransportType.class), any(), any(), any()))
                        .thenReturn(OutputStream.nullOutputStream());

                communicationLogImpl.logMessage(dir, CommunicationLog.TransportType.HTTP, CommunicationLog.MessageType.UNKNOWN,
                        requestCommContext,
                        inputStream);
                communicationLogImpl.logMessage(dir, CommunicationLog.TransportType.HTTP, CommunicationLog.MessageType.UNKNOWN,
                        requestCommContext,
                        OutputStream.nullOutputStream());
                verify(communicationLogSinkImplMock, times(2)).
                        createTargetStream(eq(CommunicationLog.TransportType.HTTP), any(), any(), any());
            }
        }
    }
}
