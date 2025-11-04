package org.somda.sdc.biceps.common.access;

/**
 * Defines an interface to declare a class capable of starting read transactions.
 */
public interface ReadTransactionProvider {
    /**
     * Starts a read transaction.
     * <p>
     * Use the read transaction in an auto-closeable {@code try...} block in order to ensure releasing the lock that
     * is gained before this function returns.
     *
     * @return a new locked MDIB access transaction to operate on (read-only).
     */
    ReadTransaction startTransaction();
}
