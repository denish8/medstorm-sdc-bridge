package it.org.somda.sdc.dpws.soap;

import it.org.somda.sdc.dpws.IntegrationTestUtil;
import it.org.somda.sdc.dpws.MockedUdpBindingModule;
import it.org.somda.sdc.dpws.TestServiceMetadata;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.http.HttpException;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.SoapConfig;
import org.somda.sdc.dpws.soap.SoapConstants;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.factory.SoapFaultFactory;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingConstants;
import org.somda.sdc.dpws.soap.wsaddressing.model.AttributedQNameType;
import org.somda.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.somda.sdc.dpws.soap.wsaddressing.model.ProblemActionType;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants;
import org.somda.sdc.dpws.soap.wstransfer.WsTransferConstants;
import test.org.somda.common.LoggingTestWatcher;

import javax.xml.bind.JAXBElement;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(LoggingTestWatcher.class)
class FaultIT {
    private static final Duration MAX_WAIT_TIME = IntegrationTestUtil.MAX_WAIT_TIME;

    private final IntegrationTestUtil IT = new IntegrationTestUtil();

    private BasicPopulatedDevice devicePeer;
    private ClientPeer clientPeer;

    private HostingServiceProxy hostingServiceProxy;
    private final SoapUtil soapUtil = IT.getInjector().getInstance(SoapUtil.class);
    private final SoapFaultFactory soapFaultFactory = IT.getInjector().getInstance(SoapFaultFactory.class);

    FaultIT() {
        IntegrationTestUtil.preferIpV4Usage();
    }

    @BeforeEach
    void setUp() throws Exception {
        devicePeer = new BasicPopulatedDevice(new MockedUdpBindingModule());
        clientPeer = new ClientPeer(new DefaultDpwsConfigModule() {
            @Override
            public void customConfigure() {
                bind(SoapConfig.JAXB_CONTEXT_PATH, String.class,
                        TestServiceMetadata.JAXB_CONTEXT_PATH);
            }
        }, new MockedUdpBindingModule());
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        hostingServiceProxy = clientPeer.getClient().connect(devicePeer.getEprAddress())
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
    }

    @AfterEach
    void tearDown() {
        this.devicePeer.stopAsync().awaitTerminated();
        this.clientPeer.stopAsync().awaitTerminated();
        this.devicePeer = null;
        this.clientPeer = null;
    }

    @Test
    void invalidActions() throws Exception {
        final HostedServiceProxy srv1 = hostingServiceProxy.getHostedServices().get(TestServiceMetadata.SERVICE_ID_1);
        assertNotNull(srv1);

        {
            // Test missing action
            final SoapMessage reqMsg = soapUtil.createMessage();
            try {
                srv1.sendRequestResponse(reqMsg);
                fail("Expected a SoapFaultException to be thrown in case of a missing action");
            } catch (SoapFaultException e) {
                assertEquals(SoapConstants.SENDER, e.getFault().getCode().getValue());
                assertEquals(WsAddressingConstants.MESSAGE_ADDRESSING_HEADER_REQUIRED, e.getFault().getCode().getSubcode().getValue());
                assertEquals(1, e.getFault().getDetail().getAny().size());
                var detail = (JAXBElement<AttributedQNameType>) e.getFault().getDetail().getAny().get(0);
                assertEquals(WsAddressingConstants.QNAME_ACTION, detail.getValue().getValue());

                // Check HTTP status code
                assertNotNull(e.getCause());
                assertTrue(e.getCause() instanceof HttpException);
                assertEquals(HttpStatus.BAD_REQUEST_400, ((HttpException) e.getCause()).getStatusCode());
            }
        }

        {
            // Test unknown action
            var expectedUnknownAction = "http://unknown-action";
            final SoapMessage reqMsg = soapUtil.createMessage(expectedUnknownAction);
            try {
                srv1.sendRequestResponse(reqMsg);
                fail("Expected a SoapFaultException to be thrown in case of an unknown action");
            } catch (SoapFaultException e) {
                assertEquals(SoapConstants.SENDER, e.getFault().getCode().getValue());
                assertEquals(WsAddressingConstants.ACTION_NOT_SUPPORTED, e.getFault().getCode().getSubcode().getValue());
                assertEquals(1, e.getFault().getDetail().getAny().size());
                var detail = (JAXBElement<ProblemActionType>) e.getFault().getDetail().getAny().get(0);
                assertEquals(expectedUnknownAction, detail.getValue().getAction().getValue());

                // Check HTTP status code
                assertNotNull(e.getCause());
                assertTrue(e.getCause() instanceof HttpException);
                assertEquals(HttpStatus.BAD_REQUEST_400, ((HttpException) e.getCause()).getStatusCode());
            }
        }
    }

    @Test
    void faultMessageContainsRelatesTo() throws Exception {
        final HostedServiceProxy srv1 = hostingServiceProxy.getHostedServices().get(TestServiceMetadata.SERVICE_ID_1);
        assertNotNull(srv1);

        {
            // Test relatesTo is present, without messageId
            final SoapMessage reqMsg = soapUtil.createMessage();
            try {
                srv1.sendRequestResponse(reqMsg);
                fail("Expected a SoapFaultException to be thrown in case of a missing action");
            } catch (SoapFaultException e) {
                final var relatesTo = e.getFaultMessage().getWsAddressingHeader().getRelatesTo();
                assertTrue(relatesTo.isPresent());
                assertEquals(WsAddressingConstants.UNSPECIFIED_MESSAGE, relatesTo.get().getValue());
            }
        }

        {
            // Test relatesTo is present, with messageId
            final SoapMessage reqMsg = soapUtil.createMessage();
            final var msgId = new AttributedURIType();
            msgId.setValue(soapUtil.createRandomUuidUri());
            reqMsg.getWsAddressingHeader().setMessageId(msgId);
            try {
                srv1.sendRequestResponse(reqMsg);
                fail("Expected a SoapFaultException to be thrown in case of a missing action");
            } catch (SoapFaultException e) {
                final var relatesTo = e.getFaultMessage().getWsAddressingHeader().getRelatesTo();
                assertTrue(relatesTo.isPresent());
                assertEquals(msgId.getValue(), relatesTo.get().getValue());
            }
        }
    }
}
