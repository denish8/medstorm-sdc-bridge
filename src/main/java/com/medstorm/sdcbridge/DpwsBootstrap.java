package com.medstorm.sdcbridge;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Objects;

final class DpwsBootstrap {
    private DpwsBootstrap() {}

    /** Call this as the first line in main() before Spring/Guice are started. */
    static void primeFromNicHint() {
        // Force IPv4 stack early (harmless if already set)
        System.setProperty("java.net.preferIPv4Stack", "true");

        final String nicHint = firstNonBlank(
                System.getProperty("sdc.nic"),
                System.getenv("SDC_NIC"),
                ""
        ).trim();
        if (nicHint.isBlank()) {
            System.out.println("[DpwsBootstrap] no sdc.nic provided; skipping NIC priming");
            return;
        }

        final NetworkInterface ni = resolveNic(nicHint);
        if (ni == null) {
            System.out.printf("[DpwsBootstrap] could not resolve NIC from hint \"%s\"%n", nicHint);
            return;
        }

        // Pick first IPv4 address on that NIC
        InetAddress ipv4Addr = null;
        try {
            for (Enumeration<InetAddress> e = ni.getInetAddresses(); e.hasMoreElements();) {
                InetAddress a = e.nextElement();
                if (a instanceof Inet4Address) {
                    ipv4Addr = a;
                    break;
                }
            }
        } catch (Exception ignored) { /* keep null */ }

        final String nicName = ni.getName();
        final String nicDisplay = ni.getDisplayName() != null ? ni.getDisplayName() : nicName;
        final String ipv4 = ipv4Addr != null ? ipv4Addr.getHostAddress() : null;

        System.out.printf("[DpwsBootstrap] primed props: ifname=%s, display=\"%s\", ipv4=%s%n",
                Objects.toString(nicName, "n/a"),
                Objects.toString(nicDisplay, "n/a"),
                Objects.toString(ipv4, "n/a"));

        // ======= SDCri expects a mix of keys across versions. Seed them all. =======

        // --- Canonical names (title-case namespace)
        set("Dpws.Udp.NetworkInterfaceName", nicName);
        set("Dpws.NetworkInterfaceName", nicName);
        set("Dpws.NetworkInterface", nicName);
        set("WsDiscovery.NetworkInterfaceName", nicName);

        // --- AdapterName aliases frequently used in discovery modules
        set("Dpws.AdapterName", nicName);
        set("Dpws.Udp.AdapterName", nicName);
        set("WsDiscovery.AdapterName", nicName);

        // --- Lowercase variants seen in some builds
        set("dpws.udp.networkinterfacename", nicName);
        set("dpws.networkinterfacename", nicName);
        set("dpws.networkinterface", nicName);

        // --- org.somda.* variants (both casings are used historically)
        set("org.somda.sdc.dpws.udp.NetworkInterfaceName", nicName);
        set("org.somda.sdc.dpws.udp.networkInterfaceName", nicName);
        set("org.somda.sdc.dpws.NetworkInterfaceName", nicName);
        set("org.somda.sdc.dpws.networkInterfaceName", nicName);
        set("org.somda.sdc.dpws.NetworkInterface", nicName);
        set("org.somda.sdc.dpws.networkInterface", nicName);

        // --- org.somda.* AdapterName aliases (cover discovery paths)
        set("org.somda.sdc.dpws.AdapterName", nicName);
        set("org.somda.sdc.dpws.adapterName", nicName);
        set("org.somda.sdc.dpws.udp.AdapterName", nicName);
        set("org.somda.sdc.dpws.udp.adapterName", nicName);
        set("org.somda.sdc.dpws.wsdiscovery.AdapterName", nicName);
        set("org.somda.sdc.dpws.wsdiscovery.adapterName", nicName);

        // --- BindAddress as STRING properties (critical for some 6.0.0 code paths)
        if (ipv4 != null) {
            set("Dpws.Udp.BindAddress", ipv4);
            set("dpws.udp.bindaddress", ipv4);
            set("org.somda.sdc.dpws.udp.BindAddress", ipv4);
            set("org.somda.sdc.dpws.udp.bindAddress", ipv4);
        }
    }

    private static String firstNonBlank(String a, String b, String c) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return c;
    }

    private static void set(String k, String v) {
        if (k == null || v == null) return;
        System.setProperty(k, v);
    }

    private static NetworkInterface resolveNic(String hint) {
        try {
            if (hint != null && !hint.isBlank()) {
                NetworkInterface byName = NetworkInterface.getByName(hint);
                if (byName != null) return byName;
            }
        } catch (Exception ignored) {}

        final String h = hint == null ? "" : hint.toLowerCase(Locale.ROOT);
        try {
            for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements();) {
                NetworkInterface ni = e.nextElement();
                if (ni == null) continue;
                final String dn = ni.getDisplayName();
                final String n  = ni.getName();
                if (equalsIgnoreCase(n, hint) || equalsIgnoreCase(dn, hint)) return ni;
                if (containsIgnoreCase(dn, h)) return ni;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    private static boolean containsIgnoreCase(String a, String bLower) {
        return a != null && bLower != null && a.toLowerCase(Locale.ROOT).contains(bLower);
    }
}
