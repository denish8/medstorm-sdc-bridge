package org.somda.sdc.glue.consumer.sco.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.glue.consumer.sco.ScoController;

import javax.annotation.Nullable;

public interface ScoControllerFactory {
    ScoController createScoController(@Assisted HostingServiceProxy hostingServiceProxy,
                                      @Assisted("setServiceProxy") @Nullable HostedServiceProxy setServiceProxy,
                                      @Assisted("contextServiceProxy") @Nullable
                                              HostedServiceProxy contextServiceProxy);
}
