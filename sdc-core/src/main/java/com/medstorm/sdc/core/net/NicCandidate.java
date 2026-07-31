package com.medstorm.sdc.core.net;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A network interface reduced to the facts interface selection actually depends on.
 *
 * <p>Exists so the selection policy can be tested. {@link java.net.NetworkInterface}
 * is final with no public constructor, so a real machine's adapter topology cannot
 * otherwise be reproduced in a test &mdash; which is precisely how a selection bug
 * survives until it meets unfamiliar hardware.
 *
 * @param <T> the underlying interface object, carried through so the caller gets it back.
 */
public record NicCandidate<T>(
        T source,
        String name,
        String displayName,
        boolean up,
        boolean loopback,
        List<String> ipv4Addresses) {

    /**
     * Markers that identify Windows filter and miniport pseudo-adapters.
     *
     * <p>Windows exposes one pseudo-adapter per filter driver bound to a NIC, each
     * inheriting the physical adapter's display name with a suffix. A MediaTek
     * MT7920 presents as nine {@code wireless_*} interfaces, of which one is real.
     * They are only deprioritised, never excluded outright &mdash; the address check
     * does the real filtering, and an explicit name match still wins.
     */
    private static final List<String> PSEUDO_ADAPTER_MARKERS = List.of(
            "wfp",
            "lightweight filter",
            "filter driver",
            "packet scheduler",
            "miniport",
            "kernel debug",
            "virtual");

    public NicCandidate {
        Objects.requireNonNull(name, "name");
        ipv4Addresses = List.copyOf(ipv4Addresses);
    }

    /**
     * Whether this interface can actually carry SDC traffic.
     *
     * <p>An interface that is up but has no IPv4 address cannot host the HTTP server
     * or send WS-Discovery, so selecting one is always a mistake regardless of how
     * well its name matches.
     */
    public boolean usable() {
        return up && !loopback && !ipv4Addresses.isEmpty();
    }

    /** How many pseudo-adapter markers appear in the display name; lower is better. */
    int pseudoAdapterScore() {
        String haystack = (displayName == null ? "" : displayName).toLowerCase(Locale.ROOT);
        int score = 0;
        for (String marker : PSEUDO_ADAPTER_MARKERS) {
            if (haystack.contains(marker)) {
                score++;
            }
        }
        return score;
    }

    String describe() {
        return name + " (" + (displayName == null ? "?" : displayName) + ")"
                + " up=" + up
                + " ipv4=" + ipv4Addresses;
    }
}
