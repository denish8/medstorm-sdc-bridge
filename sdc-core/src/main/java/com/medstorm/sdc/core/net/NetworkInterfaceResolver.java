package com.medstorm.sdc.core.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Resolves a human-supplied network interface hint to a usable interface.
 *
 * <p>A hint may be an IPv4 address ({@code 192.168.1.6}), a JDK interface name
 * ({@code wlan0}, {@code wireless_32768}), a full adapter display name, or a
 * colloquial one ({@code Wi-Fi}). Matching is tried in that order, most specific
 * first.
 *
 * <p><strong>Only interfaces that could actually carry traffic are considered</strong>
 * &mdash; up, non-loopback, and holding at least one IPv4 address. Earlier
 * implementations matched on display-name substring alone and would select a
 * Windows filter pseudo-adapter, which is up and non-loopback but has no address.
 * Startup then failed several layers away with "No IPv4 address found on NIC
 * 'wireless_0'", inside Guice injector construction, on hardware that merely
 * enumerated its adapters in a different order.
 */
public final class NetworkInterfaceResolver {
    private static final Logger LOG = LoggerFactory.getLogger(NetworkInterfaceResolver.class);

    /** Colloquial hints that should match any wireless adapter. */
    private static final List<String> WIRELESS_ALIASES = List.of("wi-fi", "wifi", "wlan", "wireless");

    private NetworkInterfaceResolver() {
    }

    /**
     * Resolves {@code hint} against this machine's interfaces.
     *
     * @return the matching interface, or empty if nothing usable matched.
     */
    public static Optional<NetworkInterface> resolve(String hint) {
        List<NetworkInterface> all;
        try {
            all = Collections.list(NetworkInterface.getNetworkInterfaces());
        } catch (SocketException e) {
            LOG.error("Could not enumerate network interfaces: {}", e.toString());
            return Optional.empty();
        }
        return resolve(hint, all);
    }

    /**
     * Resolves {@code hint} against an explicit interface list.
     */
    public static Optional<NetworkInterface> resolve(String hint, List<NetworkInterface> interfaces) {
        List<NicCandidate<NetworkInterface>> candidates = interfaces.stream()
                .map(NetworkInterfaceResolver::toCandidate)
                .collect(Collectors.toList());

        Optional<NicCandidate<NetworkInterface>> match = select(hint, candidates);
        if (match.isPresent()) {
            NicCandidate<NetworkInterface> c = match.get();
            LOG.info("Resolved NIC hint '{}' to {}", hint, c.describe());
            return Optional.of(c.source());
        }

        LOG.error("No usable network interface matched '{}'. Usable interfaces are: {}",
                hint, describeUsable(candidates));
        return Optional.empty();
    }

    /**
     * The selection policy, over the reduced view of an interface.
     *
     * <p>Package-private so it can be tested against reproduced adapter topologies.
     */
    static <T> Optional<NicCandidate<T>> select(String hint, List<NicCandidate<T>> candidates) {
        if (hint == null || hint.isBlank()) {
            return Optional.empty();
        }
        final String trimmed = hint.trim();
        final String lower = trimmed.toLowerCase(Locale.ROOT);

        // Only interfaces that can carry traffic are eligible, whatever the hint says.
        List<NicCandidate<T>> usable = candidates.stream()
                .filter(NicCandidate::usable)
                .collect(Collectors.toList());

        warnAboutNamedButUnusable(trimmed, candidates, usable);

        // 1. An IPv4 literal is unambiguous, so it wins outright.
        Optional<NicCandidate<T>> byAddress = usable.stream()
                .filter(c -> c.ipv4Addresses().contains(trimmed))
                .findFirst();
        if (byAddress.isPresent()) {
            return byAddress;
        }

        // 2. Exact JDK interface name.
        Optional<NicCandidate<T>> byName = usable.stream()
                .filter(c -> trimmed.equalsIgnoreCase(c.name()))
                .findFirst();
        if (byName.isPresent()) {
            return byName;
        }

        // 3. Exact adapter display name.
        Optional<NicCandidate<T>> byDisplay = usable.stream()
                .filter(c -> trimmed.equalsIgnoreCase(c.displayName()))
                .findFirst();
        if (byDisplay.isPresent()) {
            return byDisplay;
        }

        // 4. Fuzzy: substring, plus colloquial wireless aliases. Several adapters
        //    routinely match here, so order deterministically and prefer the one that
        //    looks least like a filter pseudo-adapter.
        List<NicCandidate<T>> fuzzy = usable.stream()
                .filter(c -> matchesLoosely(c, lower))
                .sorted(Comparator
                        .comparingInt(NicCandidate<T>::pseudoAdapterScore)
                        // Filter adapters inherit the real adapter's name plus a suffix,
                        // so the shortest display name is the physical one.
                        .thenComparingInt(c -> lengthOf(c.displayName()))
                        .thenComparing(NicCandidate::name))
                .collect(Collectors.toList());

        if (fuzzy.isEmpty()) {
            return Optional.empty();
        }
        if (fuzzy.size() > 1) {
            LOG.warn("NIC hint '{}' matched {} usable interfaces; choosing {}. Pass an IPv4 address"
                            + " or exact interface name to remove the ambiguity. Candidates: {}",
                    hint, fuzzy.size(), fuzzy.get(0).name(),
                    fuzzy.stream().map(NicCandidate::describe).collect(Collectors.joining("; ")));
        }
        return Optional.of(fuzzy.get(0));
    }

    private static <T> boolean matchesLoosely(NicCandidate<T> candidate, String lowerHint) {
        String name = lowerOrEmpty(candidate.name());
        String display = lowerOrEmpty(candidate.displayName());

        if (name.contains(lowerHint) || display.contains(lowerHint)) {
            return true;
        }
        // "Wi-Fi" should find "wireless_32768", whose name shares no substring with it.
        if (WIRELESS_ALIASES.contains(lowerHint)) {
            return WIRELESS_ALIASES.stream().anyMatch(alias -> name.contains(alias) || display.contains(alias));
        }
        return false;
    }

    /**
     * If the hint names an interface exactly but that interface was filtered out, say so.
     * Otherwise the caller sees only "no match" and has no way to tell that their hint
     * was right and the interface was merely down or unaddressed.
     */
    private static <T> void warnAboutNamedButUnusable(String hint,
                                                      List<NicCandidate<T>> all,
                                                      List<NicCandidate<T>> usable) {
        all.stream()
                .filter(c -> !usable.contains(c))
                .filter(c -> hint.equalsIgnoreCase(c.name()) || hint.equalsIgnoreCase(c.displayName()))
                .forEach(c -> LOG.warn("Interface '{}' matches the hint but is not usable"
                                + " (up={}, loopback={}, ipv4={}); ignoring it.",
                        c.name(), c.up(), c.loopback(), c.ipv4Addresses()));
    }

    /** The first IPv4 address on an interface, if any. */
    public static Optional<InetAddress> firstIpv4(NetworkInterface nic) {
        if (nic == null) {
            return Optional.empty();
        }
        return Collections.list(nic.getInetAddresses()).stream()
                .filter(Inet4Address.class::isInstance)
                .filter(a -> !a.isLoopbackAddress())
                .findFirst();
    }

    /** Human-readable list of what the caller could have passed; used in error messages. */
    public static String describeUsableInterfaces() {
        try {
            return describeUsable(Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
                    .map(NetworkInterfaceResolver::toCandidate)
                    .collect(Collectors.toList()));
        } catch (SocketException e) {
            return "<could not enumerate: " + e + ">";
        }
    }

    private static <T> String describeUsable(List<NicCandidate<T>> candidates) {
        String usable = candidates.stream()
                .filter(NicCandidate::usable)
                .map(NicCandidate::describe)
                .collect(Collectors.joining("; "));
        return usable.isEmpty() ? "<none - no interface is up with an IPv4 address>" : usable;
    }

    private static NicCandidate<NetworkInterface> toCandidate(NetworkInterface nic) {
        boolean up = false;
        boolean loopback = false;
        try {
            up = nic.isUp();
            loopback = nic.isLoopback();
        } catch (SocketException e) {
            LOG.debug("Could not read flags for interface {}: {}", nic.getName(), e.toString());
        }

        List<String> ipv4 = new ArrayList<>();
        for (InetAddress address : Collections.list(nic.getInetAddresses())) {
            if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                ipv4.add(address.getHostAddress());
            }
        }
        return new NicCandidate<>(nic, nic.getName(), nic.getDisplayName(), up, loopback, ipv4);
    }

    private static String lowerOrEmpty(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static int lengthOf(String value) {
        return value == null ? Integer.MAX_VALUE : value.length();
    }
}
