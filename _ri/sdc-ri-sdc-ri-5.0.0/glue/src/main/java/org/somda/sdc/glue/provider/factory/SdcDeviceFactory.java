package org.somda.sdc.glue.provider.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.dpws.device.DeviceSettings;
import org.somda.sdc.dpws.service.HostedService;
import org.somda.sdc.glue.provider.SdcDevice;
import org.somda.sdc.glue.provider.SdcDevicePlugin;
import org.somda.sdc.glue.provider.localization.LocalizationService;
import org.somda.sdc.glue.provider.localization.LocalizationStorage;
import org.somda.sdc.glue.provider.sco.OperationInvocationReceiver;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * Factory to create SDC compatible devices.
 */
public interface SdcDeviceFactory {
    /**
     * Creates a new {@linkplain SdcDevice} instance.
     *
     * @param deviceSettings               the DPWS device settings to use.
     * @param mdibAccess                   the MDIB to be exposed on the network.
     * @param operationInvocationReceivers callback interceptors for incoming set service requests.
     * @param plugins                      {@link org.somda.sdc.glue.provider.plugin.SdcRequiredTypesAndScopes} if the
     *                                     collection is empty or custom plugins to run on start up and shut down in the
     *                                     order given by this collection.
     *                                     Make sure plugins are independent to each other.
     *                                     Also make sure that at least the
     *                                     {@link org.somda.sdc.glue.provider.plugin.SdcRequiredTypesAndScopes}
     *                                     or a functionally equivalent plugin is loaded.
     *                                     See {@link SdcDevicePlugin} for more details.
     * @return a new {@link SdcDevice}.
     * Use {@link SdcDevice#startAsync()} in order to start exposing the device on the network.
     */
    SdcDevice createSdcDevice(@Assisted DeviceSettings deviceSettings,
                              @Assisted LocalMdibAccess mdibAccess,
                              @Assisted("operationInvocationReceivers")
                                      Collection<OperationInvocationReceiver> operationInvocationReceivers,
                              @Assisted("plugins") Collection<SdcDevicePlugin> plugins);

    /**
     * Creates a new {@linkplain SdcDevice} instance.
     *
     * @param deviceSettings               the DPWS device settings to use.
     * @param mdibAccess                   the MDIB to be exposed on the network.
     * @param operationInvocationReceivers callback interceptors for incoming set service requests.
     * @param plugins                      {@link org.somda.sdc.glue.provider.plugin.SdcRequiredTypesAndScopes} if the
     *                                     collection is empty or custom plugins to run on start up and shut down in the
     *                                     order given by this collection.
     *                                     Make sure plugins are independent to each other.
     *                                     Also make sure that at least the
     *                                     {@link org.somda.sdc.glue.provider.plugin.SdcRequiredTypesAndScopes}
     *                                     or a functionally equivalent plugin is loaded.
     *                                     See {@link SdcDevicePlugin} for more details.
     * @param localizationStorage          optional {@link LocalizationStorage} implementation.
     *                                     If provided it's used by the {@link LocalizationService} to fetch
     *                                     localized texts.
     *                                     {@link LocalizationService} is enabled for the device only if localization
     *                                     storage is provided.
     * @param customHostedServices         list of services to be started in addition to SDC services.
     * @return a new {@link SdcDevice}.
     * Use {@link SdcDevice#startAsync()} in order to start exposing the device on the network.
     */
    SdcDevice createSdcDevice(@Assisted DeviceSettings deviceSettings,
                              @Assisted LocalMdibAccess mdibAccess,
                              @Assisted("operationInvocationReceivers")
                                      Collection<OperationInvocationReceiver> operationInvocationReceivers,
                              @Assisted("plugins") Collection<SdcDevicePlugin> plugins,
                              @Assisted @Nullable LocalizationStorage localizationStorage,
                              @Assisted List<HostedService> customHostedServices);
}
