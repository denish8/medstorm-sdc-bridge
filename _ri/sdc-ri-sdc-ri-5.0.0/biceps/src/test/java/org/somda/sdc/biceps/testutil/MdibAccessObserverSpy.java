package org.somda.sdc.biceps.testutil;

import com.google.common.eventbus.Subscribe;
import org.somda.sdc.biceps.common.access.MdibAccessObserver;
import org.somda.sdc.biceps.common.event.AbstractMdibAccessMessage;
import org.somda.sdc.common.util.AutoLock;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MdibAccessObserverSpy implements MdibAccessObserver {
    private final List<AbstractMdibAccessMessage> recordedMessages;
    private final ReentrantLock messageLock;
    private final Condition messageCondition;

    public MdibAccessObserverSpy() {
        this.recordedMessages = new ArrayList<>();
        this.messageLock = new ReentrantLock();
        this.messageCondition = messageLock.newCondition();
    }

    public List<AbstractMdibAccessMessage> getRecordedMessages() {
        try (AutoLock ignored = AutoLock.lock(messageLock)) {
            return recordedMessages;
        }
    }

    public boolean waitForNumberOfRecordedMessages(int messageNumber,
                                                   Duration waitTime) {
        try (AutoLock ignored = AutoLock.lock(messageLock)) {
            do {
                Instant start = Instant.now();
                try {
                    if (recordedMessages.size() == messageNumber) {
                        return true;
                    }
                    if (messageCondition.await(waitTime.toMillis(), TimeUnit.MILLISECONDS)) {
                        if (recordedMessages.size() == messageNumber) {
                            return true;
                        }
                    }
                } catch (InterruptedException e) {
                    return recordedMessages.size() == messageNumber;
                }

                Instant finish = Instant.now();
                waitTime = waitTime.minus(Duration.between(start, finish));
            } while (waitTime.toMillis() > 0);
            return recordedMessages.size() == messageNumber;
        }

    }

    public void reset() {
        try (AutoLock ignored = AutoLock.lock(messageLock)) {
            recordedMessages.clear();
        }
    }

    @Subscribe
    void onUpdate(AbstractMdibAccessMessage updates) {
        try (AutoLock ignored = AutoLock.lock(messageLock)) {
            recordedMessages.add(updates);
            messageCondition.signalAll();
        }
    }
}
