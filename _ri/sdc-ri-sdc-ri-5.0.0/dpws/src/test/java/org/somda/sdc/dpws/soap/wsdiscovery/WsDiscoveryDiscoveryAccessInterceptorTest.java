package org.somda.sdc.dpws.soap.wsdiscovery;

import com.google.common.primitives.UnsignedInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.dpws.CommunicationLogContext;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.helper.JaxbMarshalling;
import org.somda.sdc.dpws.soap.ApplicationInfo;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.NotificationSource;
import org.somda.sdc.dpws.soap.RequestResponseServer;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.TransportInfo;
import org.somda.sdc.dpws.soap.factory.NotificationSourceFactory;
import org.somda.sdc.dpws.soap.factory.SoapMessageFactory;
import org.somda.sdc.dpws.soap.model.Envelope;
import org.somda.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wsaddressing.model.ObjectFactory;
import org.somda.sdc.dpws.soap.wsdiscovery.factory.WsDiscoveryTargetServiceFactory;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ByeType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.HelloType;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class WsDiscoveryDiscoveryAccessInterceptorTest extends DpwsTest {
    private List<SoapMessage> sentSoapMessages;
    private WsDiscoveryTargetService wsDiscoveryTargetService;
    private QName expectedType;
    private String expectedScope;
    private String expectedXAddr;
    private List<QName> expectedTypes;
    private List<String> expectedScopes;
    private List<String> expectedXAddrs;
    private SoapUtil soapUtil;
    private SoapMarshalling unmarshaller;
    private RequestResponseServer reqResServer;
    private SoapMessageFactory soapMessageFactory;
    private CommunicationContext mockCommunicationContext;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        mockCommunicationContext = new CommunicationContext(
                new ApplicationInfo(),
                new TransportInfo(
                        "mock.scheme",
                        "localhost",
                        123,
                        "remotehost",
                        456,
                        Collections.emptyList()
                ),
                new CommunicationLogContext("txetnoCgoLnoitacinummoC")
        );

        sentSoapMessages = new ArrayList<>();

        soapMessageFactory = getInjector().getInstance(SoapMessageFactory.class);
        NotificationSourceFactory nSourceFactory = getInjector().getInstance(NotificationSourceFactory.class);
        NotificationSource notificationSource = nSourceFactory.createNotificationSource(notification ->
                sentSoapMessages.add(notification));

        WsDiscoveryTargetServiceFactory wsdTargetServiceFactory = getInjector()
                .getInstance(WsDiscoveryTargetServiceFactory.class);

        soapUtil = getInjector().getInstance(SoapUtil.class);

        ObjectFactory wsaFactory = getInjector().getInstance(ObjectFactory.class);
        EndpointReferenceType expectedEpr = wsaFactory.createEndpointReferenceType();
        AttributedURIType eprUri = wsaFactory.createAttributedURIType();
        eprUri.setValue("http://expectedEpr-uri");
        expectedEpr.setAddress(eprUri);

        wsDiscoveryTargetService = wsdTargetServiceFactory.createWsDiscoveryTargetService(expectedEpr,
                notificationSource);
        expectedType = new QName("http://namespace", "type");
        expectedScope = "http://namespace/scope";
        expectedXAddr = "http://1.2.3.4";

        expectedTypes = new ArrayList<>();
        expectedTypes.add(expectedType);
        wsDiscoveryTargetService.setTypes(expectedTypes);

        expectedScopes = new ArrayList<>();
        expectedScopes.add(expectedScope);
        wsDiscoveryTargetService.setScopes(expectedScopes);

        expectedXAddrs = new ArrayList<>();
        expectedXAddrs.add(expectedXAddr);
        wsDiscoveryTargetService.setXAddrs(expectedXAddrs);

        reqResServer = getInjector().getInstance(RequestResponseServer.class);
        reqResServer.register(wsDiscoveryTargetService);

        getInjector().getInstance(JaxbMarshalling.class).startAsync().awaitRunning();
        unmarshaller = getInjector().getInstance(SoapMarshalling.class);
        unmarshaller.startAsync().awaitRunning();
    }

    @Test
    void sendHello() throws Exception {
        UnsignedInteger version1 = wsDiscoveryTargetService.getMetadataVersion();

        UnsignedInteger version2 = wsDiscoveryTargetService.sendHello();
        assertNotEquals(version1.longValue(), version2.longValue());

        UnsignedInteger version3 = wsDiscoveryTargetService.sendHello();
        assertEquals(version2, version3);

        UnsignedInteger version4 = wsDiscoveryTargetService.sendHello(true);
        assertNotEquals(version2, version4);

        assertEquals(3, sentSoapMessages.size());

        sentSoapMessages.forEach(message -> {
            Optional<HelloType> body = soapUtil.getBody(message, HelloType.class);
            HelloType actualHello = body.orElseThrow(() -> new RuntimeException("SOAP message body malformed"));
            assertEquals(expectedTypes.size(), actualHello.getTypes().size());
            assertEquals(expectedType, actualHello.getTypes().get(0));
            assertEquals(expectedScopes.size(), actualHello.getScopes().getValue().size());
            assertEquals(expectedScope, actualHello.getScopes().getValue().get(0));
            assertEquals(expectedXAddrs.size(), actualHello.getXAddrs().size());
            assertEquals(expectedXAddr, actualHello.getXAddrs().get(0));
        });
    }

    @Test
    void sendBye() throws Exception {
        wsDiscoveryTargetService.sendBye();
        assertEquals(1, sentSoapMessages.size());
        Optional<ByeType> body = soapUtil.getBody(sentSoapMessages.get(0), ByeType.class);
        ByeType actualBye = body.orElseThrow(() -> new RuntimeException("SOAP message body malformed"));
        assertEquals(expectedTypes.size(), actualBye.getTypes().size());
        assertEquals(expectedType, actualBye.getTypes().get(0));
        assertEquals(expectedScopes.size(), actualBye.getScopes().getValue().size());
        assertEquals(expectedScope, actualBye.getScopes().getValue().get(0));
        assertEquals(expectedXAddrs.size(), actualBye.getXAddrs().size());
        assertEquals(expectedXAddr, actualBye.getXAddrs().get(0));
    }

    @Test
    void processProbe() throws Exception {
        Envelope soapEnv = unmarshaller.unmarshal(getClass().getResourceAsStream("probe-message.xml"));
        SoapMessage probe = soapMessageFactory.createSoapMessage(soapEnv);

        SoapMessage response = soapUtil.createMessage();
        assertEquals(0, response.getOriginalEnvelope().getBody().getAny().size());
        reqResServer.receiveRequestResponse(probe, response, mockCommunicationContext);
        assertEquals(1, response.getOriginalEnvelope().getBody().getAny().size());
    }

    @Test
    void processResolve() throws Exception {
        Envelope soapEnv = unmarshaller.unmarshal(getClass().getResourceAsStream("resolve-message.xml"));
        SoapMessage resolve = soapMessageFactory.createSoapMessage(soapEnv);

        SoapMessage response = soapUtil.createMessage();
        assertEquals(0, response.getOriginalEnvelope().getBody().getAny().size());
        reqResServer.receiveRequestResponse(resolve, response, mockCommunicationContext);
        assertEquals(1, response.getOriginalEnvelope().getBody().getAny().size());
    }
}