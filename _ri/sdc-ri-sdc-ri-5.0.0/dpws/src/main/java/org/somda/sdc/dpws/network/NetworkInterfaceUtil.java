package org.somda.sdc.dpws.network;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Iterator;
import java.util.Optional;

/**
 * Utility class for network interface related functionality.
 */
public class NetworkInterfaceUtil {

    /**
     * Loops through a network interface's addresses and returns the first one that matches an IPv4 address.
     *
     * @param networkInterface the network interface where to seek for an IPv4 address.
     * @return the first found IPv4 address or empty if there is no IPv4 address connected to the network interface.
     */
    public Optional<InetAddress> getFirstIpV4Address(NetworkInterface networkInterface) {
        final Iterator<InetAddress> inetAddressIterator = networkInterface.getInetAddresses().asIterator();
        while (inetAddressIterator.hasNext()) {
            final InetAddress nextAddress = inetAddressIterator.next();
            if (nextAddress instanceof Inet4Address) {
                return Optional.of(nextAddress);
            }
        }
        return Optional.empty();
    }
}
