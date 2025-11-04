package org.somda.sdc.dpws.device;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import org.somda.sdc.common.guice.DefaultCommonModule;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.DpwsUtil;
import org.somda.sdc.dpws.device.factory.DeviceFactory;
import org.somda.sdc.dpws.guice.DefaultDpwsModule;
import org.somda.sdc.dpws.service.factory.HostedServiceFactory;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingConfig;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import test.org.somda.common.TestLogging;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

public class DeviceImplTest implements Runnable {
    public static void main(String[] args) {
        TestLogging.configure();
        new DeviceImplTest().run();
    }

    @Override
    public void run() {
        Injector inj = Guice.createInjector(new DefaultDpwsModule(), new DefaultCommonModule(), new DpwsConfig());

        WsAddressingUtil wsaUtil = inj.getInstance(WsAddressingUtil.class);
        DeviceSettings devConf = new DeviceSettings() {
            @Override
            public synchronized EndpointReferenceType getEndpointReference() {
                return wsaUtil.createEprWithAddress("urn:uuid:00000000-0000-0000-0000-000000000000");
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

        DpwsFramework dpwsFramework = inj.getInstance(DpwsFramework.class);
        dpwsFramework.startAsync().awaitRunning();

        Device device = inj.getInstance(DeviceFactory.class).createDevice(devConf);

        HostedServiceFactory hsFactory = inj.getInstance(HostedServiceFactory.class);
//        HostedService testService = hsFactory.createHostedService("TestService", new WebService() {
//        }, URI.create("http://localhost/wsdl"), null);
//        device.getHostingServiceAccess().addHostedService(testService);

        device.getDiscoveryAccess().setTypes(Collections.singletonList(new QName("http://test-type", "Device")));
        device.getDiscoveryAccess().setScopes(Collections.singletonList("http://test-scope/Scope"));

        DpwsUtil dpwsUtil = inj.getInstance(DpwsUtil.class);
        device.getHostingServiceAccess().setThisDevice(dpwsUtil.createDeviceBuilder()
                .setFriendlyName(dpwsUtil.createLocalizedStrings()
                        .add("en", "Draeger IACS Monitoring Unit")
                        .get())
                .setFirmwareVersion("v1.2.3")
                .setSerialNumber("1234-5678-9101-1121").get());

        device.getHostingServiceAccess().setThisModel(dpwsUtil.createModelBuilder()
                .setManufacturer(dpwsUtil.createLocalizedStrings()
                        .add("en", "Draegerwerk AG")
                        .add("de", "Drägerwerk AG")
                        .get())
                .setModelName(dpwsUtil.createLocalizedStrings()
                        .add("IACS")
                        .get())
                .setPresentationUrl("http://www.google.com")
                .get());

        device.startAsync().awaitRunning();

        System.out.println("Device started");
        try {
            int value;
            while ((value = System.in.read()) != -1) {
                System.out.println((char) value);
                if (((char) value) == '\n') {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Stop device");
        device.stopAsync().awaitTerminated();
        dpwsFramework.stopAsync().awaitTerminated();
        System.out.println("Device stopped");
    }

    private static class DpwsConfig extends AbstractModule {
        @Override
        protected void configure() {
            bind(Boolean.class)
                    .annotatedWith(Names.named(WsAddressingConfig.IGNORE_MESSAGE_IDS))
                    .toInstance(Boolean.FALSE);
        }
    }
}