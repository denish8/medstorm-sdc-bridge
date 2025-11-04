package com.example.consumer3_localization;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.message.GetLocalizedText;
import org.somda.sdc.biceps.model.message.GetSupportedLanguages;
import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.biceps.model.participant.MdsDescriptor;
import org.somda.sdc.common.util.AutoLock;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.client.Client;
import org.somda.sdc.dpws.client.DiscoveryObserver;
import org.somda.sdc.dpws.client.event.DeviceEnteredMessage;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.glue.consumer.ConnectConfiguration;
import org.somda.sdc.glue.consumer.PrerequisitesException;
import org.somda.sdc.glue.consumer.SdcRemoteDevice;
import org.somda.sdc.glue.consumer.SdcRemoteDevicesConnector;
import org.somda.sdc.glue.consumer.localization.LocalizationServiceAccess;
import org.somda.sdc.glue.consumer.sco.InvocationException;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * This is an example consumer that connects to the provider and queries Localization service.
 */
public class Consumer extends AbstractIdleService {
    private static final Logger LOG = LogManager.getLogger(Consumer.class);
    private static final Duration MAX_WAIT = Duration.ofSeconds(11);
    private static final long MAX_WAIT_SEC = MAX_WAIT.getSeconds();

    private static final String MDS_0 = "mds_0";
    private static final String EXPECTED_CODING_SYSTEM_NAME = "Common Parameter Nomenclature";
    private static final String EXPECTED_CONCEPT_DESCRIPTION = "Konzeptbeschreibung";

    private final ConsumerUtil consumerUtil;
    private final Client client;
    private final SdcRemoteDevicesConnector connector;
    private final DpwsFramework dpwsFramework;
    private final NetworkInterface networkInterface;
    private final Thread connectorThread;
    private final Lock connectLock;
    private final Condition connectCondition;
    private HostingServiceProxy hostingServiceProxy;
    private SdcRemoteDevice sdcRemoteDevice;
    private LocalizationServiceAccess localizationService;
    private int connectCount;

    Consumer(ConsumerUtil consumerUtil) throws SocketException, UnknownHostException {
        this.consumerUtil = consumerUtil;

        this.hostingServiceProxy = null;
        this.sdcRemoteDevice = null;

        this.connectCount = 0;
        this.connectLock = new ReentrantLock();
        this.connectCondition = connectLock.newCondition();
        this.connectCount = 0;

        var injector = consumerUtil.getInjector();
        this.dpwsFramework = injector.getInstance(DpwsFramework.class);
        this.client = injector.getInstance(Client.class);
        this.connector = injector.getInstance(SdcRemoteDevicesConnector.class);
        if (consumerUtil.getIface() != null && !consumerUtil.getIface().isBlank()) {
            LOG.info("Starting with interface {}", consumerUtil.getIface());
            this.networkInterface = NetworkInterface.getByName(consumerUtil.getIface());
        } else {
            if (consumerUtil.getAddress() != null && !consumerUtil.getAddress().isBlank()) {
                // bind to adapter matching ip
                LOG.info("Starting with address {}", consumerUtil.getAddress());
                this.networkInterface = NetworkInterface.getByInetAddress(
                        InetAddress.getByName(consumerUtil.getAddress())
                );
            } else {
                // find loopback interface for fallback
                networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
                LOG.info("Starting with fallback default adapter {}", networkInterface);
            }
        }

        this.connectorThread = new Thread(new ConnectorThread());
        connectorThread.setDaemon(true);
    }

    public static void main(String[] args) throws Exception {
        var settings = new ConsumerUtil(args);
        var targetEpr = settings.getEpr();
        if (targetEpr == null || targetEpr.isEmpty()) {
            LOG.error("An EPR is required but was not found (see command line argument --epr)");
            System.exit(1);
        }

        var consumer = new Consumer(settings);
        consumer.startAsync().awaitRunning();

        LOG.info("Press any key to exit");
        try {
            System.in.read();
        } catch (IOException e) {
            // pass and quit
        }
        LOG.info("Shutting down");
        consumer.stopAsync().awaitTerminated();

    }

    protected void startUp() {
        dpwsFramework.setNetworkInterface(networkInterface);
        dpwsFramework.startAsync().awaitRunning();
        client.startAsync().awaitRunning();

        connectorThread.start();

        LOG.info("Starting implicit discovery of hosting service with EPR {}", consumerUtil.getEpr());
        client.registerDiscoveryObserver(new ImplicitDiscovery());

        LOG.info("Starting explicit discovery of hosting service with EPR {}", consumerUtil.getEpr());
        triggerConnect();
    }

    protected void shutDown() {
        connectorThread.interrupt();
        connector.stopAsync().awaitTerminated();
        client.stopAsync().awaitTerminated();
        dpwsFramework.stopAsync().awaitTerminated();
    }

    private void triggerConnect() {
        try (var ignored = AutoLock.lock(connectLock)) {
            connectCount++;
            connectCondition.signalAll();
        }
    }

    class ConnectorThread implements Runnable {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try (var ignored = AutoLock.lock(connectLock)) {
                    if (connectCount == 0) {
                        connectCondition.await();
                        if (connectCount == 0) {
                            continue;
                        }
                    }
                    connectCount--;
                    connect();
                } catch (InterceptorException e) {
                    LOG.warn(e);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        private void connect() throws InterceptorException {
            if (hostingServiceProxy != null) {
                LOG.info("Skip connect, SdcDevice with EPR {} already connected", consumerUtil.getEpr());
                return;
            }
            LOG.info("Connect to EPR {}", consumerUtil.getEpr());

            var hostingServiceFuture = client.connect(consumerUtil.getEpr());
            try {
                hostingServiceProxy = hostingServiceFuture.get(MAX_WAIT_SEC, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                LOG.warn("Explicit discovery failed after {}s. Waiting for device to join the network.", MAX_WAIT_SEC, e);
                return;
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("Explicit discovery failed. Waiting for device to join the network.", e);
                return;
            }

            try {
                var remoteDeviceFuture = connector
                    .connect(
                        hostingServiceProxy,
                        ConnectConfiguration.create(ConnectConfiguration.ALL_EPISODIC_AND_WAVEFORM_REPORTS)
                    );
                sdcRemoteDevice = remoteDeviceFuture.get(MAX_WAIT_SEC, TimeUnit.SECONDS);
                localizationService = sdcRemoteDevice.getLocalizationServiceAccess();
            } catch (TimeoutException e) {
                LOG.error("Couldn't attach to remote MDIB and subscriptions for {} after {}s", consumerUtil.getEpr(), MAX_WAIT_SEC, e);
                System.exit(1);
            } catch (PrerequisitesException | InterruptedException | ExecutionException e) {
                LOG.error("Couldn't attach to remote MDIB and subscriptions for {}", consumerUtil.getEpr(), e);
                System.exit(1);
            }

            var testSuccessful = true;

            var codingSystemName = getCodingSystemName();

            if (codingSystemName != null && codingSystemName.getVersion() != null) {
                preFetchCache(codingSystemName.getVersion());
            }

            if (codingSystemName != null && StringUtils.isNotBlank(codingSystemName.getRef())) {
                var translatedText = requestTranslation(codingSystemName).getValue();
                if (EXPECTED_CODING_SYSTEM_NAME.equals(translatedText)) {
                    LOG.info("Successfully fetch translation for 'Coding System Name': '{}'", translatedText);
                } else {
                    LOG.error("'Coding System Name' translation '{}' doesn't match expected value {}",
                            translatedText, EXPECTED_CODING_SYSTEM_NAME);
                    testSuccessful = false;
                }
            }

            var conceptDescription = getConceptDescription();
            if (conceptDescription != null && StringUtils.isNotBlank(conceptDescription.getRef())) {
                var translatedText = requestTranslation(conceptDescription).getValue();
                if (EXPECTED_CONCEPT_DESCRIPTION.equals(translatedText)) {
                    LOG.info("Successfully fetch translation for 'Concept Description': '{}'", translatedText);
                } else {
                    LOG.error("'Concept Description' translation '{}' doesn't match expected value {}",
                            translatedText, EXPECTED_CONCEPT_DESCRIPTION);
                    testSuccessful = false;
                }
            }
            System.exit(testSuccessful ? 0 : 1);
        }

        private LocalizedText getCodingSystemName() {
            var mds = getMdsDescriptor();
            var codingSystemNameList = mds.getType() != null ?
                    mds.getType().getCodingSystemName() :
                    Collections.emptyList();
            if (codingSystemNameList.isEmpty()) {
                LOG.warn("Failed to get 'Coding System Name' data");
                return null;
            }
            return mds.getType().getCodingSystemName().get(0);
        }

        private LocalizedText getConceptDescription() {
            var mds = getMdsDescriptor();
            var conceptDescriptionList = mds.getType() != null ?
                    mds.getType().getConceptDescription() :
                    Collections.emptyList();
            if (conceptDescriptionList.isEmpty()) {
                LOG.warn("Failed to get 'Concept description' data");
                return null;
            }
            return mds.getType().getConceptDescription().get(0);
        }

        private MdsDescriptor getMdsDescriptor() {
            var mds = sdcRemoteDevice.getMdibAccess().getDescriptor(MDS_0, MdsDescriptor.class);
            if (mds.isEmpty()) {
                LOG.error("Couldn't get MdsDescriptor from remote device, exiting..");
                System.exit(1);
            }

            return mds.get();
        }

        /**
         * Prefetch localized text translations cache for all supported languages.
         * @param version the version of localized text translations which should be cached.
         */
        private void preFetchCache(BigInteger version) {
            try {
                var supportedLangResponse = localizationService.getSupportedLanguages(
                        new GetSupportedLanguages()).get();
                localizationService.cachePrefetch(version, supportedLangResponse.getLang());
            } catch (InvocationException | ExecutionException | InterruptedException e) {
                LOG.warn("Failed to prefetch localized texts cache.", e);
            }
        }

        private LocalizedText requestTranslation(LocalizedText localizedText) {
            var request = new GetLocalizedText();
            request.setVersion(localizedText.getVersion());
            request.setLang(List.of(localizedText.getLang()));
            request.setRef(List.of(localizedText.getRef()));
            try {
                var response = localizationService.getLocalizedText(request).get();
                return response.getText().get(0);
            } catch (InterruptedException | ExecutionException | IndexOutOfBoundsException e) {
                LOG.warn("Failed to fetch translation for localized text: {}", localizedText.toString(), e);
                return localizedText;
            }
        }
    }

    class ImplicitDiscovery implements DiscoveryObserver {
        @Subscribe
        void deviceEntered(DeviceEnteredMessage message) {
            if (!message.getPayload().getEprAddress().equalsIgnoreCase(consumerUtil.getEpr())) {
                LOG.info("Implicit discovery: EPR mismatch ({})", message.getPayload().getEprAddress());
                return;
            }

            LOG.info("Device with EPR {} entered the network. Try to connect.", consumerUtil.getEpr());
            triggerConnect();
        }
    }
}
