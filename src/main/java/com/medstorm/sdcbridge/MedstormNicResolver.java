// import java.net.*;
// import java.util.*;

// public class MedstormNicResolver {

//     public static NetworkInterface resolve(String ifName) throws SocketException {
//         if (ifName == null || ifName.isBlank()) {
//             throw new IllegalArgumentException("Interface name is null or empty");
//         }

//         NetworkInterface nic = NetworkInterface.getByName(ifName);

//         // If not found by system name, try by display name
//         if (nic == null) {
//             Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
//             while (ifaces.hasMoreElements()) {
//                 NetworkInterface ni = ifaces.nextElement();
//                 if (ni.getDisplayName().equalsIgnoreCase(ifName)
//                         || ni.getName().equalsIgnoreCase(ifName)) {
//                     nic = ni;
//                     break;
//                 }
//             }
//         }

//         if (nic == null) {
//             throw new SocketException("NetworkInterface not found for name/display: " + ifName);
//         }

//         // Optional: log
//         InetAddress ipv4 = getFirstIpv4(nic);
//         System.out.printf("[MedstormNicResolver] resolved NIC '%s' (%s) -> IPv4=%s%n",
//                 ifName, nic.getDisplayName(), ipv4);

//         return nic;
//     }

//     private static InetAddress getFirstIpv4(NetworkInterface ni) {
//         Enumeration<InetAddress> addrs = ni.getInetAddresses();
//         while (addrs.hasMoreElements()) {
//             InetAddress addr = addrs.nextElement();
//             if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
//                 return addr;
//             }
//         }
//         return null;
//     }
// }

package com.medstorm.sdcbridge;

import java.net.*;
import java.util.*;

public final class MedstormNicResolver {
    private MedstormNicResolver() {}

    /** Resolve a NIC by name, display name, IPv4, or "Wi-Fi"/"wlan0" heuristics. */
    public static Optional<NetworkInterface> resolveNic(String hintRaw) {
        final String hint = hintRaw == null ? "" : hintRaw.trim();
        if (hint.isEmpty()) return Optional.empty();

        // exact by name, e.g. "wlan0"
        try {
            NetworkInterface byName = NetworkInterface.getByName(hint);
            if (byName != null) return Optional.of(byName);
        } catch (Exception ignore) {}

        // exact by display name, e.g. "Intel(R) Wi-Fi 6 AX201 160MHz"
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (hint.equalsIgnoreCase(Optional.ofNullable(ni.getDisplayName()).orElse("")))
                    return Optional.of(ni);
            }
        } catch (SocketException ignore) {}

        // by IPv4 address string, e.g. "192.168.10.134"
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (Enumeration<InetAddress> e = ni.getInetAddresses(); e.hasMoreElements();) {
                    InetAddress a = e.nextElement();
                    if (a instanceof Inet4Address && a.getHostAddress().equals(hint))
                        return Optional.of(ni);
                }
            }
        } catch (SocketException ignore) {}

        // friendly heuristics: "Wi-Fi", "wifi", "wlan0"
        try {
            final String h = hint.toLowerCase(Locale.ROOT);
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                final String name = Optional.ofNullable(ni.getName()).orElse("").toLowerCase(Locale.ROOT);
                final String disp = Optional.ofNullable(ni.getDisplayName()).orElse("").toLowerCase(Locale.ROOT);
                final boolean wifiish = name.startsWith("wlan")
                        || disp.contains("wi-fi") || disp.contains("wifi") || disp.contains("wireless");
                if (wifiish && (h.equals("wlan0") || h.equals("wi-fi") || h.equals("wifi")))
                    return Optional.of(ni);
            }
        } catch (SocketException ignore) {}

        return Optional.empty();
    }
}
