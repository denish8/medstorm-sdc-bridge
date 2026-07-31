package com.medstorm.sdcbridge.patch;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.somda.sdc.dpws.soap.wseventing.SourceSubscriptionManager;
import org.somda.sdc.dpws.soap.wseventing.SinkSubscriptionManager;
import org.somda.sdc.dpws.soap.wseventing.SinkSubscriptionManagerImpl;
import org.somda.sdc.dpws.soap.wseventing.factory.SubscriptionManagerFactory;

import org.somda.sdc.dpws.soap.wseventing.PatchedSourceSubscriptionManagerImpl;

public final class MedstormEventingPatchModule extends AbstractModule {
    private static final Logger log = LoggerFactory.getLogger(MedstormEventingPatchModule.class);

    @Override
    protected void configure() {
        log.warn("[PATCH] MedstormEventingPatchModule installed!");

        install(new FactoryModuleBuilder()
                .implement(SourceSubscriptionManager.class, PatchedSourceSubscriptionManagerImpl.class)
                .implement(SinkSubscriptionManager.class, SinkSubscriptionManagerImpl.class)
                .build(SubscriptionManagerFactory.class));
    }
}
