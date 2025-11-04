package org.somda.sdc.glue.consumer.localization.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.glue.consumer.localization.LocalizationServiceProxy;

import javax.annotation.Nullable;

/**
 * Factory to create {@linkplain LocalizationServiceProxy} instances.
 */
public interface LocalizationServiceProxyFactory {
    /**
     * Creates a new {@linkplain LocalizationServiceProxy} instance.
     *
     * @param hostingServiceProxy from which to retrieve hosted localization service proxy.
     * @param hostedServiceProxy  the hosted service proxy which invokes network requests to the localization service.
     * @return a new {@linkplain LocalizationServiceProxy} instance.
     */
    LocalizationServiceProxy createLocalizationServiceProxy(
            @Assisted HostingServiceProxy hostingServiceProxy,
            @Assisted("localizationServiceProxy") @Nullable HostedServiceProxy hostedServiceProxy);
}
