package com.example.provider2_extension;

import com.google.common.util.concurrent.AbstractIdleService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.model.extension.ExtensionType;
import org.somda.sdc.biceps.model.participant.AbstractMetricValue;
import org.somda.sdc.biceps.model.participant.GenerationMode;
import org.somda.sdc.biceps.model.participant.MeasurementValidity;
import org.somda.sdc.biceps.model.participant.NumericMetricState;
import org.somda.sdc.biceps.model.participant.NumericMetricValue;
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
import org.somda.sdc.glue.examples.extension.CompiledExtension;
import org.somda.sdc.glue.provider.SdcDevice;
import org.somda.sdc.glue.provider.factory.SdcDeviceFactory;
import org.somda.sdc.glue.provider.plugin.SdcRequiredTypesAndScopes;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Collections;

/**
 * Provider with a descriptor and state extension.
 */
public class Provider extends AbstractIdleService {
    private static final Logger LOG = LogManager.getLogger(Provider.class);

    private static final String NUMERIC_METRIC_HANDLE = "numeric.ch0.vmd0";
    private static final String MY_EXTENSION_NAMESPACE = "http://biceps.extension";
    private static final String MY_EXTENSION_STATE_NAME = "MyStateExtension";

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
                .createSdcDevice(new DeviceSettings() {
                                     @Override
                                     public EndpointReferenceType getEndpointReference() {
                                         return injector.getInstance(WsAddressingUtil.class)
                                                 .createEprWithAddress(finalEpr);
                                     }

                                     @Override
                                     public NetworkInterface getNetworkInterface() {
                                         return networkInterface;
                                     }
                                 }, this.mdibAccess, Collections.emptyList(),
                        Collections.singleton(injector.getInstance(SdcRequiredTypesAndScopes.class)));

        DpwsUtil dpwsUtil = injector.getInstance(DpwsUtil.class);
        sdcDevice.getHostingServiceAccess().setThisDevice(dpwsUtil.createDeviceBuilder()
                .setFriendlyName(dpwsUtil.createLocalizedStrings()
                        .add("en", "Provider with extensions example")
                        .get()).get());
    }

    public static void main(String[] args) throws SocketException, UnknownHostException {
        // starts an MDIB with a descriptor and state extensions
        // runs with state updates every 5 seconds

        var providerUtil = new ProviderUtil(args);
        var provider = new Provider(providerUtil);
        provider.startAsync().awaitRunning();

        var reportInterval = providerUtil.getReportInterval().toMillis();
        LOG.info("Sending metric state report with extension every {} ms", reportInterval);
        var thread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(reportInterval);
                    provider.changeNumericMetric();
                } catch (InterruptedException | PreprocessingException e) {
                    LOG.warn("Thread loop stopping", e);
                    break;
                }
            }
        });
        thread.setDaemon(true);
        thread.start();

        try {
            System.in.read();
        } catch (IOException e) {
            // pass and quit
        }

        thread.interrupt();
        provider.stopAsync().awaitTerminated();
    }

    void changeNumericMetric() throws PreprocessingException {
        var stateOpt = mdibAccess.getState(NUMERIC_METRIC_HANDLE, NumericMetricState.class);
        assert stateOpt.isPresent();

        // Change standardized values
        var state = stateOpt.get();
        var val = state.getMetricValue();
        if (val != null && val.getValue() != null) {
            val.setValue(val.getValue().add(BigDecimal.ONE));
        } else {
            val = new NumericMetricValue();
            val.setValue(BigDecimal.ONE);
        }
        val.setDeterminationTime(Instant.now());
        if (val.getMetricQuality() == null) {
            var qual = new AbstractMetricValue.MetricQuality();
            qual.setMode(GenerationMode.DEMO);
            qual.setValidity(MeasurementValidity.VLD);
            val.setMetricQuality(qual);
        }
        state.setMetricValue(val);

        var myExtension = new JAXBElement<>(new QName(MY_EXTENSION_NAMESPACE, MY_EXTENSION_STATE_NAME), String.class, "");

        // Change extension value
        if (state.getExtension() != null) {
            if (!state.getExtension().getAny().isEmpty()) {
                myExtension = (JAXBElement<String>) state.getExtension().getAny().get(0);
            }
        } else {
            state.setExtension(new ExtensionType());
        }
        myExtension.setValue("Extension value " + val.getValue().toString());
        state.getExtension().getAny().clear();
        state.getExtension().getAny().add(myExtension);

        var compiledExtension = new CompiledExtension();
        compiledExtension.setField1("Foo");
        compiledExtension.setField2(BigInteger.TEN);
        state.getExtension().getAny().add(compiledExtension);

        mdibAccess.writeStates(MdibStateModifications.create(MdibStateModifications.Type.METRIC).add(state));
        LOG.info("Changed numeric metric value to {}", val.getValue());
    }

    @Override
    protected void startUp() throws Exception {
        // load initial MDIB including the descriptor extension from file
        var mdibAsStream = com.example.provider1.Provider.class.getClassLoader()
                .getResourceAsStream("provider2_extension/Mdib.xml");
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
