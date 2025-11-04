package org.somda.sdc.dpws.soap.wsaddressing;

import com.google.common.collect.EvictingQueue;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.factory.SoapFaultFactory;
import org.somda.sdc.dpws.soap.interception.Direction;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.NotificationObject;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.dpws.soap.wsaddressing.factory.WsAddressingFaultFactory;
import org.somda.sdc.dpws.soap.wsaddressing.model.AttributedURIType;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Implements a WS-Addressing server interceptor to check WS-Addressing header information.
 * <p>
 * The {@linkplain WsAddressingServerInterceptor} is in charge of
 * <ul>
 * <li>checking for existing wsa:Action attribute (and cancelling an incoming request in case it is missing) and
 * <li>tracking message ids and reject duplicates.
 * </ul>
 * todo DGr process ReplyTo automatically
 */
public class WsAddressingServerInterceptor implements Interceptor {
    private static final Logger LOG = LogManager.getLogger(WsAddressingServerInterceptor.class);

    private final Boolean ignoreMessageIds;
    private final WsAddressingFaultFactory addressingFaultFactory;
    private final WsAddressingUtil wsaUtil;
    private final SoapUtil soapUtil;
    private final EvictingQueue<String> messageIdCache;
    private final SoapFaultFactory soapFaultFactory;
    private final Logger instanceLogger;

    @Inject
    WsAddressingServerInterceptor(@Named(WsAddressingConfig.MESSAGE_ID_CACHE_SIZE) Integer messageIdCacheSize,
                                  @Named(WsAddressingConfig.IGNORE_MESSAGE_IDS) Boolean ignoreMessageIds,
                                  WsAddressingFaultFactory addressingFaultFactory,
                                  SoapFaultFactory soapFaultFactory,
                                  WsAddressingUtil wsaUtil,
                                  SoapUtil soapUtil,
                                  @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.messageIdCache = EvictingQueue.create(messageIdCacheSize);
        this.ignoreMessageIds = ignoreMessageIds;
        this.addressingFaultFactory = addressingFaultFactory;
        this.soapFaultFactory = soapFaultFactory;
        this.wsaUtil = wsaUtil;
        this.soapUtil = soapUtil;
    }

    @MessageInterceptor(direction = Direction.REQUEST)
    void processMessage(RequestResponseObject rrInfo) throws SoapFaultException {
        var logMissingMessageId = resolveLogCallForMissingMessageIds(rrInfo.getCommunicationContext());
        processMessage(rrInfo.getRequest(), logMissingMessageId);
        rrInfo.getResponse().getWsAddressingHeader().setRelatesTo(wsaUtil.createRelatesToType(
                rrInfo.getRequest().getWsAddressingHeader().getMessageId().orElse(null)));
        rrInfo.getResponse().getWsAddressingHeader().setMessageId(wsaUtil.createAttributedURIType(
                soapUtil.createRandomUuidUri()));
    }

    @MessageInterceptor
    void processMessage(NotificationObject nInfo) throws SoapFaultException {
        var logMissingMessageId = resolveLogCallForMissingMessageIds(nInfo.getCommunicationContext().orElse(null));
        processMessage(nInfo.getNotification(), logMissingMessageId);
    }

    private void processMessage(SoapMessage msg, Consumer<SoapMessage> logMissingMessageId) throws SoapFaultException {
        processAction(msg);
        processMessageId(msg, logMissingMessageId);
    }

    private void processAction(SoapMessage msg) throws SoapFaultException {
        Optional<AttributedURIType> action = msg.getWsAddressingHeader().getAction();

        if (action.isEmpty() || Optional.ofNullable(action.get().getValue()).isEmpty()) {
            throw new SoapFaultException(soapFaultFactory.createSenderFault(
                    "WS-Addressing header 'Action' required, but not given"),
                    msg.getWsAddressingHeader().getMessageId().orElse(null));
        }

        if (action.get().getValue().isEmpty()) {
            throw new SoapFaultException(soapFaultFactory.createSenderFault(
                    "WS-Addressing header 'Action' given, but empty"),
                    msg.getWsAddressingHeader().getMessageId().orElse(null));
        }
    }

    // note the synchronized keyword as the server interceptor is shared between different requests in order to
    // facilitate duplicate detection
    private synchronized void processMessageId(SoapMessage msg, Consumer<SoapMessage> logMissingMessageId) {
        if (ignoreMessageIds) {
            return;
        }

        Optional<AttributedURIType> messageId = msg.getWsAddressingHeader().getMessageId();
        if (messageId.isEmpty()) {
            logMissingMessageId.accept(msg);
            return;
        }

        Optional<String> foundMessageId = messageIdCache.stream()
                .filter(s -> messageId.get().getValue().equals(s))
                .findFirst();
        if (foundMessageId.isPresent()) {
            String actionUri = "unknown action";
            if (msg.getWsAddressingHeader().getAction().isPresent()) {
                actionUri = msg.getWsAddressingHeader().getAction().get().getValue();
            }

            String faultMsg = String.format("Found message duplicate: %s (message: %s). Skip processing.",
                    foundMessageId.get(), actionUri);
            instanceLogger.debug(faultMsg);
            throw new RuntimeException(faultMsg);
        }
        messageIdCache.add(messageId.get().getValue());
    }

    private Consumer<SoapMessage> resolveLogCallForMissingMessageIds(CommunicationContext communicationContext) {
        var logMsg = "Incoming message {} had no MessageID element in its header";

        // Typically missing message IDs are ok as long as the enclosing SOAP messages are conveyed using a
        // connection-agnostic protocol (e.g. TCP)
        Consumer<SoapMessage> logCall = soapMessage -> instanceLogger.debug(logMsg, soapMessage);
        if (communicationContext.getTransportInfo().getScheme()
                .equalsIgnoreCase(DpwsConstants.URI_SCHEME_SOAP_OVER_UDP)) {
            // In DPWS only UDP SOAP messages are required to enclose message IDs
            // - promote missing message IDs to warn here
            logCall = soapMessage -> instanceLogger.warn(logMsg, soapMessage);
        }

        return logCall;
    }
}
