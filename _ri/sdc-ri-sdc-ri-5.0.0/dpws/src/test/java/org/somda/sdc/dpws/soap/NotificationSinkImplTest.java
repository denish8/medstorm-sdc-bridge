package org.somda.sdc.dpws.soap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.helper.JaxbMarshalling;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.factory.NotificationSinkFactory;
import org.somda.sdc.dpws.soap.factory.SoapMessageFactory;
import org.somda.sdc.dpws.soap.interception.Direction;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.NotificationObject;
import org.somda.sdc.dpws.soap.interception.RequestObject;
import org.somda.sdc.dpws.soap.model.Envelope;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingServerInterceptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationSinkImplTest extends DpwsTest {
    private List<String> dispatchedSequence;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        getInjector().getInstance(JaxbMarshalling.class).startAsync().awaitRunning();
        getInjector().getInstance(SoapMarshalling.class).startAsync().awaitRunning();
        dispatchedSequence = new ArrayList<>();
    }

    @Test
    void receiveNotification() throws Exception {
        SoapMarshalling unmarshaller = getInjector().getInstance(SoapMarshalling.class);

        NotificationSink nSink = getInjector().getInstance(NotificationSinkFactory.class).createNotificationSink(
                getInjector().getInstance(WsAddressingServerInterceptor.class));
        Envelope soapEnv = unmarshaller.unmarshal(getClass().getResourceAsStream("soap-envelope.xml"));

        SoapMessageFactory soapMessageFactory = getInjector().getInstance(SoapMessageFactory.class);
        SoapMessage notification = soapMessageFactory.createSoapMessage(soapEnv);

        nSink.register(new Interceptor() {
            @MessageInterceptor(direction = Direction.NOTIFICATION)
            void onDelete(NotificationObject nInfo) {
                dispatchedSequence.add("NOTIFICATION(MAX)");
            }
        });

        nSink.register(new Interceptor() {
            @MessageInterceptor(direction = Direction.NOTIFICATION, sequenceNumber = 5)
            void onDelete(NotificationObject nInfo) {
                dispatchedSequence.add("NOTIFICATION(5)");
            }
        });

        nSink.register(new Interceptor() {
            @MessageInterceptor(value = "http://example.com/fabrikam/mail/Delete", direction = Direction.NOTIFICATION)
            void onDelete(NotificationObject nInfo) {
                dispatchedSequence.add("NOTIFICATION(ACTION, MAX)");
            }
        });

        // Shall be skipped since argument is missing
        nSink.register(new Interceptor() {
            @MessageInterceptor(value = "http://example.com/fabrikam/mail/Delete", direction = Direction.NOTIFICATION)
            void onDelete() {
                dispatchedSequence.add("INVALID NOTIFICATION(ACTION, MAX)");
            }
        });

        // Shall be skipped since direction is invalid
        nSink.register(new Interceptor() {
            @MessageInterceptor(direction = Direction.RESPONSE)
            void onDelete(NotificationObject nInfo) {
                dispatchedSequence.add("INVALID NOTIFICATION(MAX)");
            }
        });

        // Shall be skipped since parameter is Request, but should be Notification
        nSink.register(new Interceptor() {
            @MessageInterceptor(value = "http://example.com/fabrikam/mail/Delete", direction = Direction.NOTIFICATION)
            void onDelete(RequestObject rInfo) {
                dispatchedSequence.add("INVALID NOTIFICATION(ACTION, MAX)");
            }
        });

        nSink.register(new Interceptor() {
            @MessageInterceptor(value = "http://example.com/fabrikam/mail/Delete", direction = Direction.NOTIFICATION)
            void onDelete(NotificationObject nInfo) throws SoapFaultException {
                throw new SoapFaultException(nInfo.getNotification());
            }
        });

        var commMock = mock(CommunicationContext.class);
        var transportInfoMock = mock(TransportInfo.class);
        when(commMock.getTransportInfo()).thenReturn(transportInfoMock);
        when(transportInfoMock.getScheme()).thenReturn("any");


        // ensure the last interceptor throws a soap fault exception that is propagated properly
        assertThrows(SoapFaultException.class, () -> nSink.receiveNotification(notification, commMock));

        // but every other interceptor is still executed
        assertEquals(3, dispatchedSequence.size());
        assertEquals("NOTIFICATION(5)", dispatchedSequence.get(0));
        assertEquals("NOTIFICATION(MAX)", dispatchedSequence.get(1));
        assertEquals("NOTIFICATION(ACTION, MAX)", dispatchedSequence.get(2));
    }
}