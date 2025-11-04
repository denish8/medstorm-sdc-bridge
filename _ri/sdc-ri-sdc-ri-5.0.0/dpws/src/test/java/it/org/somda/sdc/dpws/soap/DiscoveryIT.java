package it.org.somda.sdc.dpws.soap;


import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import it.org.somda.sdc.dpws.IntegrationTestUtil;
import it.org.somda.sdc.dpws.MockedUdpBindingModule;
import it.org.somda.sdc.dpws.TestServiceMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.dpws.client.DiscoveredDevice;
import org.somda.sdc.dpws.client.DiscoveryFilterBuilder;
import org.somda.sdc.dpws.client.DiscoveryObserver;
import org.somda.sdc.dpws.client.event.DeviceEnteredMessage;
import org.somda.sdc.dpws.client.event.DeviceProbeTimeoutMessage;
import org.somda.sdc.dpws.client.event.ProbedDeviceFoundMessage;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.SoapConfig;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConfig;
import test.org.somda.common.LoggingTestWatcher;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(LoggingTestWatcher.class)
class DiscoveryIT {
    private static final Duration MAX_WAIT_TIME = IntegrationTestUtil.MAX_WAIT_TIME;

    private DevicePeer devicePeer;
    private ClientPeer clientPeer;

    public DiscoveryIT() {
        IntegrationTestUtil.preferIpV4Usage();
    }

    @BeforeEach
    public void setUp() {
        this.devicePeer = new BasicPopulatedDevice(new MockedUdpBindingModule());
        this.clientPeer = new ClientPeer(new DefaultDpwsConfigModule() {
            @Override
            public void customConfigure() {
                bind(WsDiscoveryConfig.MAX_WAIT_FOR_PROBE_MATCHES, Duration.class,
                        Duration.ofSeconds(MAX_WAIT_TIME.getSeconds() / 2));
                bind(SoapConfig.JAXB_CONTEXT_PATH, String.class,
                        TestServiceMetadata.JAXB_CONTEXT_PATH);
            }
        }, new MockedUdpBindingModule());
    }

    @AfterEach
    public void tearDown() {
        this.devicePeer.stopAsync().awaitTerminated();
        this.clientPeer.stopAsync().awaitTerminated();
    }

    @Test
    void explicitDeviceDiscovery() throws Exception {
        // Given a device under test (DUT) and a client up and running
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        // Given a discovery observer
        final SettableFuture<Integer> actualDeviceFoundCount = SettableFuture.create();
        final SettableFuture<String> actualEpr = SettableFuture.create();
        DiscoveryObserver obs = new DiscoveryObserver() {
            @Subscribe
            void deviceFound(ProbedDeviceFoundMessage message) {
                if (devicePeer.getEprAddress().equals(message.getPayload().getEprAddress())) {
                    actualEpr.set(message.getPayload().getEprAddress());
                }
            }

            @Subscribe
            void timeout(DeviceProbeTimeoutMessage message) {
            }
        };

        // When explicit discovery is triggered (filter set to none)
        clientPeer.getClient().registerDiscoveryObserver(obs);
        DiscoveryFilterBuilder discoveryFilterBuilder = new DiscoveryFilterBuilder();
        clientPeer.getClient().probe(discoveryFilterBuilder.get());

        // Then expect the found EPR address to be the DUT's EPR address
        final String expectedEprAddress = devicePeer.getEprAddress().toString();
        assertEquals(expectedEprAddress, actualEpr.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS));

        // Resolve and verify EPR address against the DUT's EPR address
        assertEquals(expectedEprAddress,
                clientPeer.getClient().resolve(devicePeer.getEprAddress()).get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS)
                        .getEprAddress().toString());
    }

    @Test
    void implicitDeviceDiscovery() throws Exception {
        // Given a running client that listens to discovered devices
        clientPeer.startAsync().awaitRunning();
        final SettableFuture<String> actualEpr = SettableFuture.create();
        final DiscoveryObserver obs = new DiscoveryObserver() {
            @Subscribe
            void deviceFound(DeviceEnteredMessage message) {
                if (devicePeer.getEprAddress().equals(message.getPayload().getEprAddress())) {
                    actualEpr.set(message.getPayload().getEprAddress());
                }
            }
        };
        clientPeer.getClient().registerDiscoveryObserver(obs);

        // When a device under test (DUT) joins the network
        devicePeer.startAsync().awaitRunning();

        // Then expect the found EPR address to be the DUT's EPR address
        final String expectedEprAddress = devicePeer.getEprAddress();
        assertEquals(expectedEprAddress, actualEpr.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS));

        // Resolve and verify EPR address against the DUT's EPR address
        assertEquals(expectedEprAddress,
                     clientPeer.getClient().resolve(devicePeer.getEprAddress()).get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS)
                             .getEprAddress());
    }

    @Test
    void directedProbe() throws Exception {

        // Given a device under test (DUT) and a client up and running
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        // When the DUT's physical addresses are resolved
        final DiscoveredDevice discoveredDevice = clientPeer.getClient().resolve(devicePeer.getEprAddress())
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        final List<String> xAddrs = discoveredDevice.getXAddrs();
        assertFalse(xAddrs.isEmpty());
        var uri = xAddrs.get(0);

        // Then expect the EPR address returned by a directed probe to be the DUT's EPR address
        final String expectedEprAddress = devicePeer.getEprAddress();

        assertEquals(expectedEprAddress, clientPeer.getClient().directedProbe(uri)
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS)
                .getProbeMatch().get(0).getEndpointReference().getAddress().getValue());
    }

    @Test
    void connectWithDiscoveredDevice() throws Exception {
        // Given a device under test (DUT) and a client up and running
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        // When the client connects to the DUT
        final DiscoveredDevice discoveredDevice = clientPeer.getClient().resolve(devicePeer.getEprAddress())
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        final ListenableFuture<HostingServiceProxy> hostingServiceProxyFuture = clientPeer.getClient().connect(discoveredDevice);

        // Then expect a hosting service to be resolved that matches the DUT EPR address
        final String expectedEprAddress = devicePeer.getEprAddress();
        final HostingServiceProxy hostingServiceProxy = hostingServiceProxyFuture.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        final String actualEprAddress = hostingServiceProxy.getEndpointReferenceAddress();
        assertEquals(expectedEprAddress, actualEprAddress);
    }

    @Test
    void connectWithEprAddress() throws Exception {
        // Given a device under test (DUT) and a client up and running
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        // When the client connects to the DUT
        final ListenableFuture<HostingServiceProxy> hostingServiceProxyFuture = clientPeer.getClient()
                .connect(devicePeer.getEprAddress());

        // Then expect a hosting service to be resolved that matches the DUT EPR address
        final String expectedEprAddress = devicePeer.getEprAddress();
        final HostingServiceProxy hostingServiceProxy = hostingServiceProxyFuture.get(MAX_WAIT_TIME.getSeconds(),
                TimeUnit.SECONDS);
        final String actualEprAddress = hostingServiceProxy.getEndpointReferenceAddress();
        assertEquals(expectedEprAddress, actualEprAddress);
    }

    // @Test
    public void sample() throws Exception {
        // Given a device under test (DUT) and a client up and running
        devicePeer.startAsync().awaitRunning();
        Thread.sleep(100000000);
    }
}
