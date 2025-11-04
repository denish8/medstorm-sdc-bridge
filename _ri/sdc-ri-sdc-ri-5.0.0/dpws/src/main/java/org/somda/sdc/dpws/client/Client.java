package org.somda.sdc.dpws.client;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Service;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ProbeMatchesType;

/**
 * Core class to access DPWS client functionality.
 * <p>
 * Before access to client functions can be established, make sure to start the client service by using
 * {@link #startAsync()}.
 * Use {@link #stopAsync()} to stop the client service.
 * <p>
 * <em>Important note: in case the client does not work, check if your code starts the
 * {@link org.somda.sdc.dpws.DpwsFramework} in advance.</em>
 */
public interface Client extends Service {
    /**
     * Subscribes to discovery events.
     *
     * @param observer the observer that is supposed to receive events.
     */
    void registerDiscoveryObserver(DiscoveryObserver observer);

    /**
     * Unsubscribes from discovery events.
     *
     * @param observer the observer that shall not receive events anymore.
     */
    void unregisterDiscoveryObserver(DiscoveryObserver observer);

    /**
     * Probes for devices.
     * <p>
     * This method synchronously sends a WS-Discovery Probe.
     * All parties that subscribed to event messages by using {@link #registerDiscoveryObserver(DiscoveryObserver)}
     * will be notified on found devices and probe ending.
     *
     * @param discoveryFilter types and scopes the discovery process shall filter against.
     * @throws TransportException   if probe cannot be sent.
     * @throws InterceptorException if one of the interceptors pops up with an error.
     */
    void probe(DiscoveryFilter discoveryFilter) throws TransportException, InterceptorException;

    /**
     * Sends a directed probe to a specific physical address.
     * <p>
     * This method is an asynchronous unidirectional call; the result will not be notified to any subscribed parties.
     *
     * @param xAddr the device's physical address.
     * @return a future that holds the result of the directed probe.
     */
    ListenableFuture<ProbeMatchesType> directedProbe(String xAddr);

    /**
     * Resolves physical addresses (XAddrs) of a device.
     * <p>
     * This method is an asynchronous unidirectional call; the result will not be notified to any subscribed parties.
     *
     * @param eprAddress the endpoint reference address of the device to resolve.
     * @return a future that holds the result of the resolve.
     * @throws InterceptorException if one of the interceptors pops up with an error.
     */
    ListenableFuture<DiscoveredDevice> resolve(String eprAddress) throws InterceptorException;

    /**
     * Connects to a hosting service by using {@link DiscoveredDevice}.
     * <p>
     * This function requires a fully populated {@link DiscoveredDevice} including XAddrs.
     * <p>
     * By saying connect this method resolves a hosting service by using WS-TransferGet and hosted service information
     * by using WS-MetadataExchange.
     *
     * @param discoveredDevice a fully populated {@link DiscoveredDevice}.
     * @return a future that holds the result of the connect.
     */
    ListenableFuture<HostingServiceProxy> connect(DiscoveredDevice discoveredDevice);

    /**
     * Connects to a hosting service by using an EPR address.
     * <p>
     * Shortcut for first doing a {@link #resolve(String)} followed by a {@link #connect(DiscoveredDevice)}.
     *
     * @param eprAddress the EPR address of a device.
     * @return a future that holds the result of the connect.
     * @throws InterceptorException if one of the interceptors pops up with an error.
     */
    ListenableFuture<HostingServiceProxy> connect(String eprAddress) throws InterceptorException;
}
