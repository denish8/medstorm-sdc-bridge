package org.somda.sdc.dpws.soap.wseventing.helper;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import org.somda.sdc.dpws.device.helper.RequestResponseServerHttpHandler;
import org.somda.sdc.dpws.http.HttpServerRegistry;
import org.somda.sdc.dpws.http.HttpUriBuilder;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wseventing.EventSourceInterceptorDispatcher;
import org.somda.sdc.dpws.soap.wseventing.SourceSubscriptionManager;
import org.somda.sdc.dpws.soap.wseventing.WsEventingConfig;
import org.somda.sdc.dpws.soap.wseventing.factory.WsEventingFaultFactory;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBElement;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Utility class for the {@linkplain EventSourceInterceptorDispatcher} and {@linkplain SubscriptionRegistry}.
 */
public class EventSourceUtil {
    private final HttpServerRegistry httpServerRegistry;
    private final HttpUriBuilder httpUriBuilder;
    private final WsAddressingUtil wsaUtil;
    private final WsEventingFaultFactory faultFactory;
    private final SoapUtil soapUtil;
    private final String subscriptionManagerPath;
    private final Provider<RequestResponseServerHttpHandler> rrServerHttpHandlerProvider;
    private final Duration maxExpires;

    @Inject
    EventSourceUtil(HttpServerRegistry httpServerRegistry,
                    HttpUriBuilder httpUriBuilder,
                    WsAddressingUtil wsaUtil,
                    WsEventingFaultFactory faultFactory,
                    SoapUtil soapUtil,
                    @Named(WsEventingConfig.SOURCE_MAX_EXPIRES) Duration maxExpires,
                    @Named(WsEventingConfig.SOURCE_SUBSCRIPTION_MANAGER_PATH) String subscriptionManagerPath,
                    Provider<RequestResponseServerHttpHandler> rrServerHttpHandlerProvider) {
        this.httpServerRegistry = httpServerRegistry;
        this.httpUriBuilder = httpUriBuilder;
        this.wsaUtil = wsaUtil;
        this.faultFactory = faultFactory;
        this.soapUtil = soapUtil;
        this.subscriptionManagerPath = subscriptionManagerPath;
        this.rrServerHttpHandlerProvider = rrServerHttpHandlerProvider;
        this.maxExpires = maxExpires;
    }

    /**
     * Creates a new subscription manager EPR based on the given transport information.
     *
     * @param scheme      the scheme (HTTP/HTTPS) to use.
     * @param address     the local address to use for the setup of the manager.
     * @param port        the local port to use for the setup of the manager.
     * @param interceptor the interceptor that handles incoming HTTP requests.
     * @return the EPR to be used in SubscribeResponse messages.
     */
    public EndpointReferenceType createSubscriptionManagerEprAndRegisterHttpHandler(String scheme,
                                                                                    String address,
                                                                                    Integer port,
                                                                                    Interceptor interceptor) {
        var hostPart = httpUriBuilder.buildUri(scheme, address, port);
        String contextPath = "/" + UUID.randomUUID() + "/" + subscriptionManagerPath;
        String eprAddress = hostPart + contextPath;

        RequestResponseServerHttpHandler handler = rrServerHttpHandlerProvider.get();
        handler.register(interceptor);
        httpServerRegistry.registerContext(hostPart, true, contextPath, null, null, handler);

        return wsaUtil.createEprWithAddress(eprAddress);
    }

    /**
     * Validates a request message against a specific type and returns.
     *
     * @param rrObj        the request response object to be inspected.
     * @param expectedType the expected type to check against.
     * @param <T>          type information for the expected type
     * @return the instance found in the request response object, cast to the given type.
     * @throws SoapFaultException if validation fails.
     */
    public <T> T validateRequestBody(RequestResponseObject rrObj, Class<T> expectedType) throws SoapFaultException {
        final var any = rrObj.getRequest().getOriginalEnvelope().getBody().getAny();
        if (any.isEmpty()) {
            throw new SoapFaultException(
                    createInvalidMsg(rrObj, "The request body was empty"),
                    rrObj.getRequest().getWsAddressingHeader().getMessageId().orElse(null)
            );
        }
        return soapUtil.getBody(rrObj.getRequest(), expectedType).orElseThrow(() ->
                new SoapFaultException(
                        createInvalidMsg(
                                rrObj,
                                String.format(
                                        "Expected a request body element of type %s, found %s",
                                        expectedType.getName(),
                                        any.get(0).getClass().getName()
                                )
                        ),
                        rrObj.getRequest().getWsAddressingHeader().getMessageId().orElse(null)
                )
        );
    }

    /**
     * Creates a SOAP fault message with a reason text.
     *
     * @param rrObj  the request response object to be used for the fault message.
     * @param reason the reason text.
     * @return a SOAP message representing a fault message.
     */
    public SoapMessage createInvalidMsg(RequestResponseObject rrObj, String reason) {
        return faultFactory.createInvalidMessage(reason, rrObj.getRequest().getOriginalEnvelope());
    }

    /**
     * Creates a notification message.
     *
     * @param wsaAction the action URI used for the notification.
     * @param payload   the payload to send as a {@link JAXBElement} or an object that can be marshalled.
     * @param subMan    the subscription manager used for reference parameter retrieval.
     * @return a SOAP message representing the notification.
     */
    public SoapMessage createForNotifyTo(String wsaAction, Object payload, SourceSubscriptionManager subMan) {
        final var notifyTo = subMan.getNotifyTo();
        String wsaTo = wsaUtil.getAddressUri(notifyTo).orElseThrow(() ->
                new RuntimeException("Could not resolve URI from NotifyTo"));
        final var referenceParameters = notifyTo.getReferenceParameters();
        return soapUtil.createMessage(wsaAction, wsaTo, payload, referenceParameters);
    }

    /**
     * Grants an expiration duration for a subscription.
     *
     * @param expires the requested expiration duration.
     * @return the granted expiration, which is at most {@link WsEventingConfig#SOURCE_MAX_EXPIRES}.
     * @throws SoapFaultException if the duration is zero or negative.
     */
    public Duration grantExpires(@Nullable Duration expires) throws SoapFaultException {
        final var validatedExpires = validateExpires(expires);
        if (validatedExpires != null && maxExpires.compareTo(expires) >= 0) {
            return validatedExpires;
        } else {
            return maxExpires;
        }
    }

    private @Nullable Duration validateExpires(@Nullable Duration requestedExpires) throws SoapFaultException {
        if (requestedExpires == null) {
            return null;
        }
        if (requestedExpires.isZero() || requestedExpires.isNegative()) {
            throw new SoapFaultException(faultFactory.createInvalidExpirationTime());
        }

        return requestedExpires;
    }

    /**
     * Inspects a request message to contain a valid subscription manager.
     *
     * @param rrObj         the request response object to inspect.
     * @param subscriptions available subscriptions.
     * @return returns the subscription manager of a valid subscription identifier in rrObj.
     * @throws SoapFaultException if the subscription is not valid (non-existing, expired, ...).
     */
    public SourceSubscriptionManager validateSubscriptionEpr(
            RequestResponseObject rrObj,
            Map<String, SourceSubscriptionManager> subscriptions
    ) throws SoapFaultException {
        final var toUri = rrObj.getRequest().getWsAddressingHeader().getTo().orElseThrow(() ->
                new SoapFaultException(
                        createInvalidMsg(
                                rrObj,
                                "No wsa:To element found in request message"
                        ),
                        rrObj.getRequest().getWsAddressingHeader().getMessageId().orElse(null)
                )
        );

        return Optional.ofNullable(subscriptions.get(toUri.getValue())).orElseThrow(() ->
                new SoapFaultException(createInvalidMsg(rrObj,
                        String.format("Subscription manager '%s' does not exist.", toUri.getValue())),
                        rrObj.getRequest().getWsAddressingHeader().getMessageId().orElse(null)));
    }


    /**
     * Helper function that unregisters a subscription manager from an HTTP handler.
     *
     * @param subMan the subscription manager for which to end HTTP request handling.
     */
    public void unregisterHttpHandler(SourceSubscriptionManager subMan) {
        final var fullUri = URI.create(subMan.getSubscriptionManagerEpr().getAddress().getValue());
        final var uriWithoutPath = httpUriBuilder.buildUri(fullUri.getScheme(), fullUri.getHost(), fullUri.getPort());
        httpServerRegistry.unregisterContext(uriWithoutPath, fullUri.getPath());
    }
}
