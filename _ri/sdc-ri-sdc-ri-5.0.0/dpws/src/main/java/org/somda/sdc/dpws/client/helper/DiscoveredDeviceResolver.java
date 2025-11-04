package org.somda.sdc.dpws.client.helper;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.client.ClientConfig;
import org.somda.sdc.dpws.client.DiscoveredDevice;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryClient;
import org.somda.sdc.dpws.soap.wsdiscovery.event.HelloMessage;
import org.somda.sdc.dpws.soap.wsdiscovery.event.ProbeMatchesMessage;
import org.somda.sdc.dpws.soap.wsdiscovery.model.HelloType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ProbeMatchType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ProbeMatchesType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ResolveMatchesType;

import javax.xml.namespace.QName;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Provide different functions to resolve a {@link DiscoveredDevice} object from hello or probe messages.
 */
public class DiscoveredDeviceResolver {
    private static final Logger LOG = LogManager.getLogger(DiscoveredDeviceResolver.class);
    private final WsDiscoveryClient wsDiscoveryClient;
    private final Duration maxWaitForResolveMatches;
    private final WsAddressingUtil wsaUtil;
    private final Boolean autoResolve;
    private final Logger instanceLogger;

    /**
     * Assisted constructor.
     *
     * @param wsDiscoveryClient WS-Discovery client to send explicit resolve request if xAddrs are missing.
     */
    @AssistedInject
    DiscoveredDeviceResolver(@Assisted WsDiscoveryClient wsDiscoveryClient,
                             @Named(ClientConfig.MAX_WAIT_FOR_RESOLVE_MATCHES) Duration maxWaitForResolveMatches,
                             @Named(ClientConfig.AUTO_RESOLVE) Boolean autoResolve,
                             WsAddressingUtil wsaUtil,
                             @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.wsDiscoveryClient = wsDiscoveryClient;
        this.maxWaitForResolveMatches = maxWaitForResolveMatches;
        this.autoResolve = autoResolve;
        this.wsaUtil = wsaUtil;
    }

    /**
     * Take a hello message to resolve {@link DiscoveredDevice} object.
     *
     * @param helloMessage The hello message retrieved by a {@link WsDiscoveryClient} implementation.
     * @return The device proxy instance or {@link Optional#empty()} if resolving failed.
     */
    public Optional<DiscoveredDevice> resolve(HelloMessage helloMessage) {
        HelloType hm = helloMessage.getPayload();
        return resolve(hm.getEndpointReference(), hm.getTypes(), hm.getScopes().getValue(), hm.getXAddrs(),
                hm.getMetadataVersion());
    }

    /**
     * Take a probe matches message to resolve {@link DiscoveredDevice} object.
     *
     * @param probeMatchesMessage The probe matches message retrieved by a {@link WsDiscoveryClient} implementation.
     * @return The device proxy instance or {@link Optional#empty()} if resolving failed.
     */
    public Optional<DiscoveredDevice> resolve(ProbeMatchesMessage probeMatchesMessage) {
        ProbeMatchesType probe = probeMatchesMessage.getPayload();
        if (probe.getProbeMatch().size() != 1) {
            return Optional.empty();
        }

        ProbeMatchType pm = probe.getProbeMatch().get(0);
        List<String> scopes = Collections.emptyList();
        if (pm.getScopes() != null) {
            scopes = pm.getScopes().getValue();
        }
        return resolve(pm.getEndpointReference(), pm.getTypes(), scopes, pm.getXAddrs(),
                pm.getMetadataVersion());
    }

    private Optional<DiscoveredDevice> resolve(EndpointReferenceType epr,
                                               List<QName> types,
                                               List<String> scopes,
                                               List<String> xAddrs,
                                               long metadataVersion) {
        if (wsaUtil.getAddressUri(epr).isEmpty()) {
            instanceLogger.info("Empty device endpoint reference found. Skip resolve");
            return Optional.empty();
        }

        if (xAddrs.isEmpty() && autoResolve) {
            return sendResolve(epr).flatMap(rms -> Optional.ofNullable(rms.getResolveMatch()).map(rm ->
                    wsaUtil.getAddressUri(rm.getEndpointReference()).map(uri -> {
                        List<String> rmScopes = Collections.emptyList();
                        if (rm.getScopes() != null) {
                            rmScopes = rm.getScopes().getValue();
                        }
                        return Optional.of(new DiscoveredDevice(
                                uri,
                                rm.getTypes(),
                                rmScopes,
                                rm.getXAddrs(),
                                rm.getMetadataVersion()));
                    })
                            .orElse(Optional.empty()))
                    .orElse(Optional.empty()));
        }

        return wsaUtil.getAddressUri(epr).map(uri -> new DiscoveredDevice(uri, types, scopes, xAddrs, metadataVersion));
    }

    private Optional<ResolveMatchesType> sendResolve(EndpointReferenceType epr) {
        ListenableFuture<ResolveMatchesType> resolveMatches = null;
        try {
            resolveMatches = wsDiscoveryClient.sendResolve(epr);
        } catch (MarshallingException e) {
            instanceLogger.info("Resolve of '{}' failed due to marshalling exception", epr, e.getCause());
        } catch (TransportException e) {
            instanceLogger.info("Transmission of resolve request to '{}' failed", epr, e.getCause());
        } catch (InterceptorException e) {
            instanceLogger.info(e.getMessage(), e.getCause(), e);
        }

        if (resolveMatches != null) {
            try {
                return Optional.ofNullable(
                    resolveMatches.get(maxWaitForResolveMatches.toMillis(), TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                instanceLogger.info("Resolve of '{}' failed due to thread interruption", epr, e.getCause());
            } catch (ExecutionException e) {
                instanceLogger.info("Resolve of '{}' failed", epr, e.getCause());
            } catch (TimeoutException e) {
                resolveMatches.cancel(true);
                instanceLogger.debug("Did not get resolve answer from '{}' within {} ms", wsaUtil.getAddressUri(epr),
                    maxWaitForResolveMatches.toMillis());
            }
        }
        return Optional.empty();
    }
}
