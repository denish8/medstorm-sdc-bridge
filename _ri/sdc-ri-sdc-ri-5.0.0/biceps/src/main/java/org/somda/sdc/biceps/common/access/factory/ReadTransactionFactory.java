package org.somda.sdc.biceps.common.access.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.biceps.common.access.ReadTransaction;
import org.somda.sdc.biceps.common.storage.MdibStorage;

import javax.annotation.Nullable;
import java.util.concurrent.locks.Lock;

/**
 * Factory to create {@linkplain ReadTransaction} objects.
 */
public interface ReadTransactionFactory {
    /**
     * Creates a read transaction intended for multiple read requests on an MDIB access object.
     *
     * @param mdibStorage the storage where to access data.
     * @param lock        the lock that is used for the lifetime of the read transaction.
     *                    The lock is gained on object construction and unlocked on auto-close.
     * @return a new {@link ReadTransaction} instance.
     */
    ReadTransaction createReadTransaction(@Assisted MdibStorage mdibStorage,
                                          @Assisted @Nullable Lock lock);
}
