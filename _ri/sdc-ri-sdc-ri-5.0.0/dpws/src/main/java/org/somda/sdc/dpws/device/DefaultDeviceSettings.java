package org.somda.sdc.dpws.device;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.UUID;

/**
 * Default device settings if none are injected to a {@linkplain Device}.
 */
public class DefaultDeviceSettings implements DeviceSettings {
    private static final Logger LOG = LogManager.getLogger(DefaultDeviceSettings.class);

    private final EndpointReferenceType endpointReference;
    private final Logger instanceLogger;

    @Inject
    DefaultDeviceSettings(WsAddressingUtil wsaUtil,
                          SoapUtil soapUtil,
                          @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.endpointReference = wsaUtil.createEprWithAddress(soapUtil.createUriFromUuid(UUID.randomUUID()));
    }

    @Override
    public EndpointReferenceType getEndpointReference() {
        return endpointReference;
    }

    @Override
    public NetworkInterface getNetworkInterface() {
        try {
            return NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
        } catch (SocketException | NullPointerException e) {
            instanceLogger.warn("No default network interface was resolvable: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
