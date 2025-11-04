package it.org.somda.sdc.dpws.soap;

import it.org.somda.sdc.dpws.IntegrationTestUtil;
import it.org.somda.sdc.dpws.MockedUdpBindingModule;
import it.org.somda.sdc.dpws.TestServiceMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.dpws.device.DeviceConfig;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.SoapConfig;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.dpws.soap.wsmetadataexchange.GetMetadataClient;
import org.somda.sdc.dpws.soap.wsmetadataexchange.WsMetadataExchangeConstants;
import org.somda.sdc.dpws.soap.wsmetadataexchange.model.Metadata;
import org.somda.sdc.dpws.soap.wsmetadataexchange.model.MetadataSection;
import org.somda.sdc.dpws.wsdl.WsdlMarshalling;
import org.somda.sdc.dpws.wsdl.WsdlProvisioningMode;
import org.somda.sdc.dpws.wsdl.WsdlRetriever;
import test.org.somda.common.LoggingTestWatcher;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(LoggingTestWatcher.class)
class WsdlRetrievalIT {
    private static final Duration MAX_WAIT_TIME = IntegrationTestUtil.MAX_WAIT_TIME;

    private final IntegrationTestUtil IT = new IntegrationTestUtil();

    private BasicPopulatedDevice devicePeer;
    private ClientPeer clientPeer;

    private HostingServiceProxy hostingServiceProxy;
    private final SoapUtil soapUtil = IT.getInjector().getInstance(SoapUtil.class);

    WsdlRetrievalIT() {
        IntegrationTestUtil.preferIpV4Usage();
    }

    @AfterEach
    void tearDown() {
        this.devicePeer.stopAsync().awaitTerminated();
        this.clientPeer.stopAsync().awaitTerminated();
        this.devicePeer = null;
        this.clientPeer = null;
    }

    @Test
    void getWsdlAsResource() throws Exception {
        var metadataSection = setUpWithProvisioningMode(WsdlProvisioningMode.RESOURCE);
        assertNull(metadataSection.getAny());
        assertNotNull(metadataSection.getLocation());

        var retriever = clientPeer.getInjector().getInstance(WsdlRetriever.class);
        var wsdlMap = retriever.retrieveWsdls(hostingServiceProxy);
        assertFalse(wsdlMap.isEmpty());

        // BasicPopulatedDevice provides two WSDLs
        assertEquals(2, wsdlMap.values().stream().mapToLong(Collection::size).sum());

        var wsdlMarshaller = clientPeer.getInjector().getInstance(WsdlMarshalling.class);
        for (List<String> wsdlList : wsdlMap.values()) {
            for (String wsdl : wsdlList) {
                var tdef = wsdlMarshaller.unmarshal(new ByteArrayInputStream(
                        wsdl.getBytes(StandardCharsets.UTF_8)
                ));
                assertNotNull(tdef);
            }
        }
    }

    @Test
    void getWsdlAsInlineDefinition() throws Exception {
        var metadataSection = setUpWithProvisioningMode(WsdlProvisioningMode.INLINE);
        assertNotNull(metadataSection.getAny());
        assertNull(metadataSection.getLocation());

        var retriever = clientPeer.getInjector().getInstance(WsdlRetriever.class);
        var wsdlMap = retriever.retrieveWsdls(hostingServiceProxy);
        assertFalse(wsdlMap.isEmpty());

        // BasicPopulatedDevice provides two WSDLs
        assertEquals(2, wsdlMap.values().stream().mapToLong(Collection::size).sum());

        var wsdlMarshaller = clientPeer.getInjector().getInstance(WsdlMarshalling.class);
        for (List<String> wsdlList : wsdlMap.values()) {
            for (String wsdl : wsdlList) {
                var tdef = wsdlMarshaller.unmarshal(new ByteArrayInputStream(
                        wsdl.getBytes(StandardCharsets.UTF_8)
                ));
                assertNotNull(tdef);
            }
        }
    }

    private MetadataSection setUpWithProvisioningMode(WsdlProvisioningMode provisioningMode)
            throws InterceptorException, InterruptedException, ExecutionException, TimeoutException {
        devicePeer = new BasicPopulatedDevice(
                null,
                new DefaultDpwsConfigModule() {
                    @Override
                    public void customConfigure() {
                        bind(DeviceConfig.WSDL_PROVISIONING_MODE,
                                WsdlProvisioningMode.class,
                                provisioningMode);
                    }
                }, new MockedUdpBindingModule());
        clientPeer = new ClientPeer(
                new DefaultDpwsConfigModule() {
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

        var hostedServiceProxy = hostingServiceProxy.getHostedServices().values().iterator().next();
        var getMetadataClient = devicePeer.getInjector().getInstance(GetMetadataClient.class);
        var metadataFuture = getMetadataClient.sendGetMetadata(hostedServiceProxy);
        var soapMessage = metadataFuture.get(MAX_WAIT_TIME.toSeconds(), TimeUnit.SECONDS);
        var metadata = soapUtil.getBody(soapMessage, Metadata.class);
        assertTrue(metadata.isPresent());

        var sections = metadata.get().getMetadataSection().stream()
                .filter(metadataSection ->
                        WsMetadataExchangeConstants.DIALECT_WSDL.equals(metadataSection.getDialect()))
                .collect(Collectors.toList());
        assertEquals(1, sections.size());
        return sections.get(0);
    }
}
