package org.somda.sdc.dpws;

import com.google.common.util.concurrent.Service;

import java.net.NetworkInterface;
import java.util.Collection;

/**
 * Interface that supplies DPWS core functions.
 * <p>
 * This service is required to be started before any other interaction with the DPWS stack.
 * It is required once per Guice module.
 * <p>
 * {@linkplain DpwsFramework} is responsible to run a multicast UDP queue in order to send and
 * receive discovery messages.
 * Moreover, it prints out a list of network adapters for information purposes.
 * <p>
 * Do not forget to stop the DPWS framework for a graceful shutdown.
 */
public interface DpwsFramework extends Service {

    /**
     * Sets the network interface to be used by the framework.
     * <p>
     * <em>This may only be set while the framework isn't running.</em>
     *
     * @param networkInterface a network interface.
     */
    void setNetworkInterface(NetworkInterface networkInterface);

    /**
     * Registers a service to attach to the framework's lifecycle.
     * <p>
     * Starts and shuts down registered services when starting and stopping the framework.
     * Whenever a constructor (outside of the dpws package) receives a wrapped thread pool, it must register the
     * service using this method to ensure it is properly cleaned up when shutting down the device but not the JVM.
     * <p>
     * <ul>
     * <li><em>Services registered when the framework is already running will be started.</em></li>
     * <li><em>Services will be shutdown in the inverse order they're registered in,
     * i.e. the last service registered will be the first to shut down.</em></li>
     * </ul>
     * @param services {@linkplain Service}s to register for startup and shutdown.
     */
    void registerService(Collection<Service> services);
}
