package org.somda.sdc.glue.provider;

import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.device.Device;
import org.somda.sdc.dpws.device.DeviceSettings;
import org.somda.sdc.dpws.device.DiscoveryAccess;
import org.somda.sdc.dpws.device.EventSourceAccess;
import org.somda.sdc.dpws.device.HostingServiceAccess;
import org.somda.sdc.dpws.device.factory.DeviceFactory;
import org.somda.sdc.dpws.service.HostedService;
import org.somda.sdc.dpws.service.factory.HostedServiceFactory;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.wseventing.SubscriptionManager;
import org.somda.sdc.dpws.soap.wseventing.model.WsEventingStatus;
import org.somda.sdc.glue.GlueConstants;
import org.somda.sdc.glue.common.CommonConstants;
import org.somda.sdc.glue.common.WsdlConstants;
import org.somda.sdc.glue.provider.helper.SdcDevicePluginProcessor;
import org.somda.sdc.glue.provider.localization.LocalizationStorage;
import org.somda.sdc.glue.provider.plugin.SdcRequiredTypesAndScopes;
import org.somda.sdc.glue.provider.sco.OperationInvocationReceiver;
import org.somda.sdc.glue.provider.services.HighPriorityServices;
import org.somda.sdc.glue.provider.services.LowPriorityServices;
import org.somda.sdc.glue.provider.services.factory.ServicesFactory;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Adds SDC services to a DPWS device and manages incoming set service requests.
 * <p>
 * The purpose of the {@linkplain SdcDevice} class is to provide SDC data on the network.
 */
public class SdcDevice extends AbstractIdleService implements Device, EventSourceAccess, SdcDeviceContext {
    private final Device dpwsDevice;
    private final HostedServiceFactory hostedServiceFactory;
    private final HighPriorityServices highPriorityServices;
    private final Collection<OperationInvocationReceiver> operationInvocationReceivers;
    private final LocalMdibAccess mdibAccess;
    private final SdcDevicePluginProcessor pluginProcessor;
    private final LowPriorityServices lowPriorityServices;
    private final LocalizationStorage localizationStorage;
    private final List<HostedService> customHostedServices;

    @AssistedInject
    SdcDevice(@Assisted DeviceSettings deviceSettings,
              @Assisted LocalMdibAccess mdibAccess,
              @Assisted("operationInvocationReceivers")
                      Collection<OperationInvocationReceiver> operationInvocationReceivers,
              @Assisted("plugins") Collection<SdcDevicePlugin> plugins,
              Provider<SdcRequiredTypesAndScopes> sdcRequiredTypesAndScopesProvider,
              DeviceFactory deviceFactory,
              ServicesFactory servicesFactory,
              HostedServiceFactory hostedServiceFactory) {

        this(deviceSettings, mdibAccess, operationInvocationReceivers, plugins, null, Collections.emptyList(),
                sdcRequiredTypesAndScopesProvider, deviceFactory, servicesFactory, hostedServiceFactory);
    }

    @AssistedInject
    SdcDevice(@Assisted DeviceSettings deviceSettings,
              @Assisted LocalMdibAccess mdibAccess,
              @Assisted("operationInvocationReceivers")
                      Collection<OperationInvocationReceiver> operationInvocationReceivers,
              @Assisted("plugins") Collection<SdcDevicePlugin> plugins,
              @Assisted @Nullable LocalizationStorage localizationStorage,
              @Assisted List<HostedService> customHostedServices,
              Provider<SdcRequiredTypesAndScopes> sdcRequiredTypesAndScopesProvider,
              DeviceFactory deviceFactory,
              ServicesFactory servicesFactory,
              HostedServiceFactory hostedServiceFactory) {
        this.customHostedServices = customHostedServices;

        // Always support the minimally required types and scopes
        var copyPlugins = plugins;
        if (copyPlugins.isEmpty()) {
            copyPlugins = Collections.singleton(sdcRequiredTypesAndScopesProvider.get());
        }

        this.mdibAccess = mdibAccess;
        this.dpwsDevice = deviceFactory.createDevice(deviceSettings);
        this.highPriorityServices = servicesFactory.createHighPriorityServices(mdibAccess);
        this.lowPriorityServices = servicesFactory.createLowPriorityServices(mdibAccess, localizationStorage);
        this.hostedServiceFactory = hostedServiceFactory;
        this.operationInvocationReceivers = operationInvocationReceivers;

        this.pluginProcessor = new SdcDevicePluginProcessor(copyPlugins, this);
        this.localizationStorage = localizationStorage;
    }

    public LocalMdibAccess getMdibAccess() {
        return mdibAccess;
    }

    @Override
    public String getEprAddress() {
        return dpwsDevice.getEprAddress();
    }

    @Override
    public Map<String, SubscriptionManager> getActiveSubscriptions() {
        return dpwsDevice.getActiveSubscriptions();
    }

    @Override
    public Device getDevice() {
        return dpwsDevice;
    }

    @Override
    public LocalMdibAccess getLocalMdibAccess() {
        return mdibAccess;
    }

    @Override
    public Collection<OperationInvocationReceiver> getOperationInvocationReceivers() {
        return operationInvocationReceivers;
    }

    @Override
    public State getServiceState() {
        return state();
    }

    @Override
    public <T extends AbstractState> void sendPeriodicStateReport(Map<String, List<T>> states, MdibVersion mdibVersion) {
        highPriorityServices.sendPeriodicStateReport(states, mdibVersion);
    }

    /**
     * Gets the discovery access.
     *
     * <em>Please note that the discovery access is managed by this class.
     * Overwriting types and/or scopes can cause negative side-effects.</em>
     *
     * <em>Prefer using the {@link SdcRequiredTypesAndScopes} plugin to manage discovery.</em>
     *
     * @return the discovery access.
     */
    @Override
    public DiscoveryAccess getDiscoveryAccess() {
        return new DiscoveryAccess() {
            @Override
            public void setTypes(Collection<QName> types) {
                ArrayList<QName> tmpTypes = new ArrayList<>();
                if (types.stream().filter(qName -> qName.equals(CommonConstants.MEDICAL_DEVICE_TYPE))
                        .findAny().isEmpty()) {
                    tmpTypes.add(DpwsConstants.DEVICE_TYPE);
                }
                tmpTypes.addAll(types);
                dpwsDevice.getDiscoveryAccess().setTypes(tmpTypes);
            }

            @Override
            public void setScopes(Collection<String> scopes) {
                ArrayList<String> tmpScopes = new ArrayList<>();
                if (scopes.stream().filter(scope -> scope.equals(GlueConstants.SCOPE_SDC_PROVIDER))
                        .findAny().isEmpty()) {
                    tmpScopes.add(GlueConstants.SCOPE_SDC_PROVIDER);
                }
                tmpScopes.addAll(scopes);
                dpwsDevice.getDiscoveryAccess().setScopes(tmpScopes);
            }

            @Override
            public void sendHello() {
                dpwsDevice.getDiscoveryAccess().sendHello();
            }
        };
    }

    /**
     * Gets the hosting service access.
     * <p>
     * As the BICEPS services are managed by this class, there should not be any need to access the hosting services.
     * In case access is required though, consider creating a plugin that accesses the hosting services.
     *
     * <em>Prefer implementing a plugin and use {@link Device#getHostingServiceAccess()}
     * from {@link SdcDeviceContext#getDevice()}.</em>
     *
     * @return the hosting service access.
     */
    @Override
    public HostingServiceAccess getHostingServiceAccess() {
        return dpwsDevice.getHostingServiceAccess();
    }

    @Override
    protected void startUp() throws Exception {
        setupInvocationReceivers();
        setupHostedServices();

        pluginProcessor.beforeStartUp();
        dpwsDevice.startAsync().awaitRunning();
        pluginProcessor.afterStartUp();
    }

    @Override
    protected void shutDown() {
        pluginProcessor.beforeShutDown();
        mdibAccess.unregisterAllObservers();
        dpwsDevice.stopAsync().awaitTerminated();
        pluginProcessor.afterShutDown();
    }

    private void setupHostedServices() throws IOException {
        final ClassLoader classLoader = getClass().getClassLoader();
        InputStream highPrioWsdlStream =
                classLoader.getResourceAsStream("wsdl/IEEE11073-20701-HighPriority-Services.wsdl");
        assert highPrioWsdlStream != null;
        dpwsDevice.getHostingServiceAccess().addHostedService(hostedServiceFactory.createHostedService(
                "HighPriorityServices",
                Arrays.asList(
                        new QName(WsdlConstants.TARGET_NAMESPACE, WsdlConstants.SERVICE_GET),
                        new QName(WsdlConstants.TARGET_NAMESPACE, WsdlConstants.SERVICE_SET),
                        new QName(WsdlConstants.TARGET_NAMESPACE, WsdlConstants.SERVICE_CONTAINMENT_TREE),
                        new QName(WsdlConstants.TARGET_NAMESPACE, WsdlConstants.SERVICE_CONTEXT),
                        new QName(WsdlConstants.TARGET_NAMESPACE, WsdlConstants.SERVICE_DESCRIPTION_EVENT),
                        new QName(WsdlConstants.TARGET_NAMESPACE, WsdlConstants.SERVICE_STATE_EVENT),
                        new QName(WsdlConstants.TARGET_NAMESPACE, WsdlConstants.SERVICE_WAVEFORM)),
                highPriorityServices,
                ByteStreams.toByteArray(highPrioWsdlStream)));

        InputStream lowPrioWsdlStream =
                classLoader.getResourceAsStream("wsdl/IEEE11073-20701-LowPriority-Services.wsdl");
        if (lowPrioWsdlStream != null) {
            dpwsDevice.getHostingServiceAccess().addHostedService(hostedServiceFactory.createHostedService(
                    "LowPriorityServices",
                    getQNames(),
                    lowPriorityServices,
                    ByteStreams.toByteArray(lowPrioWsdlStream)));
        }

        customHostedServices.forEach(dpwsDevice.getHostingServiceAccess()::addHostedService);
    }

    private void addOperationInvocationReceiver(OperationInvocationReceiver receiver) {
        highPriorityServices.addOperationInvocationReceiver(receiver);
    }

    private void setupInvocationReceivers() {
        operationInvocationReceivers.forEach(this::addOperationInvocationReceiver);
    }

    private List<QName> getQNames() {
        var qNames = new ArrayList<QName>();

        if (localizationStorage != null) {
            qNames.add(new QName(WsdlConstants.TARGET_NAMESPACE, WsdlConstants.SERVICE_LOCALIZATION));
        }

        return qNames;
    }

    @Override
    public void sendNotification(String action, Object payload) throws MarshallingException, TransportException {
        highPriorityServices.sendNotification(action, payload);
    }

    @Override
    public void subscriptionEndToAll(WsEventingStatus status) throws TransportException {
        highPriorityServices.subscriptionEndToAll(status);
    }
}
