package it.org.somda.glue.provider;

import com.google.inject.Injector;
import it.org.somda.glue.IntegrationTestUtil;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.MdibTypeValidator;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.model.participant.Mdib;
import org.somda.sdc.biceps.model.participant.NumericMetricState;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.provider.access.factory.LocalMdibAccessFactory;
import org.somda.sdc.biceps.testutil.BaseTreeModificationsSet;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.biceps.testutil.MockEntryFactory;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.device.DeviceSettings;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.glue.common.CommonConstants;
import org.somda.sdc.glue.common.MdibMapper;
import org.somda.sdc.glue.common.MdibXmlIo;
import org.somda.sdc.glue.common.factory.MdibMapperFactory;
import org.somda.sdc.glue.provider.SdcDevice;
import org.somda.sdc.glue.provider.factory.SdcDeviceFactory;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

public class RunTestSdcDevice {
    private static final IntegrationTestUtil IT = new IntegrationTestUtil();

    public static void main(String[] args) throws IOException, PreprocessingException, JAXBException {
        final Injector injector = IT.getInjector();

        final NetworkInterface networkInterface = NetworkInterface.getByName("eth0"); // "wlan1");//InetAddress.getLocalHost());

        final DpwsFramework dpwsFramework = injector.getInstance(DpwsFramework.class);
        dpwsFramework.setNetworkInterface(networkInterface);

        BaseTreeModificationsSet baseTreeModificationsSet = new BaseTreeModificationsSet(new MockEntryFactory(
                injector.getInstance(MdibTypeValidator.class)));

        final LocalMdibAccess localMdibAccess = injector.getInstance(LocalMdibAccessFactory.class).createLocalMdibAccess();
        localMdibAccess.writeDescription(baseTreeModificationsSet.createFullyPopulatedTree());

        final SdcDevice sdcDevice = injector.getInstance(SdcDeviceFactory.class).createSdcDevice(new DeviceSettings() {
            @Override
            public EndpointReferenceType getEndpointReference() {
                return injector.getInstance(WsAddressingUtil.class)
                        .createEprWithAddress("urn:uuid:857bf583-8a51-475f-a77f-d0ca7de69b00");
            }

            @Override
            public NetworkInterface getNetworkInterface() {
                return networkInterface;
            }
        }, localMdibAccess, Collections.emptyList(), Collections.emptyList());

        sdcDevice.getDiscoveryAccess().setTypes(Arrays.asList(CommonConstants.MEDICAL_DEVICE_TYPE));

        final MdibMapperFactory mapperFactory = injector.getInstance(MdibMapperFactory.class);
        final MdibXmlIo mdibXmlIo = injector.getInstance(MdibXmlIo.class);
        final MdibMapper mdibMapper = mapperFactory.createMdibMapper(localMdibAccess);
        final Mdib expectedMdib = mdibMapper.mapMdib();
        mdibXmlIo.writeMdib(expectedMdib, new File("C:\\temp\\mdib.xml"));

        dpwsFramework.startAsync().awaitRunning();
        sdcDevice.startAsync().awaitRunning();

        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    Optional<NumericMetricState> state = localMdibAccess.getState(Handles.METRIC_0, NumericMetricState.class);
                    NumericMetricState clone = (NumericMetricState)state.get().clone();
                    clone.getMetricValue().setValue(clone.getMetricValue().getValue().add(BigDecimal.ONE));
                    localMdibAccess.writeStates(MdibStateModifications.create(MdibStateModifications.Type.METRIC)
                    .add(clone));
                } catch (InterruptedException | PreprocessingException e) {
                    e.printStackTrace();
                    break;
                }
            }
        });
        thread.setDaemon(false);
        thread.start();

        System.in.read();

        thread.interrupt();
        sdcDevice.stopAsync().awaitTerminated();
        dpwsFramework.stopAsync().awaitTerminated();
    }
}
