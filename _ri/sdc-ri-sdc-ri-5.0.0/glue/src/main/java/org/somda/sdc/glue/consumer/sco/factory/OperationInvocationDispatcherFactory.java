package org.somda.sdc.glue.consumer.sco.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.glue.consumer.sco.helper.OperationInvocationDispatcher;

public interface OperationInvocationDispatcherFactory {
    OperationInvocationDispatcher createOperationInvocationDispatcher(
            @Assisted HostingServiceProxy hostingServiceProxy);
}
