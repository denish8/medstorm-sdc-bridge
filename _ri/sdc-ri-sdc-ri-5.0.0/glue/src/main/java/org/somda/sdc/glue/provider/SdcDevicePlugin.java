package org.somda.sdc.glue.provider;

import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.dpws.device.Device;
import org.somda.sdc.glue.provider.sco.OperationInvocationReceiver;

/**
 * Definition of a plugin for {@linkplain SdcDevice} that is called back on different stages of startup and shutdown.
 * <p>
 * Every callback provides full control of {@link Device}.
 * There is also access to the {@link LocalMdibAccess} and {@link OperationInvocationReceiver} collection passed to
 * the {@link SdcDevice} constructor via {@link org.somda.sdc.glue.provider.factory.SdcDeviceFactory}.
 * <p>
 * All of the previously mentioned data is accessible through {@link SdcDeviceContext}.
 * <p>
 * The plugin mechanism can be used in order to run {@link LocalMdibAccess} controllers.
 */
public interface SdcDevicePlugin {
    /**
     * Called before the device starts up.
     *
     * @param context all accessible {@link SdcDevice} data.
     * @throws Exception any exception thrown will immediately stop the device from continuing startup.
     *                   Calls for {@link #afterStartUp(SdcDeviceContext)}, {@link #beforeShutDown(SdcDeviceContext)}
     *                   and {@link #afterShutDown(SdcDeviceContext)} are omitted.
     *                   No further plugin calls for this function are going to be invoked after.
     */
    default void beforeStartUp(SdcDeviceContext context) throws Exception {
    }

    /**
     * Called once the device is running.
     * <p>
     * This callback is triggered only if the device was running before,
     * i.e., {@link SdcDevice#isRunning()} returned true.
     *
     * @param context all accessible {@link SdcDevice} data.
     * @throws Exception any exception thrown will immediately stop the device from continuing startup.
     *                   Calls for {@link #beforeShutDown(SdcDeviceContext)}
     *                   and {@link #afterShutDown(SdcDeviceContext)} are omitted.
     *                   No further plugin calls for this function are going to be invoked after.
     */
    default void afterStartUp(SdcDeviceContext context) throws Exception {
    }

    /**
     * Called before the device is about to shutdown.
     * <p>
     * This callback is triggered only if the device was running before,
     * i.e., {@link SdcDevice#isRunning()} returned true.
     *
     * @param context all accessible {@link SdcDevice} data.
     * @throws Exception any exception thrown does not stop the device from shutting down.
     *                   A message is sent to the logger, any subsequent plugins will be invoked and
     *                   internal device functions will be stopped.
     *                   {@link #afterShutDown(SdcDeviceContext)} is still going to be invoked.
     */
    default void beforeShutDown(SdcDeviceContext context) throws Exception {
    }

    /**
     * Called after the device was shut down.
     * <p>
     * This callback is triggered only if the device was running before,
     * i.e., {@link SdcDevice#isRunning()} returned true.
     *
     * @param context all accessible {@link SdcDevice} data.
     * @throws Exception any exception thrown does not affect any other plugins from being invoked.
     *                   A message will be sent to the logger.
     */
    default void afterShutDown(SdcDeviceContext context) throws Exception {
    }
}
