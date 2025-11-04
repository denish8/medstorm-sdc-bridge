package org.somda.sdc.dpws.udp.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.udp.UdpBindingService;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.NetworkInterface;

/**
 * Factory to create {@link UdpBindingService} instances.
 */
public interface UdpBindingServiceFactory {

    /**
     * Creates a {@link UdpBindingService} instance.
     * <p>
     * The outgoing socket port is chosen by the operating system.
     *
     * @param networkInterface the network interface to bind the UDP socket to.
     * @param multicastAddress the UDP socket multicast address to join if desired (nullable for no multicast join).
     * @param multicastPort the UDP multicast socket port.
     * @param maxMessageSize maximum allowed message size for any messages sent over this binding.
     * @return the instance.
     */
    UdpBindingService createUdpBindingService(@Assisted NetworkInterface networkInterface,
                                              @Assisted @Nullable InetAddress multicastAddress,
                                              @Assisted("multicastPort") Integer multicastPort,
                                              @Assisted("maxMessageSize") Integer maxMessageSize);
}
