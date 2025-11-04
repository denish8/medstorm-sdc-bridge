package org.somda.sdc.dpws.soap.wseventing;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.JaxbUtil;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.interception.Direction;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wseventing.factory.SubscriptionManagerFactory;
import org.somda.sdc.dpws.soap.wseventing.factory.SubscriptionRegistryFactory;
import org.somda.sdc.dpws.soap.wseventing.factory.WsEventingFaultFactory;
import org.somda.sdc.dpws.soap.wseventing.helper.EventSourceUtil;
import org.somda.sdc.dpws.soap.wseventing.helper.SubscriptionRegistry;
import org.somda.sdc.dpws.soap.wseventing.model.GetStatus;
import org.somda.sdc.dpws.soap.wseventing.model.ObjectFactory;
import org.somda.sdc.dpws.soap.wseventing.model.Renew;
import org.somda.sdc.dpws.soap.wseventing.model.Subscribe;
import org.somda.sdc.dpws.soap.wseventing.model.Unsubscribe;
import org.somda.sdc.dpws.soap.wseventing.model.WsEventingStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Interceptor that manages subscriptions and dispatches subscription activity to filter dialect handlers.
 */
public class EventSourceInterceptorDispatcher extends AbstractIdleService implements Interceptor {
    private static final Logger LOG = LogManager.getLogger(EventSourceInterceptorDispatcher.class);

    private final SoapUtil soapUtil;
    private final WsEventingFaultFactory faultFactory;
    private final Map<String, SubscriptionRegistry> subscriptionRegistries;
    private final SubscriptionManagerFactory subscriptionManagerFactory;
    private final Map<String, EventSourceDialectHandler> eventSourceDialectHandlers;
    private final EventSourceUtil eventSourceUtil;
    private final JaxbUtil jaxbUtil;
    private final WsAddressingUtil wsaUtil;
    private final ObjectFactory wseFactory;
    private final Logger instanceLogger;

    @AssistedInject
    EventSourceInterceptorDispatcher(
            @Assisted Collection<EventSourceDialectHandler> eventSources,
            SoapUtil soapUtil,
            WsEventingFaultFactory faultFactory,
            EventSourceUtil eventSourceUtil,
            JaxbUtil jaxbUtil,
            WsAddressingUtil wsaUtil,
            ObjectFactory wseFactory,
            SubscriptionRegistryFactory subscriptionRegistryProvider,
            SubscriptionManagerFactory subscriptionManagerFactory,
            @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.eventSourceDialectHandlers = eventSources.stream()
                .collect(Collectors.toMap(EventSourceDialectHandler::getDialect, Function.identity()));
        this.eventSourceUtil = eventSourceUtil;
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.soapUtil = soapUtil;
        this.faultFactory = faultFactory;
        this.subscriptionRegistries = eventSources.stream()
                .collect(Collectors.toMap(EventSourceDialectHandler::getDialect, subscriptionRegistryProvider::create));
        this.subscriptionManagerFactory = subscriptionManagerFactory;
        this.jaxbUtil = jaxbUtil;
        this.wsaUtil = wsaUtil;
        this.wseFactory = wseFactory;
    }

    @MessageInterceptor(value = WsEventingConstants.WSA_ACTION_SUBSCRIBE, direction = Direction.REQUEST)
    void processSubscribe(RequestResponseObject rrObj) throws SoapFaultException {
        final var requestMsgId =
                rrObj.getRequest().getWsAddressingHeader().getMessageId().orElse(null);
        final Supplier<SoapFaultException> soapFaultExceptionSupplier = () ->
                new SoapFaultException(eventSourceUtil.createInvalidMsg(
                        rrObj,
                        String.format("Subscribe request %s was not valid", requestMsgId)
                ));
        final var subscribe = soapUtil.getBody(rrObj.getRequest(), Subscribe.class)
                .orElseThrow(soapFaultExceptionSupplier);

        // Validate delivery mode
        String deliveryMode = Optional.ofNullable(subscribe.getDelivery().getMode())
                .orElse(WsEventingConstants.SUPPORTED_DELIVERY_MODE);
        if (!deliveryMode.equals(WsEventingConstants.SUPPORTED_DELIVERY_MODE)) {
            throw new SoapFaultException(faultFactory.createDeliveryModeRequestedUnavailable(), requestMsgId);
        }

        // Validate delivery endpoint reference
        final var deliveryEndpointCount = subscribe.getDelivery().getContent().size();
        if (deliveryEndpointCount != 1) {
            throw new SoapFaultException(
                    eventSourceUtil.createInvalidMsg(
                            rrObj,
                            String.format("Expected exactly one delivery endpoint, found %s", deliveryEndpointCount)
                    ),
                    requestMsgId);
        }

        final var notifyTo = jaxbUtil.extractElement(subscribe.getDelivery().getContent().get(0),
                WsEventingConstants.NOTIFY_TO, EndpointReferenceType.class).orElseThrow(soapFaultExceptionSupplier);

        wsaUtil.getAddressUri(notifyTo).orElseThrow(soapFaultExceptionSupplier);

        // Validate expires
        final var grantedExpires = eventSourceUtil.grantExpires(subscribe.getExpires());

        // Create subscription
        final var transportInfo = rrObj.getCommunicationContext().getTransportInfo();
        final var epr = eventSourceUtil.createSubscriptionManagerEprAndRegisterHttpHandler(
                transportInfo.getScheme(),
                transportInfo.getLocalAddress().orElseThrow(() ->
                        new RuntimeException("Fatal error. Missing local address in transport information.")),
                transportInfo.getLocalPort().orElseThrow(() ->
                        new RuntimeException("Fatal error. Missing local port in transport information.")),
                this
        );

        // Validate filter type
        final var filterType = Optional.ofNullable(subscribe.getFilter()).orElseThrow(() ->
                new SoapFaultException(faultFactory.createEventSourceUnableToProcess("No filter given, " +
                        "but required."), requestMsgId));

        // Validate filter dialect
        final var filterDialect = Optional.ofNullable(filterType.getDialect())
                .orElse("http://www.w3.org/TR/1999/REC-xpath-19991116");

        final var filterDialectHandler = eventSourceDialectHandlers.get(filterDialect);
        if (filterDialectHandler == null) {
            throw new SoapFaultException(faultFactory.createFilteringRequestedUnavailable(), requestMsgId);
        }

        // Setup subscription manager
        final var subMan = subscriptionManagerFactory.createSourceSubscriptionManager(
                epr,
                grantedExpires,
                notifyTo,
                subscribe.getEndTo(),
                epr.getAddress().getValue(),
                Collections.unmodifiableList(filterType.getContent()),
                filterDialect,
                soapUtil.determineRequestDistinguishedName(rrObj)
        );

        filterDialectHandler.subscribe(subMan);

        subMan.startAsync().awaitRunning();

        subscriptionRegistries.get(filterDialect).addSubscription(subMan);

        // Build response body and populate response envelope
        final var subscribeResponse = wseFactory.createSubscribeResponse();
        subscribeResponse.setExpires(grantedExpires);

        subscribeResponse.setSubscriptionManager(subMan.getSubscriptionManagerEpr());
        soapUtil.setBody(subscribeResponse, rrObj.getResponse());
        soapUtil.setWsaAction(rrObj.getResponse(), WsEventingConstants.WSA_ACTION_SUBSCRIBE_RESPONSE);

        instanceLogger.info("Incoming subscribe request. Generated subscription id: {}. " +
                        "Notifications go to {}. Expiration in {} seconds",
                subMan.getSubscriptionId(),
                wsaUtil.getAddressUri(subMan.getNotifyTo()).orElse("<unknown>"),
                grantedExpires.getSeconds());
    }

    @MessageInterceptor(value = WsEventingConstants.WSA_ACTION_RENEW, direction = Direction.REQUEST)
    void processRenew(RequestResponseObject rrObj) throws SoapFaultException {
        final var renew = eventSourceUtil.validateRequestBody(rrObj, Renew.class);
        final var grantedExpires = eventSourceUtil.grantExpires(renew.getExpires());

        final var subMan = eventSourceUtil.validateSubscriptionEpr(rrObj, getAllSubscriptions());
        subMan.renew(grantedExpires);

        final var renewResponse = wseFactory.createRenewResponse();
        renewResponse.setExpires(grantedExpires);
        soapUtil.setBody(renewResponse, rrObj.getResponse());
        soapUtil.setWsaAction(rrObj.getResponse(), WsEventingConstants.WSA_ACTION_RENEW_RESPONSE);

        instanceLogger.info("Subscription {} is renewed. New expiration in {} seconds",
                subMan.getSubscriptionId(),
                grantedExpires.getSeconds());
    }

    @MessageInterceptor(value = WsEventingConstants.WSA_ACTION_GET_STATUS, direction = Direction.REQUEST)
    void processGetStatus(RequestResponseObject rrObj) throws SoapFaultException {
        eventSourceUtil.validateRequestBody(rrObj, GetStatus.class);

        final var subMan = eventSourceUtil.validateSubscriptionEpr(rrObj, getAllSubscriptions());
        final var expires = Duration.between(Instant.now(), subMan.getExpiresTimeout());
        if (expires.isNegative() || expires.isZero()) {
            throw new SoapFaultException(eventSourceUtil.createInvalidMsg(rrObj,
                    String.format("Subscription %s expired", subMan.getSubscriptionId())),
                    rrObj.getRequest().getWsAddressingHeader().getMessageId().orElse(null));
        }

        final var getStatusResponse = wseFactory.createGetStatusResponse();
        getStatusResponse.setExpires(expires);
        soapUtil.setBody(getStatusResponse, rrObj.getResponse());
        soapUtil.setWsaAction(rrObj.getResponse(), WsEventingConstants.WSA_ACTION_GET_STATUS_RESPONSE);
    }

    @MessageInterceptor(value = WsEventingConstants.WSA_ACTION_UNSUBSCRIBE, direction = Direction.REQUEST)
    void processUnsubscribe(RequestResponseObject rrObj) throws SoapFaultException {
        eventSourceUtil.validateRequestBody(rrObj, Unsubscribe.class);

        final var subMan = eventSourceUtil.validateSubscriptionEpr(rrObj, getAllSubscriptions());
        subMan.stopAsync().awaitTerminated();

        eventSourceDialectHandlers.values().forEach(handler -> handler.unsubscribe(subMan));

        // No response body required
        soapUtil.setWsaAction(rrObj.getResponse(), WsEventingConstants.WSA_ACTION_UNSUBSCRIBE_RESPONSE);

        instanceLogger.info("Unsubscribe {}. Invalidate subscription manager", subMan.getSubscriptionId());
    }

    public Map<String, SubscriptionManager> getActiveSubscriptions() {
        return getAllSubscriptions().values().stream()
                .map(it -> (SubscriptionManager) it)
                .collect(Collectors.toMap(SubscriptionManager::getSubscriptionId, Function.identity()));
    }

    @Override
    protected void startUp() {
        eventSourceDialectHandlers.values().forEach(it -> it.init(subscriptionRegistries.get(it.getDialect())));
    }

    @Override
    protected void shutDown() {
        getAllSubscriptions().values().forEach(subMan -> {
            subMan.offerEndTo(WsEventingStatus.STATUS_SOURCE_SHUTTING_DOWN);
        });
    }

    private Map<String, SourceSubscriptionManager> getAllSubscriptions() {
        return subscriptionRegistries.values().stream()
                .flatMap(it -> it.getSubscriptions().entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
