package org.somda.sdc.dpws.udp;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.soap.exception.TransportException;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default implementation of {@linkplain UdpMessageQueueService}.
 */
public class UdpMessageQueueServiceImpl extends AbstractIdleService implements Service, UdpMessageQueueService {
    private static final Logger LOG = LogManager.getLogger(UdpMessageQueueServiceImpl.class);
    private static final AtomicInteger INSTANCE_ID_COUNTER = new AtomicInteger(0);
    private final int instanceId;
    private final LinkedBlockingDeque<UdpMessage> incomingMessageQueue;
    private final LinkedBlockingDeque<UdpMessage> outgoingMessageQueue;
    private final EventBus eventBus;
    private final Logger instanceLogger;

    private UdpBindingService udpBinding;
    private Thread outgoingThread;
    private Thread incomingThread;

    @Inject
    UdpMessageQueueServiceImpl(EventBus eventBus, @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.instanceId = INSTANCE_ID_COUNTER.getAndIncrement();
        this.incomingMessageQueue = new LinkedBlockingDeque<>();
        this.outgoingMessageQueue = new LinkedBlockingDeque<>();
        this.eventBus = eventBus;
        this.udpBinding = null;
    }

    @Override
    public void setUdpBinding(UdpBindingService udpBinding) {
        this.udpBinding = udpBinding;
    }

    @Override
    public boolean sendMessage(UdpMessage message) {
        return isRunning() && outgoingMessageQueue.offer(message);
    }

    @Override
    public void registerUdpMessageQueueObserver(UdpMessageQueueObserver observer) {
        eventBus.register(observer);
    }

    @Override
    public void unregisterUdpMessageQueueObserver(UdpMessageQueueObserver observer) {
        eventBus.unregister(observer);
    }

    @Override
    protected void startUp() throws Exception {
        instanceLogger.info("[{}] Start UDP message queue for binding {}", instanceId, udpBinding);
        if (udpBinding == null) {
            String msg = "Cannot startup without UDP binding";
            instanceLogger.warn("[{}] {}", instanceId, msg);
            throw new Exception(msg);
        }

        startProcessingOfIncomingMessages();
        startProcessingOfOutgoingMessages();

        instanceLogger.info("[{}] UDP message queue for binding {} is running", instanceId, udpBinding);
    }

    private void startProcessingOfOutgoingMessages() {
        outgoingThread = new Thread(() -> {
            try {
                do {
                    try {
                        UdpMessage message = outgoingMessageQueue.take();
                        instanceLogger.trace("[{}] Outgoing UdpMessageQueueService received UDP message, " +
                                "sending: {}", instanceId, message);
                        udpBinding.sendMessage(message);
                    } catch (IOException e) {
                        instanceLogger.warn("[{}] Outgoing UdpMessageQueueService IO exception caught",
                                instanceId, e);
                    } catch (InterruptedException e) {
                        instanceLogger.info("[{}] Outgoing UdpMessageQueueService interrupted", instanceId);
                        instanceLogger.trace("[{}] Outgoing UdpMessageQueueService interrupted", instanceId, e);
                        break;
                    } catch (TransportException e) {
                        instanceLogger.info("[{}] Outgoing UdpMessageQueueService transport exception caught",
                                instanceId, e);
                    }
                } while (true);
            } finally {
            instanceLogger.info("[{}] Outgoing UdpMessageQueueService ended", instanceId);
            }
        });
        outgoingThread.setName(String.format("[%s] Outgoing UdpMessageQueueService", instanceId));
        outgoingThread.setDaemon(true);
        outgoingThread.start();
    }

    private void startProcessingOfIncomingMessages() {
        incomingThread = new Thread(() -> {
            try {
                do {
                    try {
                        UdpMessage message = incomingMessageQueue.take();
                        instanceLogger.trace("[{}] Incoming UdpMessageQueueService received UDP message, " +
                                "posting to EventBus: {}", instanceId, message);
                        eventBus.post(message);
                    } catch (InterruptedException e) {
                        instanceLogger.info("[{}] Incoming UdpMessageQueueService interrupted", instanceId);
                        instanceLogger.trace("[{}] Incoming UdpMessageQueueService interrupted", instanceId, e);
                        break;
                        // CHECKSTYLE.OFF: IllegalCatch
                    } catch (Exception e) {
                        // CHECKSTYLE.ON: IllegalCatch
                        instanceLogger.warn("[{}] Incoming UdpMessageQueueService encountered an error on " +
                                "event dissemination", instanceId, e);
                    }
                } while (true);
            } finally {
                instanceLogger.info("[{}] Incoming UdpMessageQueueService ended", instanceId);
            }
        });
        incomingThread.setName(String.format("[%s] Incoming UdpMessageQueueService", instanceId));
        incomingThread.setDaemon(true);
        incomingThread.start();
    }

    @Override
    protected void shutDown() {
        instanceLogger.info("[{}] Shut down UDP message queue for binding {}", instanceId, udpBinding);
        incomingMessageQueue.clear();
        outgoingMessageQueue.clear();
        incomingThread.interrupt();
        outgoingThread.interrupt();
        instanceLogger.info("[{}] UDP message queue for binding {} shut down", instanceId, udpBinding);
    }

    @Override
    public void receive(UdpMessage udpMessage) {
        instanceLogger.debug("[{}] Received UDP message, adding to queue", instanceId);
        if (!incomingMessageQueue.offer(udpMessage)) {
            instanceLogger.error("[{}] Lost incoming UDP message in message queue: {}", instanceId, udpMessage);
        }
    }
}
