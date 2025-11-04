package test.org.somda.common;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class TimedWait<T> {
    private final ReentrantLock reentrantLock;
    private final Condition condition;
    private final Supplier<T> resetSupplier;
    private T data;

    public TimedWait(Supplier<T> resetSupplier) {
        this.resetSupplier = resetSupplier;
        this.reentrantLock = new ReentrantLock();
        this.condition = reentrantLock.newCondition();
        this.data = this.resetSupplier.get();
    }

    public T getData() {
        try {
            reentrantLock.lock();
            return data;
        } finally {
            reentrantLock.unlock();
        }
    }

    public void modifyData(Consumer<T> modifierCallback) {
        try {
            reentrantLock.lock();
            modifierCallback.accept(data);
            condition.signalAll();
        } finally {
            reentrantLock.unlock();
        }
    }

    public boolean waitForData(Predicate<T> dataCondition, Duration waitTime) {
        var copyWaitTime = waitTime;
        try {
            reentrantLock.lock();
            do {
                Instant start = Instant.now();
                try {
                    if (dataCondition.test(data)) {
                        return true;
                    }
                    if (condition.await(copyWaitTime.toMillis(), TimeUnit.MILLISECONDS) && dataCondition.test(data)) {
                        return true;
                    }
                } catch (InterruptedException e) {
                    return dataCondition.test(data);
                }

                Instant finish = Instant.now();
                copyWaitTime = copyWaitTime.minus(Duration.between(start, finish));
            } while (copyWaitTime.toMillis() > 0);
            return dataCondition.test(data);
        } finally {
            reentrantLock.unlock();
        }
    }

    public void reset() {
        try {
            reentrantLock.lock();
            data = resetSupplier.get();
        } finally {
            reentrantLock.unlock();
        }
    }
}
