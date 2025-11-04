package it.org.somda.sdc.dpws.udp;

import com.google.common.util.concurrent.SettableFuture;
import it.org.somda.sdc.dpws.IntegrationTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.soap.ApplicationInfo;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.TransportInfo;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants;
import org.somda.sdc.dpws.udp.UdpBindingService;
import org.somda.sdc.dpws.udp.UdpMessage;
import org.somda.sdc.dpws.udp.factory.UdpBindingServiceFactory;
import test.org.somda.common.LoggingTestWatcher;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(LoggingTestWatcher.class)
class UdpBindingServiceImplIT {
    private final IntegrationTestUtil IT = new IntegrationTestUtil();

    private Lock lock;
    private Condition condition;
    private UdpMessage actualMessage;
    private UdpBindingServiceFactory factory;
    private NetworkInterface localhostInterface;

    public UdpBindingServiceImplIT() {
        IntegrationTestUtil.preferIpV4Usage();
    }

    @BeforeEach
    public void setUp() throws Exception {
        factory = IT.getInjector().getInstance(UdpBindingServiceFactory.class);
        lock = new ReentrantLock();
        condition = lock.newCondition();
        localhostInterface = NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
    }

    @Test
    void testSendMulticastMessage() throws Exception {
        final byte[] expectedMessage = "SAMPLE".getBytes();
        actualMessage = null;

        UdpBindingService receiver = factory.createUdpBindingService(
                localhostInterface,
                InetAddress.getByName(WsDiscoveryConstants.IPV4_MULTICAST_ADDRESS),
                DpwsConstants.DISCOVERY_PORT,
                DpwsConstants.MAX_UDP_ENVELOPE_SIZE);
        Thread t = new Thread(() -> {
            try {
                UdpBindingService sender = factory.createUdpBindingService(
                        localhostInterface,
                        InetAddress.getByName(WsDiscoveryConstants.IPV4_MULTICAST_ADDRESS),
                        DpwsConstants.DISCOVERY_PORT, DpwsConstants.MAX_UDP_ENVELOPE_SIZE);
                sender.startAsync().awaitRunning();
                sender.sendMessage(new UdpMessage(expectedMessage, expectedMessage.length));
            } catch (Exception e) {
                fail();
            }
        });

        receiver.setMessageReceiver(msg -> {
            try {
                lock.lock();
                actualMessage = msg;
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        });
        receiver.startAsync().awaitRunning();

        t.start();
        try {
            lock.lock();
            long wait = 10000;
            long tStart = System.currentTimeMillis();
            while (wait > 0) {
                if (actualMessage != null) {
                    break;
                }
                condition.await(wait, TimeUnit.MILLISECONDS);
                wait -= System.currentTimeMillis() - tStart;
            }

            assertNotNull(actualMessage);
            byte[] actualBytes = Arrays.copyOf(actualMessage.getData(), actualMessage.getLength());
            assertEquals(Arrays.toString(expectedMessage), Arrays.toString(actualBytes));
        } finally {
            lock.unlock();
        }
    }

    @Test
    void testSendMulticastMessageWithReply() throws Exception {
        final String expectedRequestStr = "REQUEST";
        final String expectedResponseStr = "RESPONSE";
        final byte[] expectedRequestBytes = expectedRequestStr.getBytes();
        final byte[] expectedResponseBytes = expectedResponseStr.getBytes();

        SettableFuture<String> settableFuture = SettableFuture.create();

        final UdpBindingService senderReceiver1 = factory.createUdpBindingService(
                localhostInterface,
                InetAddress.getByName(WsDiscoveryConstants.IPV4_MULTICAST_ADDRESS),
                DpwsConstants.DISCOVERY_PORT,
                DpwsConstants.MAX_UDP_ENVELOPE_SIZE);
        senderReceiver1.setMessageReceiver(udpMessage -> {
            if (udpMessage.toString().equals(expectedResponseStr)) {
                settableFuture.set(udpMessage.toString());
            }
        });

        final UdpBindingService senderReceiver2 = factory.createUdpBindingService(
                localhostInterface,
                InetAddress.getByName(WsDiscoveryConstants.IPV4_MULTICAST_ADDRESS),
                DpwsConstants.DISCOVERY_PORT,
                DpwsConstants.MAX_UDP_ENVELOPE_SIZE);
        senderReceiver2.setMessageReceiver(udpMessage -> {
            if (udpMessage.toString().equals(expectedRequestStr)) {
                Thread t = new Thread(() -> {
                    try {
                        var ctxt = new CommunicationContext(
                                new ApplicationInfo(),
                                new TransportInfo(
                                        DpwsConstants.URI_SCHEME_SOAP_OVER_UDP,
                                        null, null,
                                        udpMessage.getHost(), udpMessage.getPort(),
                                        Collections.emptyList()
                                ),
                                null
                        );
                        senderReceiver2.sendMessage(new UdpMessage(expectedResponseBytes, expectedResponseBytes.length, ctxt));
                    } catch (Exception e) {
                        fail();
                    }
                });
                t.start();
            }
        });

        senderReceiver1.startAsync().awaitRunning();
        senderReceiver2.startAsync().awaitRunning();

        Thread t = new Thread(() -> {
            try {
                senderReceiver1.sendMessage(new UdpMessage(expectedRequestBytes, expectedRequestBytes.length));
            } catch (Exception e) {
                fail();
            }
        });
        t.start();

        assertEquals(expectedResponseStr, settableFuture.get(10, TimeUnit.SECONDS).substring(0, expectedResponseStr.length()));
    }
}