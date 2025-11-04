package it.org.somda.sdc.dpws.soap;


import com.google.common.io.ByteStreams;
import com.google.inject.AbstractModule;
import it.org.somda.sdc.dpws.TestServiceMetadata;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.DpwsUtil;
import org.somda.sdc.dpws.device.DeviceSettings;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.service.factory.HostedServiceFactory;
import org.somda.sdc.dpws.soap.SoapConfig;
import org.somda.sdc.dpws.soap.TransportInfo;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BasicPopulatedDevice extends DevicePeer {
    public static final String SCOPE_1 = "http://integration-test-scope1";
    public static final String SCOPE_2 = "http://integration-test-scope2";

    public static final QName QNAME_1 = new QName("http://type-ns", "integration-test-type1");
    public static final QName QNAME_2 = new QName("http://type-ns", "integration-test-type2");

    private final DpwsFramework dpwsFramework;

    private DpwsTestService1 service1;
    private DpwsTestService2 service2;

    List<TransportInfo> transportInfos;

    public BasicPopulatedDevice() {
        this(null, null);
    }

    public BasicPopulatedDevice(@Nullable DeviceSettings deviceSettings, @Nullable AbstractModule overridingModule) {
        setup(new DefaultDpwsConfigModule() {
            @Override
            public void customConfigure() {
                bind(SoapConfig.JAXB_CONTEXT_PATH, String.class,
                        TestServiceMetadata.JAXB_CONTEXT_PATH);
            }
        }, deviceSettings, overridingModule);
        dpwsFramework = getInjector().getInstance(DpwsFramework.class);
        transportInfos = new ArrayList<>();
        service1 = getInjector().getInstance(DpwsTestService1.class);
        service2 = getInjector().getInstance(DpwsTestService2.class);
    }

    public BasicPopulatedDevice(@Nullable AbstractModule overridingModule) {
        this(null, overridingModule);
    }

    public BasicPopulatedDevice(@Nullable DeviceSettings deviceSettings,
                                DefaultDpwsConfigModule configModule,
                                @Nullable AbstractModule overridingModule) {
        configModule.bind(SoapConfig.JAXB_CONTEXT_PATH, String.class, TestServiceMetadata.JAXB_CONTEXT_PATH);
        setup(configModule, deviceSettings, overridingModule);
        dpwsFramework = getInjector().getInstance(DpwsFramework.class);
        transportInfos = new ArrayList<>();
        service1 = getInjector().getInstance(DpwsTestService1.class);
        service2 = getInjector().getInstance(DpwsTestService2.class);
    }

    public DpwsTestService1 getService1() {
        return service1;
    }

    public DpwsTestService2 getService2() {
        return service2;
    }

    public List<TransportInfo> getTransportInfosReceivedFromService1() {
        return transportInfos;
    }

    @Override
    protected void startUp() throws IOException {
        getDevice().getDiscoveryAccess().setScopes(Arrays.asList(SCOPE_1, SCOPE_2));
        getDevice().getDiscoveryAccess().setTypes(Arrays.asList(QNAME_1, QNAME_2));

        DpwsUtil dpwsUtil = getInjector().getInstance(DpwsUtil.class);

        getDevice().getHostingServiceAccess().setThisDevice(dpwsUtil.createThisDevice(
                dpwsUtil.createLocalizedStrings("en", "BasicPopulatedDevice peer").get(), null, null));

        HostedServiceFactory hostedServiceFactory = getInjector().getInstance(HostedServiceFactory.class);

        service1.setTransportInfoCallback(transportInfos::add);

        final ClassLoader classLoader = getClass().getClassLoader();
        InputStream wsdlResource1 = classLoader.getResourceAsStream(TestServiceMetadata.SERVICE_ID_1_RESOURCE_PATH);
        InputStream wsdlResource2 = classLoader.getResourceAsStream(TestServiceMetadata.SERVICE_ID_2_RESOURCE_PATH);
        assert wsdlResource1 != null;
        getDevice().getHostingServiceAccess().addHostedService(hostedServiceFactory.createHostedService(
                TestServiceMetadata.SERVICE_ID_1,
                Arrays.asList(
                        new QName(TestServiceMetadata.NAMESPACE_SRV, TestServiceMetadata.PORT_TYPE_NAME_1),
                        new QName(TestServiceMetadata.NAMESPACE_SRV, TestServiceMetadata.PORT_TYPE_NAME_2)),
                service1,
                ByteStreams.toByteArray(wsdlResource1)));

        assert wsdlResource2 != null;
        getDevice().getHostingServiceAccess().addHostedService(hostedServiceFactory.createHostedService(
                TestServiceMetadata.SERVICE_ID_2,
                Collections.singletonList(new QName(TestServiceMetadata.NAMESPACE_SRV, TestServiceMetadata.PORT_TYPE_NAME_3)),
                service2,
                ByteStreams.toByteArray(wsdlResource2)));

        dpwsFramework.startAsync().awaitRunning();
        getDevice().startAsync().awaitRunning();
    }

    @Override
    protected void shutDown() {
        getDevice().stopAsync().awaitTerminated();
        dpwsFramework.stopAsync().awaitTerminated();
    }
}
