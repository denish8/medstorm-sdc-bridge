package org.somda.sdc.dpws.soap.wseventing;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.soap.NotificationSource;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.factory.NotificationSourceFactory;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wseventing.helper.SubscriptionManagerBase;
import org.somda.sdc.dpws.soap.wseventing.model.Notification;
import org.somda.sdc.dpws.soap.wseventing.model.ObjectFactory;
import org.somda.sdc.dpws.soap.wseventing.model.WsEventingStatus;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Default implementation of {@link SourceSubscriptionManager}.
 */
public class SourceSubscriptionManagerImpl extends AbstractExecutionThreadService implements SourceSubscriptionManager {
    private static final Logger LOG = LogManager.getLogger(SourceSubscriptionManagerImpl.class);

    private final BlockingQueue<QueueItem> notificationQueue;
    private final SubscriptionManagerBase delegate;
    private final NotificationSourceFactory notificationSourceFactory;
    private final TransportBindingFactory transportBindingFactory;
    private final WsAddressingUtil wsaUtil;

    private final Logger instanceLogger;
    private final ObjectFactory wseFactory;
    private final SoapUtil soapUtil;

    private NotificationSource notifyToSender;
    private NotificationSource endToSender;

    private String subscriptionId;
    private String notifyToUri;
    @Nullable
    private final String callerId;

    @AssistedInject
    SourceSubscriptionManagerImpl(@Assisted("SubscriptionManager") EndpointReferenceType subscriptionManagerEpr,
                                  @Assisted Duration expires,
                                  @Assisted("NotifyTo") EndpointReferenceType notifyTo,
                                  @Assisted("EndTo") @Nullable EndpointReferenceType endTo,
                                  @Assisted("SubscriptionId") String subscriptionId,
                                  @Assisted("Filters") List<Object> filters,
                                  @Assisted("FilterDialect") String filterDialect,
                                  @Assisted("callerId") @Nullable String callerId,
                                  @Named(WsEventingConfig.NOTIFICATION_QUEUE_CAPACITY)
                                  Integer notificationQueueCapacity,
                                  NotificationSourceFactory notificationSourceFactory,
                                  TransportBindingFactory transportBindingFactory,
                                  WsAddressingUtil wsaUtil,
                                  ObjectFactory wseFactory,
                                  SoapUtil soapUtil,
                                  @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.notificationSourceFactory = notificationSourceFactory;
        this.transportBindingFactory = transportBindingFactory;
        this.wsaUtil = wsaUtil;
        this.subscriptionId = UUID.randomUUID().toString();
        this.delegate = new SubscriptionManagerBase(
                notifyTo, endTo, subscriptionId, expires, subscriptionManagerEpr, filters, filterDialect);
        this.notificationQueue = new ArrayBlockingQueue<>(notificationQueueCapacity);
        this.wseFactory = wseFactory;
        this.soapUtil = soapUtil;
        this.notifyToSender = null;
        this.endToSender = null;
        this.notifyToUri = "";
        this.callerId = callerId;
    }

    @Override
    public String getSubscriptionId() {
        return delegate.getSubscriptionId();
    }

    @Override
    public Instant getExpiresTimeout() {
        return delegate.getExpiresTimeout();
    }

    @Override
    public EndpointReferenceType getNotifyTo() {
        return delegate.getNotifyTo();
    }

    @Override
    public Optional<EndpointReferenceType> getEndTo() {
        return delegate.getEndTo();
    }

    @Override
    public Duration getExpires() {
        return delegate.getExpires();
    }

    @Override
    public EndpointReferenceType getSubscriptionManagerEpr() {
        return delegate.getSubscriptionManagerEpr();
    }

    @Override
    public List<Object> getFilters() {
        return delegate.getFilters();
    }

    @Override
    public String getFilterDialect() {
        return delegate.getFilterDialect();
    }

    @Override
    public void renew(Duration expires) {
        delegate.renew(expires);
    }

    @Override
    public Optional<String> getCallerId() {
        return Optional.ofNullable(callerId);
    }

    @Override
    public void offerNotification(Notification notification) {
        if (!isRunning()) {
            return;
        }
        if (!notificationQueue.offer(new NotificationItem(notification))) {
            stopAsync().awaitTerminated();
        }
    }

    @Override
    public void offerEndTo(SoapMessage endToMessage) {
        if (!isRunning() || endToSender == null) {
            return;
        }

        if (!notificationQueue.offer(new SubscriptionEndItem(new Notification(endToMessage)))) {
            stopAsync().awaitTerminated();
        }
    }

    @Override
    public void offerEndTo(WsEventingStatus status) {
        createEndToMessage(status).ifPresent(this::offerEndTo);
    }

    @Override
    protected void startUp() {
        notifyToUri = wsaUtil.getAddressUri(getNotifyTo()).orElseThrow(() ->
                new RuntimeException("Invalid notify-to EPR"));
        this.notifyToSender = notificationSourceFactory.createNotificationSource(
                transportBindingFactory.createTransportBinding(notifyToUri, null));

        if (getEndTo().isPresent()) {
            final Optional<String> addressUriAsString = wsaUtil.getAddressUri(getEndTo().get());
            addressUriAsString.ifPresent(s -> this.endToSender = notificationSourceFactory.createNotificationSource(
                    transportBindingFactory.createTransportBinding(s, null)));
        }

        subscriptionId = wsaUtil.getAddressUri(delegate.getSubscriptionManagerEpr()).orElseThrow(() ->
                new NoSuchElementException("Subscription manager id could not be resolved"));

        instanceLogger.info("Source subscription manager '{}' started. Start delivering notifications to '{}'",
                subscriptionId, notifyToUri);
    }

    @Override
    protected void run() {
        while (isRunning()) {
            try {
                final var queueItem = notificationQueue.take();
                if (queueItem instanceof ShutdownItem) {
                    break;
                }

                if (queueItem instanceof SubscriptionEndItem) {
                    final var subEnd = (SubscriptionEndItem) queueItem;
                    instanceLogger.info("Source subscription manager '{}' received stop signal and is about " +
                            "to shut down", subscriptionId);
                    endToSender.sendNotification(subEnd.getNotification().getPayload());
                    break;
                }

                final var subEnd = (NotificationItem) queueItem;

                instanceLogger.debug("Sending notification to {} - {}", notifyToUri,
                        subEnd.getNotification().getPayload());
                notifyToSender.sendNotification(subEnd.getNotification().getPayload());
                // CHECKSTYLE.OFF: IllegalCatch
            } catch (Exception e) {
                // CHECKSTYLE.ON: IllegalCatch
                instanceLogger.info("Source subscription manager '{}' ended unexpectedly", subscriptionId);
                instanceLogger.trace("Source subscription manager '{}' ended unexpectedly", subscriptionId, e);
                break;
            }
        }
    }

    @Override
    protected void triggerShutdown() {
        notificationQueue.clear();
        notificationQueue.offer(new ShutdownItem());

        instanceLogger.info("Source subscription manager '{}' shut down. Delivery to '{}' stopped.",
                subscriptionId, notifyToUri);
    }

    private Optional<SoapMessage> createEndToMessage(WsEventingStatus status) {
        return getEndTo().map(endTo -> {
            var subscriptionEnd = wseFactory.createSubscriptionEnd();
            subscriptionEnd.setSubscriptionManager(getSubscriptionManagerEpr());
            subscriptionEnd.setStatus(status.getUri());
            var msg = soapUtil.createMessage(WsEventingConstants.WSA_ACTION_SUBSCRIPTION_END, subscriptionEnd);
            var optionalWsaTo = wsaUtil.getAddressUri(endTo);
            optionalWsaTo.ifPresent(wsaTo -> msg.getWsAddressingHeader().setTo(wsaUtil.createAttributedURIType(wsaTo)));
            return msg;
        });
    }

    private interface QueueItem {
    }

    private static class NotificationItem implements QueueItem {
        private final Notification notification;

        NotificationItem(@Nullable Notification notification) {
            this.notification = notification;
        }

        public Notification getNotification() {
            return notification;
        }
    }

    private static class SubscriptionEndItem extends NotificationItem {
        SubscriptionEndItem(Notification notification) {
            super(notification);
        }
    }

    private static class ShutdownItem implements QueueItem {
    }
}