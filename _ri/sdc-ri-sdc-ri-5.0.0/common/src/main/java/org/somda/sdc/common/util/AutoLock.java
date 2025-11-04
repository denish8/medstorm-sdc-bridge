package org.somda.sdc.common.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Converts locks to auto-closables.
 *
 * @param <T> the lock type to auto-close.
 */
public class AutoLock<T extends Lock> implements Lock, AutoCloseable {
    private final T lock;

    private AutoLock(T lock) {
        this.lock = lock;
    }

    /**
     * Locks the given lock and return auto-closable variant.
     *
     * @param lock the lock to wrap.
     * @param <T> type of the lock (write/read/common).
     * @return an auto-closable instance to be used with <code>try (...) { ... }</code>.
     */
     public static <T extends Lock> AutoLock<T> lock(T lock) {
        lock.lock();
        return new AutoLock<>(lock);
    }

    /**
     * Returns the wrapped lock instance.
     *
     * @return the wrapped lock.
     */
    public T getLock() {
        return lock;
    }

    @Override
    public void close() {
        lock.unlock();
    }

    @Override
    public void lock() {
        lock.lock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        lock.lockInterruptibly();
    }

    @Override
    public boolean tryLock() {
        return lock.tryLock();
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return lock.tryLock(time, unit);
    }

    @Override
    public void unlock() {
        lock.unlock();
    }

    @Override
    public Condition newCondition() {
        return lock.newCondition();
    }
}
