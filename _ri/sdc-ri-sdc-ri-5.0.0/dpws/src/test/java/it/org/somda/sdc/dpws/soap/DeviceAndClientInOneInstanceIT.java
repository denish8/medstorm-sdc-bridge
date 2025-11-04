package it.org.somda.sdc.dpws.soap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import test.org.somda.common.LoggingTestWatcher;

@ExtendWith(LoggingTestWatcher.class)
class DeviceAndClientInOneInstanceIT {
    // private final IntegrationTestUtil IT = new IntegrationTestUtil();
    private String eprAddress1;
    private String eprAddress2;
    private DeviceAndClientPeer peer1;
    private DeviceAndClientPeer peer2;

    @BeforeEach
    void beforeEach() {
        eprAddress1 = "urn:uuid:11111111-1111-1111-1111-111111111111";
        eprAddress2 = "urn:uuid:22222222-2222-2222-2222-222222222222";
        peer1 = new DeviceAndClientPeer(eprAddress1);
        peer2 = new DeviceAndClientPeer(eprAddress2);
    }

    @Test
    void crossConnectWithRequestResponseAndNotification() throws Exception {
        peer1.startAsync().awaitRunning();
        peer2.startAsync().awaitRunning();

        peer1.startInteraction(eprAddress2);
        peer2.startInteraction(eprAddress1);

        peer1.stopAsync().awaitTerminated();
        peer2.stopAsync().awaitTerminated();
    }
}
