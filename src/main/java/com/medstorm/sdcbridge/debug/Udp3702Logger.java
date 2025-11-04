package com.medstorm.sdcbridge.debug;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Super-simple UDP listener just to prove that
 *  - Windows delivers 239.255.255.250:3702 to this JVM, and
 *  - we can join the correct NIC.
 *
 * It does NOT replace DPWS – it's only for diagnostics.
 */
@Component
public class Udp3702Logger {
    private static final Logger log = LoggerFactory.getLogger(Udp3702Logger.class);

    // your SDC host that appears in the logs
    // if you change IP later, change it here too
    private static final String LOCAL_IP = "192.168.10.134";

    private static final String MULTICAST_ADDR = "239.255.255.250";
    private static final int PORT = 3702;

    private MulticastSocket socket;
    private ExecutorService executor;

    @PostConstruct
    public void start() {
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "udp-3702-sniffer");
            t.setDaemon(true);
            return t;
        });
        executor.submit(this::runListener);
    }

    private void runListener() {
        try {
            InetAddress group = InetAddress.getByName(MULTICAST_ADDR);
            InetAddress local = InetAddress.getByName(LOCAL_IP);
            NetworkInterface nif = NetworkInterface.getByInetAddress(local);
            if (nif == null) {
                log.error("No NetworkInterface for {}", LOCAL_IP);
                return;
            }

            socket = new MulticastSocket(PORT);
            socket.setReuseAddress(true);
            socket.setSoTimeout(0);
            socket.setLoopbackMode(false); // we DO want loopback = false means allow
            socket.joinGroup(new InetSocketAddress(group, PORT), nif);

            log.info("UDP3702 logger JOINED {}:{} on NIC {} ({})",
                    MULTICAST_ADDR, PORT, nif.getName(), local.getHostAddress());

            byte[] buf = new byte[8192];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while (!Thread.currentThread().isInterrupted()) {
                socket.receive(packet);
                String body = new String(packet.getData(), packet.getOffset(),
                        packet.getLength(), StandardCharsets.UTF_8);

                log.info("UDP3702 <<< from {}:{}\n{}",
                        packet.getAddress().getHostAddress(),
                        packet.getPort(),
                        body);
            }
        } catch (IOException e) {
            log.error("UDP3702 logger stopped with error", e);
        }
    }

    @PreDestroy
    public void stop() {
        if (socket != null) {
            try {
                socket.leaveGroup(new InetSocketAddress(MULTICAST_ADDR, PORT),
                        NetworkInterface.getByInetAddress(InetAddress.getByName(LOCAL_IP)));
            } catch (Exception ignore) {}
            socket.close();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
