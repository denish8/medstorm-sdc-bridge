package org.somda.sdc.dpws.client.helper;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.LocalAddressResolverMock;
import org.somda.sdc.dpws.ThisDeviceBuilder;
import org.somda.sdc.dpws.ThisModelBuilder;
import org.somda.sdc.dpws.client.DiscoveredDevice;
import org.somda.sdc.dpws.client.exception.EprAddressMismatchException;
import org.somda.sdc.dpws.guice.ResolverThreadPool;
import org.somda.sdc.dpws.model.HostedServiceType;
import org.somda.sdc.dpws.model.ThisDeviceType;
import org.somda.sdc.dpws.model.ThisModelType;
import org.somda.sdc.dpws.network.LocalAddressResolver;
import org.somda.sdc.dpws.service.HostedService;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.service.factory.HostingServiceFactory;
import org.somda.sdc.dpws.service.helper.MetadataSectionUtil;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.factory.RequestResponseClientFactory;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wsaddressing.model.ReferenceParametersType;
import org.somda.sdc.dpws.soap.wsmetadataexchange.GetMetadataClient;
import org.somda.sdc.dpws.soap.wsmetadataexchange.model.Metadata;
import org.somda.sdc.dpws.soap.wsmetadataexchange.model.MetadataSection;
import org.somda.sdc.dpws.soap.wstransfer.TransferGetClient;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.Collection;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HostingServiceResolverTest extends DpwsTest {
    private MockTransferGetClient mockTransferGetClient;
    private MockGetMetadataClient mockGetMetadataClient;

    private String expectedDeviceEprAddress;
    private List<QName> expectedHostingServiceQNameTypes;
    private String expectedSerialNumber;
    private ThisDeviceType expectedDeviceType;
    private String expectedModelNumber;
    private ThisModelType expectedModelType;

    private String expectedServiceId;
    private List<EndpointReferenceType> expectedHostedServiceEprs;
    private List<QName> expectedHostedServiceQNameTypes;

    private HostingServiceFactory hostingServiceFactory;
    private org.somda.sdc.dpws.soap.wsmetadataexchange.model.ObjectFactory mexFactory;
    private org.somda.sdc.dpws.model.ObjectFactory dpwsFactory;
    private MetadataSectionUtil metadataSectionUtil;
    private WsAddressingUtil wsaUtil;
    private SoapUtil soapUtil;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        mockTransferGetClient = new MockTransferGetClient();
        mockGetMetadataClient = new MockGetMetadataClient();
        overrideBindings(new AbstractModule() {
            @Override
            protected void configure() {
                bind(RequestResponseClientFactory.class).toInstance(callback -> mock(RequestResponseClient.class));
                bind(TransferGetClient.class).toInstance(mockTransferGetClient);
                bind(GetMetadataClient.class).toInstance(mockGetMetadataClient);
                bind(LocalAddressResolver.class).to(LocalAddressResolverMock.class);
            }
        });
        super.setUp();

        // start required thread pool(s)
        getInjector().getInstance(Key.get(
                new TypeLiteral<ExecutorWrapperService<ListeningExecutorService>>() {
                },
                ResolverThreadPool.class
        )).startAsync().awaitRunning();

        wsaUtil = getInjector().getInstance(WsAddressingUtil.class);
        soapUtil = getInjector().getInstance(SoapUtil.class);
        metadataSectionUtil = getInjector().getInstance(MetadataSectionUtil.class);
        dpwsFactory = getInjector().getInstance(org.somda.sdc.dpws.model.ObjectFactory.class);
        mexFactory = getInjector().getInstance(org.somda.sdc.dpws.soap.wsmetadataexchange.model.ObjectFactory.class);

        hostingServiceFactory = getInjector().getInstance(HostingServiceFactory.class);

        expectedDeviceEprAddress = "urn:uuid:71c219ae-3b55-404f-803b-1e72390f73ba";
        expectedHostingServiceQNameTypes = Arrays.asList(new QName("http://device", "Type1"),
                new QName("http://device", "Type2"));

        expectedSerialNumber = "1234-5678-9101-2131";
        ThisDeviceBuilder tdBuilder = getInjector().getInstance(ThisDeviceBuilder.class);
        expectedDeviceType = tdBuilder.setSerialNumber(expectedSerialNumber).get();
        expectedModelNumber = "0815";
        ThisModelBuilder tmBuilder = getInjector().getInstance(ThisModelBuilder.class);
        expectedModelType = tmBuilder.setModelNumber(expectedModelNumber).get();

        expectedServiceId = "Service1";
        expectedHostedServiceEprs = Arrays.asList(wsaUtil.createEprWithAddress("http://hosted-service-epr1"),
                wsaUtil.createEprWithAddress("http://hosted-service-epr2"));
        expectedHostedServiceQNameTypes = Arrays.asList(new QName("http://service", "Type1"),
                new QName("http://service", "Type2"));
        HostedServiceType expectedHostedServiceType = dpwsFactory.createHostedServiceType();
        expectedHostedServiceType.setEndpointReference(expectedHostedServiceEprs);
        expectedHostedServiceType.setServiceId(expectedServiceId);
        expectedHostedServiceType.setTypes(expectedHostedServiceQNameTypes);
    }

    @Test
    void resolveHostingService() {
        // When no existing service is found in registry on resolving
        // Then expect the resolver to resolve the service according to the following message
        mockTransferGetClient.setTransferGetMessages(List.of(createTransferGetMessage(
                expectedDeviceEprAddress,
                expectedHostingServiceQNameTypes,
                expectedModelType,
                expectedDeviceType,
                List.of(createHostedService(expectedServiceId,
                        expectedHostedServiceQNameTypes,
                        expectedHostedServiceEprs))
        )));

        mockGetMetadataClient.setGetMetadataMessages(
                Arrays.asList(createGetMetadataMessage(), createGetMetadataMessage()));

        HostingServiceResolver hostingServiceResolver = getInjector().getInstance(HostingServiceResolver.class);
        long expectedMetadataVersion = 100;
        DiscoveredDevice expectedDiscoveredDevice = createDiscoveredDevice(expectedDeviceEprAddress, List.of("http://xAddr"),
                expectedMetadataVersion);
        ListenableFuture<HostingServiceProxy> hsF = hostingServiceResolver.resolveHostingService(expectedDiscoveredDevice);
        try {
            HostingServiceProxy actualHsp = hsF.get();
            assertEquals(expectedDeviceEprAddress, actualHsp.getEndpointReferenceAddress());
            assertEquals(expectedMetadataVersion, actualHsp.getMetadataVersion());
            assertTrue(actualHsp.getThisDevice().isPresent());
            assertEquals(expectedSerialNumber, actualHsp.getThisDevice().get().getSerialNumber());
            assertTrue(actualHsp.getThisModel().isPresent());
            assertEquals(expectedModelNumber, actualHsp.getThisModel().get().getModelNumber());
            assertEquals(1, actualHsp.getHostedServices().size());
            HostedServiceProxy actualHostedServiceProxy = actualHsp.getHostedServices().get(expectedServiceId);
            assertNotNull(actualHostedServiceProxy);
            assertEquals(expectedHostedServiceQNameTypes.toString(),
                    actualHostedServiceProxy.getType().getTypes().toString());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void resolveHostingServiceWithMismatchedEprAddress() {
        // Enforce the resolution of a hosting service proxy with unexpected EPR address
        var unexpectedDeviceEprAddress = "00001cb1-af6a-4dfb-ba11-283d88410000";
        mockTransferGetClient.setTransferGetMessages(List.of(createTransferGetMessage(
                unexpectedDeviceEprAddress,
                expectedHostingServiceQNameTypes,
                expectedModelType,
                expectedDeviceType,
                List.of(createHostedService(expectedServiceId,
                        expectedHostedServiceQNameTypes,
                        expectedHostedServiceEprs))
        )));

        mockGetMetadataClient.setGetMetadataMessages(
                Arrays.asList(createGetMetadataMessage(), createGetMetadataMessage()));

        var hostingServiceResolver = getInjector().getInstance(HostingServiceResolver.class);
        var expectedMetadataVersion = 100L;
        var expectedDiscoveredDevice = createDiscoveredDevice(
                expectedDeviceEprAddress,
                List.of("http://xAddr"),
                expectedMetadataVersion);
        assertThrows(EprAddressMismatchException.class, () -> {
            try {
                hostingServiceResolver.resolveHostingService(expectedDiscoveredDevice).get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        });
    }

    private HostedService createHostedService(String serviceId, List<QName> types, List<EndpointReferenceType> eprs) {
        HostedServiceType hst = dpwsFactory.createHostedServiceType();
        hst.setTypes(types);
        hst.setServiceId(serviceId);
        hst.setEndpointReference(eprs);
        HostedService hs = mock(HostedService.class);
        when(hs.getType()).thenReturn(hst);
        return hs;
    }

    private SoapMessage createTransferGetMessage(String eprAddress,
                                                 List<QName> types,
                                                 ThisModelType thisModel,
                                                 ThisDeviceType thisDevice,
                                                 List<HostedService> hostedServices) {
        Metadata metadata = mexFactory.createMetadata();
        List<MetadataSection> metadataSection = metadata.getMetadataSection();

        metadataSection.add(createThisModel(thisModel));
        metadataSection.add(createThisDevice(thisDevice));

        metadataSection.add(metadataSectionUtil.createRelationship(
                wsaUtil.createEprWithAddress(eprAddress),
                types,
                hostedServices));

        metadata.setMetadataSection(metadataSection);

        SoapMessage msg = soapUtil.createMessage();
        soapUtil.setBody(metadata, msg);
        return msg;
    }

    private SoapMessage createGetMetadataMessage() {
        Metadata metadata = mexFactory.createMetadata();
        SoapMessage msg = soapUtil.createMessage();
        soapUtil.setBody(metadata, msg);
        return msg;
    }

    private MetadataSection createThisModel(ThisModelType modelType) {
        MetadataSection metadataSection = mexFactory.createMetadataSection();
        metadataSection.setDialect(DpwsConstants.MEX_DIALECT_THIS_MODEL);
        metadataSection.setAny(modelType);
        return metadataSection;
    }

    private MetadataSection createThisDevice(ThisDeviceType deviceType) {
        MetadataSection metadataSection = mexFactory.createMetadataSection();
        metadataSection.setDialect(DpwsConstants.MEX_DIALECT_THIS_DEVICE);
        metadataSection.setAny(deviceType);
        return metadataSection;
    }

    private DiscoveredDevice createDiscoveredDevice(String deviceUuid, List<String> xAddrs, long version) {
        return new DiscoveredDevice(
                deviceUuid,
                mock(List.class),
                mock(List.class),
                xAddrs,
                version);
    }

    private HostingServiceProxy createHostingServiceProxy(String deviceUuid, long version) {
        return hostingServiceFactory.createHostingServiceProxy(
                deviceUuid,
                mock(List.class),
                null,
                null,
                mock(Map.class),
                version,
                mock(RequestResponseClient.class),
                mock(String.class));
    }

    class MockTransferGetClient implements TransferGetClient {
        private final Stack<SoapMessage> transferGetMessages = new Stack<>();

        void setTransferGetMessages(Collection<SoapMessage> transferGetMessages) {
            this.transferGetMessages.addAll(transferGetMessages);
        }

        @Override
        public ListenableFuture<SoapMessage> sendTransferGet(RequestResponseClient requestResponseClient, String wsaTo) {
            try {
                return Futures.immediateFuture(transferGetMessages.pop());
            } catch (EmptyStackException e) {
                throw new RuntimeException("TransferGet message stack empty");
            }
        }

        @Override
        public ListenableFuture<SoapMessage> sendTransferGet(RequestResponseClient requestResponseClient, String wsaTo, ReferenceParametersType referenceParameters) {
            try {
                return Futures.immediateFuture(transferGetMessages.pop());
            } catch (EmptyStackException e) {
                throw new RuntimeException("TransferGet message stack empty");
            }
        }
    }

    class MockGetMetadataClient implements GetMetadataClient {
        private final Stack<SoapMessage> getMetadataMessages = new Stack<>();

        void setGetMetadataMessages(Collection<SoapMessage> getMetadataMessages) {
            this.getMetadataMessages.addAll(getMetadataMessages);
        }

        @Override
        public ListenableFuture<SoapMessage> sendGetMetadata(RequestResponseClient requestResponseClient) {
            try {
                return Futures.immediateFuture(getMetadataMessages.pop());
            } catch (EmptyStackException e) {
                throw new RuntimeException("GetMetadata message stack empty");
            }
        }
    }
}