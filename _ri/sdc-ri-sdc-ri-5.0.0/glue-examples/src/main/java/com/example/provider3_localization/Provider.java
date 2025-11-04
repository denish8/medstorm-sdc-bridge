package com.example.provider3_localization;

import com.google.common.util.concurrent.AbstractIdleService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.provider.access.factory.LocalMdibAccessFactory;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.DpwsUtil;
import org.somda.sdc.dpws.device.DeviceSettings;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.glue.common.MdibXmlIo;
import org.somda.sdc.glue.common.factory.ModificationsBuilderFactory;
import org.somda.sdc.glue.provider.SdcDevice;
import org.somda.sdc.glue.provider.factory.SdcDeviceFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;

/**
 * The example provider with a localization service available.
 */
public class Provider extends AbstractIdleService {
    private static final Logger LOG = LogManager.getLogger(Provider.class);

    private final DpwsFramework dpwsFramework;
    private final LocalMdibAccess mdibAccess;
    private final SdcDevice sdcDevice;
    private final MdibXmlIo mdibXmlIo;
    private final ModificationsBuilderFactory modificationsBuilderFactory;

    Provider(ProviderUtil providerUtil) throws SocketException, UnknownHostException {
        var injector = providerUtil.getInjector();

        this.mdibXmlIo = injector.getInstance(MdibXmlIo.class);
        this.modificationsBuilderFactory = injector.getInstance(ModificationsBuilderFactory.class);
        this.dpwsFramework = injector.getInstance(DpwsFramework.class);
        this.mdibAccess = injector.getInstance(LocalMdibAccessFactory.class).createLocalMdibAccess();

        NetworkInterface networkInterface;
        if (providerUtil.getIface() != null && !providerUtil.getIface().isEmpty()) {
            LOG.info("Starting with interface {}", providerUtil.getIface());
            networkInterface = NetworkInterface.getByName(providerUtil.getIface());
        } else {
            if (providerUtil.getAddress() != null && !providerUtil.getAddress().isBlank()) {
                // bind to adapter matching ip
                LOG.info("Starting with address {}", providerUtil.getAddress());
                networkInterface = NetworkInterface.getByInetAddress(
                        InetAddress.getByName(providerUtil.getAddress())
                );
            } else {
                // find loopback interface for fallback
                networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
                LOG.info("Starting with fallback default adapter {}", networkInterface);
            }
        }
        assert networkInterface != null;
        this.dpwsFramework.setNetworkInterface(networkInterface);

        var epr = providerUtil.getEpr();
        if (epr == null) {
            epr = injector.getInstance(SoapUtil.class).createRandomUuidUri();
            LOG.info("No epr address provided, generated random epr {}", epr);
        }

        var finalEpr = epr;
        this.sdcDevice = injector.getInstance(SdcDeviceFactory.class)
                .createSdcDevice(
                        new DeviceSettings() {
                            @Override
                            public EndpointReferenceType getEndpointReference() {
                                return injector.getInstance(WsAddressingUtil.class)
                                        .createEprWithAddress(finalEpr);
                            }

                            @Override
                            public NetworkInterface getNetworkInterface() {
                                return networkInterface;
                            }
                        },
                        this.mdibAccess,
                        Collections.emptyList(),
                        Collections.emptyList(),
                        providerUtil.getLocalizationStorage(),
                        Collections.emptyList());

        DpwsUtil dpwsUtil = injector.getInstance(DpwsUtil.class);
        var thisDeviceType = dpwsUtil.createDeviceBuilder()
                .setFriendlyName(dpwsUtil.createLocalizedStrings(
                        "en", "Provider with localization service example").get())
                .get();
        sdcDevice.getDevice().getHostingServiceAccess().setThisDevice(thisDeviceType);
    }

    public static void main(String[] args) throws SocketException, UnknownHostException {
        var provider = new Provider(new ProviderUtil(args));
        provider.startAsync().awaitRunning();
        //provider.stopAsync().awaitTerminated();
    }

    @Override
    protected void startUp() throws Exception {
        // load initial MDIB including the descriptor extension from file
        var mdibAsStream = com.example.provider1.Provider.class.getClassLoader()
                .getResourceAsStream("provider3_localization/mdib.xml");
        assert mdibAsStream != null;

        var mdib = mdibXmlIo.readMdib(mdibAsStream);
        var modifications = modificationsBuilderFactory.createModificationsBuilder(mdib, true).get();
        mdibAccess.writeDescription(modifications);

        dpwsFramework.startAsync().awaitRunning();
        sdcDevice.startAsync().awaitRunning();
    }

    @Override
    protected void shutDown() {
        sdcDevice.stopAsync().awaitTerminated();
        dpwsFramework.stopAsync().awaitTerminated();
    }
}
