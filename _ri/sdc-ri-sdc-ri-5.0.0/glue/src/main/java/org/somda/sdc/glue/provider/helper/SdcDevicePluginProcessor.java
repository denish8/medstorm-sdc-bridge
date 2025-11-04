package org.somda.sdc.glue.provider.helper;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.somda.sdc.glue.provider.SdcDeviceContext;
import org.somda.sdc.glue.provider.SdcDevicePlugin;

import java.util.Collection;

/**
 * Processes all {@linkplain org.somda.sdc.glue.provider.SdcDevicePlugin} instances
 * passed to an {@linkplain org.somda.sdc.glue.provider.SdcDevice}.
 */
public class SdcDevicePluginProcessor {
    private static final Logger LOG = LogManager.getLogger(SdcDevicePluginProcessor.class);

    private final SdcDeviceContext context;
    private final Collection<SdcDevicePlugin> devicePlugins;

    /**
     * Creates a processor with given dependencies.
     *
     * @param devicePlugins the actual plugins to process once a specific processing step is called.
     * @param context       the context data passed to each {@link SdcDevicePlugin} callback.
     */
    public SdcDevicePluginProcessor(Collection<SdcDevicePlugin> devicePlugins,
                                    SdcDeviceContext context) {
        this.context = context;
        this.devicePlugins = devicePlugins;
    }

    /**
     * Triggers {@link SdcDevicePlugin#beforeStartUp(SdcDeviceContext)} for all plugins.
     *
     * @throws Exception if an exception in one of the plugins was thrown.
     *                   In case an exception occurred, subsequent plugins are not being processed afterwards.
     */
    public void beforeStartUp() throws Exception {
        for (var devicePlugin : devicePlugins) {
            try {
                devicePlugin.beforeStartUp(context);
            } catch (Exception e) {
                LOG.warn("Plugin {} has thrown an exception before start up: {}", devicePlugin, e.getMessage());
                LOG.trace("Plugin {} has thrown an exception before start up", devicePlugin, e);
                throw e;
            }
        }
    }

    /**
     * Triggers {@link SdcDevicePlugin#afterStartUp(SdcDeviceContext)} for all plugins.
     *
     * @throws Exception if an exception in one of the plugins was thrown.
     *                   In case an exception occurred, subsequent plugins are not being processed afterwards.
     */
    public void afterStartUp() throws Exception {
        for (var devicePlugin : devicePlugins) {
            try {
                devicePlugin.afterStartUp(context);
            } catch (Exception e) {
                LOG.warn("Plugin {} has thrown an exception after start up: {}", devicePlugin, e.getMessage());
                LOG.trace("Plugin {} has thrown an exception after start up", devicePlugin, e);
                throw e;
            }
        }
    }

    /**
     * Triggers {@link SdcDevicePlugin#beforeShutDown(SdcDeviceContext)} (SdcDeviceContext)} for all plugins.
     * <p>
     * All plugins are executed no matter of any thrown exceptions during processing.
     */
    public void beforeShutDown() {
        for (var devicePlugin : devicePlugins) {
            try {
                devicePlugin.beforeShutDown(context);
            } catch (Exception e) {
                LOG.warn("Plugin {} has thrown an exception before shut down: {}", devicePlugin, e.getMessage());
                LOG.trace("Plugin {} has thrown an exception before shut down", devicePlugin, e);
            }
        }
    }

    /**
     * Triggers {@link SdcDevicePlugin#afterShutDown(SdcDeviceContext)} for all plugins.
     * <p>
     * All plugins are executed no matter of any thrown exceptions during processing.
     */
    public void afterShutDown() {
        for (var devicePlugin : devicePlugins) {
            try {
                devicePlugin.afterShutDown(context);
            } catch (Exception e) {
                LOG.warn("Plugin {} has thrown an exception after shut down: {}", devicePlugin, e.getMessage());
                LOG.trace("Plugin {} has thrown an exception after shut down", devicePlugin, e);
            }
        }
    }
}
