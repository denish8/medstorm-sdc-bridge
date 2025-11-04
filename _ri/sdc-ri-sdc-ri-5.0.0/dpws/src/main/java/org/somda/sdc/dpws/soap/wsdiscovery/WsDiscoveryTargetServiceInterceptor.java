package org.somda.sdc.dpws.soap.wsdiscovery;

import com.google.common.primitives.UnsignedInteger;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.soap.NotificationSource;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.factory.SoapFaultFactory;
import org.somda.sdc.dpws.soap.interception.Direction;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingHeader;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wsdiscovery.factory.WsDiscoveryFaultFactory;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ByeType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.HelloType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ObjectFactory;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ProbeMatchType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ProbeMatchesType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ProbeType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ResolveMatchType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ResolveMatchesType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ResolveType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ScopesType;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Default implementation of the {@linkplain WsDiscoveryTargetService}.
 */
public class WsDiscoveryTargetServiceInterceptor implements WsDiscoveryTargetService {
    private static final Logger LOG = LogManager.getLogger(WsDiscoveryTargetServiceInterceptor.class);

    private final ObjectFactory wsdFactory;
    private final SoapFaultFactory soapFaultFactory;
    private final WsDiscoveryFaultFactory wsdFaultFactory;
    private final SoapUtil soapUtil;
    private final WsAddressingUtil wsaUtil;
    private final WsDiscoveryUtil wsdUtil;
    private final EndpointReferenceType targetServiceEpr;
    private final NotificationSource notificationSource;
    private final UnsignedInteger instanceId;
    private final Logger instanceLogger;
    private List<QName> types;
    private List<String> scopes;
    private List<String> xAddrs;
    private final AtomicBoolean metadataModified;
    private UnsignedInteger metadataVersion;

    private final Lock lock;

    @AssistedInject
    WsDiscoveryTargetServiceInterceptor(@Assisted EndpointReferenceType targetServiceEpr,
                                        @Assisted NotificationSource notificationSource,
                                        ObjectFactory wsdFactory,
                                        SoapFaultFactory soapFaultFactory,
                                        SoapUtil soapUtil,
                                        WsDiscoveryFaultFactory wsdFaultFactory,
                                        WsAddressingUtil wsaUtil,
                                        WsDiscoveryUtil wsdUtil,
                                        @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.targetServiceEpr = targetServiceEpr;
        this.notificationSource = notificationSource;
        this.instanceId = UnsignedInteger.valueOf(System.currentTimeMillis() / 1000L);
        this.wsdFactory = wsdFactory;
        this.soapFaultFactory = soapFaultFactory;
        this.soapUtil = soapUtil;
        this.wsdFaultFactory = wsdFaultFactory;
        this.wsaUtil = wsaUtil;
        this.wsdUtil = wsdUtil;
        lock = new ReentrantLock();
        metadataModified = new AtomicBoolean(false);
        metadataVersion = getNewMetadataVersion(null);

        types = new ArrayList<>();
        scopes = new ArrayList<>();
        xAddrs = new ArrayList<>();
    }

    @MessageInterceptor(value = WsDiscoveryConstants.WSA_ACTION_PROBE, direction = Direction.REQUEST)
    void processProbe(RequestResponseObject rrObj)
            throws SoapFaultException {
        SoapMessage inMsg = rrObj.getRequest();
        ProbeType probe = validateIncomingMessage(inMsg, ProbeType.class);
        ScopesType scopesFromProbe = Optional.ofNullable(probe.getScopes()).orElse(wsdFactory.createScopesType());
        List<QName> typesFromProbe = Optional.ofNullable(probe.getTypes()).orElse(new ArrayList<>());

        String requestMatchBy = Optional.ofNullable(scopesFromProbe.getMatchBy()).orElse(MatchBy.RFC3986.getUri());
        MatchBy matcher = Arrays.stream(MatchBy.values())
                .filter(item -> item.getUri().equals(requestMatchBy))
                .findAny().orElseThrow(() -> new SoapFaultException(wsdFaultFactory.createMatchingRuleNotSupported(),
                        rrObj.getRequest().getWsAddressingHeader().getMessageId().orElse(null)));

        EndpointReferenceType endpointReference = getEndpointReference();
        List<String> copyScopes = getScopes();
        List<QName> copyTypes = getTypes();
        List<String> copyXAddrs = getXAddrs();
        UnsignedInteger copyMetadataVersion = getMetadataVersion();

        if (!wsdUtil.isScopesMatching(copyScopes, scopesFromProbe.getValue(), matcher) ||
                !wsdUtil.isTypesMatching(this.types, typesFromProbe)) {
            throw new RuntimeException("Scopes and Types do not match in incoming Probe.");
        }

        ProbeMatchType probeMatchType = wsdFactory.createProbeMatchType();
        probeMatchType.setEndpointReference(endpointReference);
        ScopesType scopesType = wsdFactory.createScopesType();
        scopesType.setValue(copyScopes);
        probeMatchType.setScopes(scopesType);
        probeMatchType.setTypes(copyTypes);
        probeMatchType.setXAddrs(copyXAddrs);
        probeMatchType.setMetadataVersion(copyMetadataVersion.longValue());

        ProbeMatchesType probeMatchesType = wsdFactory.createProbeMatchesType();
        List<ProbeMatchType> probeMatchTypeList = new ArrayList<>();
        probeMatchTypeList.add(probeMatchType);
        probeMatchesType.setProbeMatch(probeMatchTypeList);
        SoapMessage probeMatchesMsg = rrObj.getResponse();
        WsAddressingHeader wsaHeader = probeMatchesMsg.getWsAddressingHeader();
        wsaHeader.setAction(wsaUtil.createAttributedURIType(WsDiscoveryConstants.WSA_ACTION_PROBE_MATCHES));
        wsaHeader.setTo(wsaUtil.createAttributedURIType(WsDiscoveryConstants.WSA_UDP_TO));
        probeMatchesMsg.getWsDiscoveryHeader().setAppSequence(wsdUtil.createAppSequence(instanceId));
        soapUtil.setBody(wsdFactory.createProbeMatches(probeMatchesType), probeMatchesMsg);
    }

    @MessageInterceptor(value = WsDiscoveryConstants.WSA_ACTION_RESOLVE, direction = Direction.REQUEST)
    void processResolve(RequestResponseObject rrObj)
            throws SoapFaultException {
        SoapMessage inMsg = rrObj.getRequest();
        ResolveType resolveType = validateIncomingMessage(inMsg, ResolveType.class);

        Optional.ofNullable(resolveType.getEndpointReference()).orElseThrow(() ->
                new SoapFaultException(soapFaultFactory.createSenderFault("Missing Endpoint Reference"),
                        inMsg.getWsAddressingHeader().getMessageId().orElse(null)));

        Optional.ofNullable(resolveType.getEndpointReference().getAddress()).orElseThrow(() ->
                new SoapFaultException(soapFaultFactory.createSenderFault("Missing Endpoint Reference Address"),
                        inMsg.getWsAddressingHeader().getMessageId().orElse(null)));

        EndpointReferenceType endpointReference = getEndpointReference();
        List<String> copyScopes = getScopes();
        List<QName> copyTypes = getTypes();
        List<String> copyXAddrs = getXAddrs();
        UnsignedInteger copyMetadataVersion = getMetadataVersion();

        if (!URI.create(resolveType.getEndpointReference().getAddress().getValue())
                .equals(URI.create(endpointReference.getAddress().getValue()))) {
            instanceLogger.debug("Incoming ResolveMatches message had an EPR address not matching this device." +
                            " Message EPR address is {}, device EPR address is {}",
                    resolveType.getEndpointReference().getAddress().getValue(),
                    endpointReference.getAddress().getValue());
            return;
        }

        ResolveMatchType resolveMatchType = wsdFactory.createResolveMatchType();
        resolveMatchType.setEndpointReference(endpointReference);
        ScopesType scopesType = wsdFactory.createScopesType();
        scopesType.setValue(copyScopes);
        resolveMatchType.setScopes(scopesType);
        resolveMatchType.setTypes(copyTypes);
        resolveMatchType.setXAddrs(copyXAddrs);
        resolveMatchType.setMetadataVersion(copyMetadataVersion.longValue());

        ResolveMatchesType resolveMatchesType = wsdFactory.createResolveMatchesType();
        resolveMatchesType.setResolveMatch(resolveMatchType);

        SoapMessage resolveMatchesMsg = rrObj.getResponse();
        WsAddressingHeader wsaHeader = resolveMatchesMsg.getWsAddressingHeader();
        wsaHeader.setAction(wsaUtil.createAttributedURIType(WsDiscoveryConstants.WSA_ACTION_RESOLVE_MATCHES));
        wsaHeader.setTo(wsaUtil.createAttributedURIType(WsDiscoveryConstants.WSA_UDP_TO));
        resolveMatchesMsg.getWsDiscoveryHeader().setAppSequence(wsdUtil.createAppSequence(instanceId));

        soapUtil.setBody(wsdFactory.createResolveMatches(resolveMatchesType), resolveMatchesMsg);
    }

    public EndpointReferenceType getEndpointReference() {
        return targetServiceEpr;
    }

    @Override
    public void setTypes(List<QName> qNames) {
        try {
            lock.lock();
            types = cloneQNames(qNames);
            metadataModified.set(true);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<QName> getTypes() {
        try {
            lock.lock();
            return cloneQNames(types);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setScopes(List<String> uris) {
        try {
            lock.lock();
            scopes = new ArrayList<>(uris);
            metadataModified.set(true);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<String> getScopes() {
        try {
            lock.lock();
            return new ArrayList<>(scopes);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setXAddrs(List<String> xAddrs) {
        try {
            lock.lock();
            this.xAddrs = new ArrayList<>(xAddrs);
            metadataModified.set(true);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<String> getXAddrs() {
        try {
            lock.lock();
            return new ArrayList<>(xAddrs);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setMetadataModified() {
        metadataModified.set(true);
    }

    @Override
    public UnsignedInteger getMetadataVersion() {
        try {
            lock.lock();
            return UnsignedInteger.valueOf(metadataVersion.longValue());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public UnsignedInteger sendHello() throws MarshallingException, TransportException, InterceptorException {
        return sendHello(false);
    }

    @Override
    public UnsignedInteger sendHello(boolean forceNewMetadataVersion)
            throws MarshallingException, TransportException, InterceptorException {
        UnsignedInteger currentMetadataVersion;
        if (forceNewMetadataVersion) {
            currentMetadataVersion = incMetadataVersionAndGet();
        } else {
            currentMetadataVersion = incMetadataVersionIfModifiedAndGet();
        }

        HelloType helloType = wsdFactory.createHelloType();
        helloType.setXAddrs(getXAddrs());
        ScopesType scopesType = wsdFactory.createScopesType();
        scopesType.setValue(getScopes());
        helloType.setScopes(scopesType);
        helloType.setTypes(getTypes());
        helloType.setMetadataVersion(currentMetadataVersion.longValue());
        helloType.setEndpointReference(getEndpointReference());

        sendMulticast(WsDiscoveryConstants.WSA_ACTION_HELLO, wsdFactory.createHello(helloType));
        metadataModified.set(false);

        return currentMetadataVersion;
    }

    @Override
    public void sendBye() throws MarshallingException, TransportException, InterceptorException {
        ByeType byeType = wsdFactory.createByeType();
        byeType.setXAddrs(getXAddrs());
        ScopesType scopesType = wsdFactory.createScopesType();
        scopesType.setValue(getScopes());
        byeType.setScopes(scopesType);
        byeType.setTypes(getTypes());
        byeType.setEndpointReference(getEndpointReference());

        sendMulticast(WsDiscoveryConstants.WSA_ACTION_BYE, wsdFactory.createBye(byeType));
    }

    /**
     * Increments the metadata version and retrieves the new value.
     *
     * @return new metadata version
     */
    public UnsignedInteger incMetadataVersionAndGet() {
        try {
            lock.lock();
            metadataVersion = getNewMetadataVersion(metadataVersion);
            return UnsignedInteger.valueOf(metadataVersion.longValue());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Increments the metadata version and retrieve the new value if the metadata has changed,
     * returns previous version otherwise.
     *
     * @return metadata version
     */
    public UnsignedInteger incMetadataVersionIfModifiedAndGet() {
        try {
            lock.lock();
            if (metadataModified.get()) {
                metadataVersion = getNewMetadataVersion(metadataVersion);
            }
            return UnsignedInteger.valueOf(metadataVersion.longValue());
        } finally {
            lock.unlock();
        }
    }

    private UnsignedInteger getNewMetadataVersion(@Nullable UnsignedInteger currentVersion) {
        // Metadata version is calculated from timestamp in seconds
        var newVersion = UnsignedInteger.valueOf(Instant.now().toEpochMilli() / 1000L);
        if (currentVersion == null) {
            return newVersion;
        }

        // If there is more than one metadata version increment within one second just increment the current version.
        // This approach does not scale if there are tons of changes to the metadata version in a short time.
        // For this unlikely scenario an implementation is required that can persist metadata versions
        if (newVersion.compareTo(currentVersion) <= 0) {
            return currentVersion.plus(UnsignedInteger.ONE);
        }

        return newVersion;
    }

    private void sendMulticast(String action, JAXBElement<?> body)
            throws MarshallingException, TransportException, InterceptorException {
        SoapMessage soapMessage = soapUtil.createMessage(action, WsDiscoveryConstants.WSA_UDP_TO, body);
        soapMessage.getWsDiscoveryHeader().setAppSequence(wsdUtil.createAppSequence(instanceId));
        notificationSource.sendNotification(soapMessage);
    }

    private <T> T validateIncomingMessage(SoapMessage request, Class<T> bodyType) throws SoapFaultException {
        String to = request.getWsAddressingHeader().getTo().orElseThrow(() ->
                new SoapFaultException(soapFaultFactory.createSenderFault(
                        String.format("wsa:To field is invalid. Expected '%s', but none found ",
                                WsDiscoveryConstants.WSA_UDP_TO)),
                        request.getWsAddressingHeader().getMessageId().orElse(null))).getValue();

        if (!to.equals(WsDiscoveryConstants.WSA_UDP_TO)) {
            throw new SoapFaultException(soapFaultFactory.createSenderFault(
                    String.format("wsa:To field is invalid. Expected '%s', but actual is '%s'",
                            WsDiscoveryConstants.WSA_UDP_TO, to)),
                    request.getWsAddressingHeader().getMessageId().orElse(null));
        }

        return soapUtil.getBody(request, bodyType).orElseThrow(() ->
                new SoapFaultException(soapFaultFactory.createSenderFault("Body type is invalid"),
                        request.getWsAddressingHeader().getMessageId().orElse(null)));
    }

    private List<QName> cloneQNames(List<QName> qNames) {
        return qNames.stream()
                .map(qName -> new QName(qName.getNamespaceURI(), qName.getLocalPart(), qName.getPrefix()))
                .collect(Collectors.toList());
    }
}
