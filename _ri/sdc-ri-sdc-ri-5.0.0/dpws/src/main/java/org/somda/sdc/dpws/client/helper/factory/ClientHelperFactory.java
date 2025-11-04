package org.somda.sdc.dpws.client.helper.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.client.helper.DiscoveredDeviceResolver;
import org.somda.sdc.dpws.client.helper.DiscoveryClientUdpProcessor;
import org.somda.sdc.dpws.client.helper.HelloByeAndProbeMatchesObserverImpl;
import org.somda.sdc.dpws.soap.NotificationSink;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryClient;

/**
 * Factory to create util objects for the DPWS client.
 */
public interface ClientHelperFactory {
    /**
     * Creates a resolver for discovered devices.
     *
     * @param wsDiscoveryClient the discovery client needed to formulate resolve requests.
     * @return a {@link DiscoveredDeviceResolver} instance.
     */
    DiscoveredDeviceResolver createDiscoveredDeviceResolver(@Assisted WsDiscoveryClient wsDiscoveryClient);

    /**
     * Creates a {@linkplain DiscoveryClientUdpProcessor} instance.
     *
     * @param notificationSink notification that processes incoming {@link SoapMessage} instances.
     * @return a configured {@link DiscoveryClientUdpProcessor} instance.
     */
    DiscoveryClientUdpProcessor createDiscoveryClientUdpProcessor(@Assisted NotificationSink notificationSink);

    /**
     * Creates a discovery observer that listens to Hello, Bye and ProbeMatches messages.
     *
     * @param discoveredDeviceResolver the resolver that is used if hello messages do not contain XAddrs.
     * @return the oberserver instance.
     */
    HelloByeAndProbeMatchesObserverImpl createDiscoveryObserver(
        @Assisted DiscoveredDeviceResolver discoveredDeviceResolver);
}
