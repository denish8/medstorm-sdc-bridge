package org.somda.sdc.biceps.guice;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.MdibEntityImpl;
import org.somda.sdc.biceps.common.access.ReadTransaction;
import org.somda.sdc.biceps.common.access.ReadTransactionImpl;
import org.somda.sdc.biceps.common.access.factory.ReadTransactionFactory;
import org.somda.sdc.biceps.common.factory.MdibEntityGuiceAssistedFactory;
import org.somda.sdc.biceps.common.storage.MdibStorage;
import org.somda.sdc.biceps.common.storage.MdibStorageImpl;
import org.somda.sdc.biceps.common.storage.MdibStoragePreprocessingChain;
import org.somda.sdc.biceps.common.storage.factory.MdibStorageFactory;
import org.somda.sdc.biceps.common.storage.factory.MdibStoragePreprocessingChainFactory;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccessImpl;
import org.somda.sdc.biceps.consumer.access.factory.RemoteMdibAccessFactory;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.provider.access.LocalMdibAccessImpl;
import org.somda.sdc.biceps.provider.access.factory.LocalMdibAccessFactory;

/**
 * Default BICEPS module.
 */
public class DefaultBicepsModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new FactoryModuleBuilder()
                .implement(MdibEntity.class, MdibEntityImpl.class)
                .build(MdibEntityGuiceAssistedFactory.class));

        install(new FactoryModuleBuilder()
                .implement(MdibStoragePreprocessingChain.class, MdibStoragePreprocessingChain.class)
                .build(MdibStoragePreprocessingChainFactory.class));

        install(new FactoryModuleBuilder()
                .implement(MdibStorage.class, MdibStorageImpl.class)
                .build(MdibStorageFactory.class));

        install(new FactoryModuleBuilder()
                .implement(ReadTransaction.class, ReadTransactionImpl.class)
                .build(ReadTransactionFactory.class));

        install(new FactoryModuleBuilder()
                .implement(LocalMdibAccess.class, LocalMdibAccessImpl.class)
                .build(LocalMdibAccessFactory.class));

        install(new FactoryModuleBuilder()
                .implement(RemoteMdibAccess.class, RemoteMdibAccessImpl.class)
                .build(RemoteMdibAccessFactory.class));
    }
}
