package org.somda.sdc.biceps.common.access;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.storage.MdibStorage;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.MdibVersion;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

/**
 * Default implementation of {@linkplain ReadTransaction}.
 */
public class ReadTransactionImpl implements ReadTransaction {
    private final Lock lock;
    private final MdibStorage mdibStorage;

    @AssistedInject
    public ReadTransactionImpl(@Assisted MdibStorage mdibStorage,
                               @Assisted Lock lock) {
        this.lock = lock;
        this.lock.lock();
        this.mdibStorage = mdibStorage;
    }

    @Override
    public void close() {
        lock.unlock();
    }

    @Override
    public MdibVersion getMdibVersion() {
        return mdibStorage.getMdibVersion();
    }

    @Override
    public BigInteger getMdDescriptionVersion() {
        return mdibStorage.getMdDescriptionVersion();
    }

    @Override
    public BigInteger getMdStateVersion() {
        return mdibStorage.getMdStateVersion();
    }

    @Override
    public <T extends AbstractDescriptor> Optional<T> getDescriptor(String handle, Class<T> descrClass) {
        return mdibStorage.getDescriptor(handle, descrClass);
    }

    @Override
    public Optional<AbstractDescriptor> getDescriptor(String handle) {
        return getDescriptor(handle, AbstractDescriptor.class);
    }

    @Override
    public Optional<MdibEntity> getEntity(String handle) {
        return mdibStorage.getEntity(handle);
    }

    @Override
    public List<MdibEntity> getRootEntities() {
        return mdibStorage.getRootEntities();
    }

    @Override
    public Optional<AbstractState> getState(String handle) {
        return mdibStorage.getState(handle);
    }

    @Override
    public <T extends AbstractState> Optional<T> getState(String handle, Class<T> stateClass) {
        return mdibStorage.getState(handle, stateClass);
    }

    @Override
    public <T extends AbstractState> List<T> getStatesByType(Class<T> stateClass) {
        return mdibStorage.getStatesByType(stateClass);
    }

    @Override
    public <T extends AbstractContextState> List<T> getContextStates(String descriptorHandle, Class<T> stateClass) {
        return mdibStorage.getContextStates(descriptorHandle, stateClass);
    }

    @Override
    public List<AbstractContextState> getContextStates(String descriptorHandle) {
        return mdibStorage.getContextStates(descriptorHandle);
    }

    @Override
    public List<AbstractContextState> getContextStates() {
        return mdibStorage.getContextStates();
    }

    @Override
    public <T extends AbstractContextState> List<T> findContextStatesByType(Class<T> stateClass) {
        return mdibStorage.findContextStatesByType(stateClass);
    }

    @Override
    public <T extends AbstractDescriptor> Collection<MdibEntity> findEntitiesByType(Class<T> type) {
        return mdibStorage.findEntitiesByType(type);
    }

    @Override
    public <T extends AbstractDescriptor> List<MdibEntity> getChildrenByType(String handle, Class<T> type) {
        return mdibStorage.getChildrenByType(handle, type);
    }
}
