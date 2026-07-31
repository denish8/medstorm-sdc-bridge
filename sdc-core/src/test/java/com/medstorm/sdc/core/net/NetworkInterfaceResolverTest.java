package com.medstorm.sdc.core.net;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkInterfaceResolverTest {

    private static NicCandidate<String> nic(String name, String display, boolean up, String... ipv4) {
        return new NicCandidate<>(name, name, display, up, false, List.of(ipv4));
    }

    private static NicCandidate<String> loopback() {
        return new NicCandidate<>("loopback_0", "loopback_0", "Software Loopback Interface 1",
                true, true, List.of("127.0.0.1"));
    }

    /**
     * The adapter topology of the Windows 11 / MediaTek MT7920 machine on which
     * {@code -Dsdc.nic=Wi-Fi} failed with "No IPv4 address found on NIC 'wireless_0'".
     *
     * <p>Nine interfaces carry the wireless card's display-name prefix. Exactly one
     * is the physical adapter; the rest are filter pseudo-adapters with no address.
     * Enumeration order puts a filter first, which is what the old substring match
     * selected.
     */
    private static List<NicCandidate<String>> mediaTekMachine() {
        List<NicCandidate<String>> all = new ArrayList<>();
        all.add(nic("ethernet_0", "WAN Miniport (IP)-WFP Native MAC Layer LightWeight Filter-0000", true));
        all.add(nic("ethernet_1", "WAN Miniport (IP)-QoS Packet Scheduler-0000", true));
        all.add(nic("ethernet_32768", "ASIX AX88772C USB2.0 to Fast Ethernet Adapter", false));
        all.add(nic("ethernet_32769", "Microsoft Kernel Debug Network Adapter", false));
        all.add(nic("ethernet_32770", "Bluetooth Device (Personal Area Network)", false));
        all.add(loopback());
        // The nine wireless_* interfaces, in real enumeration order.
        all.add(nic("wireless_0",
                "MediaTek Wi-Fi 6 MT7920 Wireless LAN Card-WFP Native MAC Layer LightWeight Filter-0000", true));
        all.add(nic("wireless_1",
                "MediaTek Wi-Fi 6 MT7920 Wireless LAN Card-Virtual WiFi Filter Driver-0000", true));
        all.add(nic("wireless_2",
                "MediaTek Wi-Fi 6 MT7920 Wireless LAN Card-Native WiFi Filter Driver-0000", true));
        all.add(nic("wireless_3",
                "MediaTek Wi-Fi 6 MT7920 Wireless LAN Card-QoS Packet Scheduler-0000", true));
        all.add(nic("wireless_4",
                "MediaTek Wi-Fi 6 MT7920 Wireless LAN Card-WFP 802.3 MAC Layer LightWeight Filter-0000", true));
        all.add(nic("wireless_5",
                "Microsoft Wi-Fi Direct Virtual Adapter-WFP Native MAC Layer LightWeight Filter-0000", false));
        all.add(nic("wireless_6",
                "Microsoft Wi-Fi Direct Virtual Adapter-Native WiFi Filter Driver-0000", false));
        // The physical adapter - the only one with an address.
        all.add(nic("wireless_32768", "MediaTek Wi-Fi 6 MT7920 Wireless LAN Card", true, "192.168.1.6"));
        return all;
    }

    @Nested
    @DisplayName("regression: the MediaTek MT7920 machine")
    class MediaTekRegression {

        @Test
        @DisplayName("'Wi-Fi' selects the physical adapter, not the first filter pseudo-adapter")
        void wiFiHintSelectsPhysicalAdapter() {
            var match = NetworkInterfaceResolver.select("Wi-Fi", mediaTekMachine());
            assertTrue(match.isPresent(), "'Wi-Fi' must resolve on a machine with a working Wi-Fi adapter");
            assertEquals("wireless_32768", match.get().name());
        }

        @Test
        @DisplayName("no hint spelling can reach an adapter without an IPv4 address")
        void phantomAdaptersAreNeverSelected() {
            List<String> phantoms = List.of("wireless_0", "wireless_1", "wireless_2", "wireless_3", "wireless_4");
            for (String hint : List.of("Wi-Fi", "wifi", "wlan0", "wireless", "MediaTek")) {
                var match = NetworkInterfaceResolver.select(hint, mediaTekMachine());
                match.ifPresent(c -> assertTrue(!phantoms.contains(c.name()),
                        "hint '" + hint + "' selected pseudo-adapter " + c.name()));
            }
        }

        @Test
        @DisplayName("naming a phantom adapter outright still does not select it")
        void explicitlyNamingAnUnusableAdapterYieldsNothing() {
            var match = NetworkInterfaceResolver.select("wireless_0", mediaTekMachine());
            assertTrue(match.isEmpty(),
                    "wireless_0 has no IPv4 address; selecting it can only fail later, further from the cause");
        }
    }

    @Test
    @DisplayName("an IPv4 literal is unambiguous and wins over any name match")
    void exactIpv4Wins() {
        var match = NetworkInterfaceResolver.select("192.168.1.6", mediaTekMachine());
        assertEquals("wireless_32768", match.orElseThrow().name());
    }

    @Test
    @DisplayName("an exact interface name beats a fuzzy display-name match")
    void exactNameBeatsFuzzy() {
        List<NicCandidate<String>> nics = List.of(
                nic("eth0", "Wi-Fi lookalike adapter", true, "10.0.0.2"),
                nic("wlan0", "Some Wireless Card", true, "10.0.0.3"));
        assertEquals("wlan0", NetworkInterfaceResolver.select("wlan0", nics).orElseThrow().name());
    }

    @Test
    @DisplayName("an exact display name resolves")
    void exactDisplayNameResolves() {
        var match = NetworkInterfaceResolver.select("MediaTek Wi-Fi 6 MT7920 Wireless LAN Card", mediaTekMachine());
        assertEquals("wireless_32768", match.orElseThrow().name());
    }

    @Test
    @DisplayName("colloquial aliases reach Linux-style interface names")
    void wirelessAliasesMatchLinuxNaming() {
        List<NicCandidate<String>> nics = List.of(
                nic("eth0", "Intel Ethernet", true, "10.0.0.2"),
                nic("wlan0", "Intel Wireless-AC 9560", true, "10.0.0.3"));
        for (String hint : List.of("Wi-Fi", "wifi", "wireless", "wlan")) {
            assertEquals("wlan0", NetworkInterfaceResolver.select(hint, nics).orElseThrow().name(),
                    "hint '" + hint + "' should reach wlan0");
        }
    }

    @Test
    @DisplayName("a down interface is ignored even when its name matches exactly")
    void downInterfaceIsIgnored() {
        List<NicCandidate<String>> nics = List.of(
                nic("eth0", "Unplugged Ethernet", false, "10.0.0.2"),
                nic("wlan0", "Wireless", true, "10.0.0.3"));
        assertTrue(NetworkInterfaceResolver.select("eth0", nics).isEmpty());
    }

    @Test
    @DisplayName("loopback is never selected")
    void loopbackIsNeverSelected() {
        assertTrue(NetworkInterfaceResolver.select("loopback_0", List.of(loopback())).isEmpty());
        assertTrue(NetworkInterfaceResolver.select("127.0.0.1", List.of(loopback())).isEmpty());
    }

    @Test
    @DisplayName("a blank or unmatched hint resolves to nothing rather than guessing")
    void blankOrUnmatchedHintYieldsEmpty() {
        assertTrue(NetworkInterfaceResolver.select(null, mediaTekMachine()).isEmpty());
        assertTrue(NetworkInterfaceResolver.select("", mediaTekMachine()).isEmpty());
        assertTrue(NetworkInterfaceResolver.select("   ", mediaTekMachine()).isEmpty());
        assertTrue(NetworkInterfaceResolver.select("no-such-adapter", mediaTekMachine()).isEmpty());
    }

    @Test
    @DisplayName("selection is deterministic when several usable adapters match")
    void ambiguousMatchIsDeterministicAndPrefersThePhysicalAdapter() {
        List<NicCandidate<String>> nics = List.of(
                nic("wireless_1", "Acme Wireless Card-QoS Packet Scheduler-0000", true, "10.0.0.9"),
                nic("wireless_0", "Acme Wireless Card-WFP Native MAC Layer LightWeight Filter-0000",
                        true, "10.0.0.8"),
                nic("wireless_2", "Acme Wireless Card", true, "10.0.0.7"));
        for (int i = 0; i < 5; i++) {
            assertEquals("wireless_2", NetworkInterfaceResolver.select("wireless", nics).orElseThrow().name());
        }
    }
}
