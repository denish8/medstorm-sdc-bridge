package org.somda.sdc.dpws.soap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.helper.JaxbMarshalling;
import org.somda.sdc.dpws.soap.factory.NotificationSourceFactory;
import org.somda.sdc.dpws.soap.factory.SoapMessageFactory;
import org.somda.sdc.dpws.soap.interception.Direction;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.NotificationObject;
import org.somda.sdc.dpws.soap.interception.RequestObject;
import org.somda.sdc.dpws.soap.model.Envelope;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationSourceImplTest extends DpwsTest {
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
    void sendNotification() throws Exception {
        SoapMarshalling unmarshaller = getInjector().getInstance(SoapMarshalling.class);
        Envelope soapEnv = unmarshaller.unmarshal(getClass().getResourceAsStream("soap-envelope.xml"));

        SoapMessageFactory soapMessageFactory = getInjector().getInstance(SoapMessageFactory.class);
        SoapMessage notification = soapMessageFactory.createSoapMessage(soapEnv);

        NotificationSourceFactory clientFactory = getInjector().getInstance(NotificationSourceFactory.class);
        NotificationSource nSource = clientFactory.createNotificationSource(not -> dispatchedSequence.add("NETWORK"));

        nSource.register(new Interceptor() {
            @MessageInterceptor(direction = Direction.NOTIFICATION)
            void onDelete(NotificationObject nInfo) {
                dispatchedSequence.add("NOTIFICATION(MAX)");
            }
        });

        nSource.register(new Interceptor() {
            @MessageInterceptor(direction = Direction.NOTIFICATION, sequenceNumber = 5)
            void onDelete(NotificationObject nInfo) {
                dispatchedSequence.add("NOTIFICATION(5)");
            }
        });

        nSource.register(new Interceptor() {
            @MessageInterceptor(value = "http://example.com/fabrikam/mail/Delete", direction = Direction.NOTIFICATION)
            void onDelete(NotificationObject nInfo) {
                dispatchedSequence.add("NOTIFICATION(ACTION, MAX)");
            }
        });

        // Shall be skipped since argument is missing
        nSource.register(new Interceptor() {
            @MessageInterceptor(value = "http://example.com/fabrikam/mail/Delete", direction = Direction.NOTIFICATION)
            void onDelete() {
                dispatchedSequence.add("INVALID NOTIFICATION(ACTION, MAX)");
            }
        });

        // Shall be skipped since argument is Request, but should be Notification
        nSource.register(new Interceptor() {
            @MessageInterceptor(value = "http://example.com/fabrikam/mail/Delete", direction = Direction.NOTIFICATION)
            void onDelete(RequestObject rInfo) {
                dispatchedSequence.add("INVALID NOTIFICATION(ACTION, MAX)");
            }
        });

        // Shall be skipped since direction is Request, but should be Notification
        nSource.register(new Interceptor() {
            @MessageInterceptor(value = "http://example.com/fabrikam/mail/Delete", direction = Direction.REQUEST)
            void onDelete(NotificationObject nInfo) {
                dispatchedSequence.add("INVALID NOTIFICATION(ACTION, MAX)");
            }
        });

        nSource.sendNotification(notification);

        assertEquals(4, dispatchedSequence.size());
        assertEquals("NOTIFICATION(5)", dispatchedSequence.get(0));
        assertEquals("NOTIFICATION(MAX)", dispatchedSequence.get(1));
        assertEquals("NOTIFICATION(ACTION, MAX)", dispatchedSequence.get(2));
        assertEquals("NETWORK", dispatchedSequence.get(3));
    }
}