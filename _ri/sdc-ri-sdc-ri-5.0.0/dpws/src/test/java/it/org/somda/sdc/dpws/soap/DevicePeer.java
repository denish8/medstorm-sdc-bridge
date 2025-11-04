package it.org.somda.sdc.dpws.soap;

import com.google.inject.AbstractModule;
import it.org.somda.sdc.dpws.IntegrationTestPeer;
import org.somda.sdc.dpws.device.Device;
import org.somda.sdc.dpws.device.DeviceSettings;
import org.somda.sdc.dpws.device.factory.DeviceFactory;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.UUID;

public abstract class DevicePeer extends IntegrationTestPeer {
    private String eprAddress;
    private Device device;
    private boolean isSetup;

    public DevicePeer() {
        isSetup = false;
    }

    void setup(DefaultDpwsConfigModule configModule, @Nullable DeviceSettings deviceSettings, @Nullable AbstractModule overridingModule) {
        setupInjector(configModule, overridingModule);

        if (deviceSettings == null) {
            this.eprAddress = getInjector().getInstance(SoapUtil.class).createUriFromUuid(UUID.randomUUID());
            final WsAddressingUtil wsaUtil = getInjector().getInstance(WsAddressingUtil.class);
            final EndpointReferenceType epr = wsaUtil.createEprWithAddress(this.eprAddress);
            deviceSettings = new DeviceSettings() {
                @Override
                public EndpointReferenceType getEndpointReference() {
                    return epr;
                }

                @Override
                public NetworkInterface getNetworkInterface() {
                    try {
                        return NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        } else {
            this.eprAddress = deviceSettings.getEndpointReference().getAddress().getValue();
        }

        this.device = getInjector().getInstance(DeviceFactory.class).createDevice(deviceSettings);

        isSetup = true;
    }

    public String getEprAddress() {
        checkSetup();
        return eprAddress;
    }

    public Device getDevice() {
        checkSetup();
        return device;
    }

    private void checkSetup() {
        if (!isSetup) {
            throw new RuntimeException("Call setup() before access getter method");
        }
    }

}
