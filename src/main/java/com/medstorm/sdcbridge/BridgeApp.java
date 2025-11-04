// src/main/java/com/medstorm/sdcbridge/BridgeApp.java
package com.medstorm.sdcbridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;

@SpringBootApplication
public class BridgeApp {
    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("jdk.net.usePlainDatagramSocketImpl", "true");

        DpwsBootstrap.primeFromNicHint(); // logs display + ipv4

        final String nicHint = System.getProperty("sdc.nic", "");
        final NetworkInterface nic = resolveNic(nicHint);
        if (nic != null) {
            final String nicName = nic.getName(); // e.g. "wlan0"
            final InetAddress ipv4 = Collections.list(nic.getInetAddresses())
                    .stream().filter(a -> a instanceof Inet4Address).findFirst().orElse(null);

            if (ipv4 != null) {
                final String ip = ipv4.getHostAddress();

                // ==== CRITICAL: set every DPWS/WS-Discovery variant ====
                // (Most important are the org.somda.sdc.dpws.udp.* names.)
                System.setProperty("org.somda.sdc.dpws.udp.NetworkInterfaceName", nicName);
                System.setProperty("Dpws.Udp.NetworkInterfaceName", nicName);
                System.setProperty("org.somda.sdc.dpws.NetworkInterfaceName", nicName);
                System.setProperty("Dpws.NetworkInterfaceName", nicName);
                System.setProperty("Dpws.NetworkInterface", nicName);
                System.setProperty("Dpws.AdapterName", nicName);
                System.setProperty("Dpws.Udp.AdapterName", nicName);

                // WS-Discovery (some builds check these)
                System.setProperty("org.somda.sdc.dpws.wsdiscovery.NetworkInterfaceName", nicName);
                System.setProperty("WsDiscovery.NetworkInterfaceName", nicName);
                System.setProperty("WsDiscovery.AdapterName", nicName);

                // Bind address (string variants)
                System.setProperty("org.somda.sdc.dpws.udp.BindAddress", ip);
                System.setProperty("org.somda.sdc.dpws.udp.bindAddress", ip);
                System.setProperty("Dpws.Udp.BindAddress", ip);
                System.setProperty("dpws.udp.bindaddress", ip);
            }
        }

        SpringApplication.run(BridgeApp.class, args);
    }

    private static NetworkInterface resolveNic(String hint) {
        if (hint == null || hint.isBlank()) return null;
        try {
            NetworkInterface ni = NetworkInterface.getByName(hint);
            if (ni != null) return ni;
        } catch (Exception ignore) {}

        try {
            String h = hint.toLowerCase(Locale.ROOT);
            for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements();) {
                NetworkInterface ni = e.nextElement();
                if (ni == null) continue;
                String dn = ni.getDisplayName();
                String n = ni.getName();
                if (dn != null && dn.equalsIgnoreCase(hint)) return ni;
                if (n != null && n.equalsIgnoreCase(hint)) return ni;
                if (dn != null && dn.toLowerCase(Locale.ROOT).contains(h)) return ni;
            }
        } catch (Exception ignore) {}
        return null;
    }
}
