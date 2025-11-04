package org.somda.sdc.dpws.udp;

import com.google.common.eventbus.Subscribe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.dpws.DpwsTest;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

class UdpMessageQueueServiceTest extends DpwsTest {
    private UdpMessageQueueServiceImpl udpMsgQueue;
    private byte[] actualMessage;
    private Condition condition;
    private Lock lock;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        udpMsgQueue = getInjector().getInstance(UdpMessageQueueServiceImpl.class);
        final UdpBindingServiceMock udpBindingServiceMock = new UdpBindingServiceMock();
        udpMsgQueue.setUdpBinding(udpBindingServiceMock);
        udpBindingServiceMock.setMessageReceiver(udpMsgQueue);
        lock = new ReentrantLock();
        condition = lock.newCondition();
        actualMessage = null;
    }

    @Test
    void sendAndReceiveMessage() throws Exception {
        String expectedMsg = "sample";
        udpMsgQueue.startAsync().awaitRunning();
        udpMsgQueue.registerUdpMessageQueueObserver(new UdpMessageQueueObserver() {
            @Subscribe
            private void receive(UdpMessage msg) {
                lock.lock();
                try {
                    actualMessage = msg.getData();
                    condition.signalAll();
                } finally {
                    lock.unlock();
                }
            }
        });

        UdpMessage msg = new UdpMessage(expectedMsg.getBytes(), expectedMsg.length());
        udpMsgQueue.sendMessage(msg);
        lock.lock();
        try {
            if (actualMessage == null) {
                condition.await(1, TimeUnit.HOURS);
            }
            assertThat(actualMessage, is(notNullValue(null)));
            assertThat(actualMessage, is(expectedMsg.getBytes()));
        } finally {
            lock.unlock();
        }
    }
}