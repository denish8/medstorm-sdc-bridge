package it.org.somda.sdc.dpws;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.somda.sdc.dpws.udp.UdpBindingService;
import org.somda.sdc.dpws.udp.factory.UdpBindingServiceFactory;

public class MockedUdpBindingModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new FactoryModuleBuilder()
                .implement(UdpBindingService.class, UdpBindingServiceMock.class)
                .build(UdpBindingServiceFactory.class));
    }
}
