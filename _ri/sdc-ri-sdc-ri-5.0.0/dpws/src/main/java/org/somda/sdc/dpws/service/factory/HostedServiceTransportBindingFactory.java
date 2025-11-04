package org.somda.sdc.dpws.service.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostedServiceTransportBinding;

/**
 * Factory to create {@link HostedServiceTransportBinding} instances.
 *
 * todo DGr this factory is not used; remove
 */
public interface HostedServiceTransportBindingFactory {
    /**
     * Create instance.
     *
     * @param hostedServiceProxy The hosted service to bind to.
     * @return transport binding for hosted service
     */
    HostedServiceTransportBinding createHostedServiceTransportBinding(@Assisted HostedServiceProxy hostedServiceProxy);
}
