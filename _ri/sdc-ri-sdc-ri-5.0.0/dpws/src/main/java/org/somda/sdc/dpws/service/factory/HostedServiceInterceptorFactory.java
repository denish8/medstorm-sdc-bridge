package org.somda.sdc.dpws.service.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.service.HostedService;
import org.somda.sdc.dpws.service.HostedServiceInterceptor;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryTargetService;

/**
 * Factory to create {@link HostedServiceInterceptor} instances.
 */
public interface HostedServiceInterceptorFactory {
    /**
     * Creates a hosted service interceptor.
     *
     * @param hostedService hosted service to create the interceptor for.
     * @param targetService the {@link WsDiscoveryTargetService} that is used to resolve hosting service types.
     * @return an interceptor for given hosted service.
     */
    HostedServiceInterceptor createHostedServiceInterceptor(@Assisted HostedService hostedService,
                                                            @Assisted WsDiscoveryTargetService targetService);
}
