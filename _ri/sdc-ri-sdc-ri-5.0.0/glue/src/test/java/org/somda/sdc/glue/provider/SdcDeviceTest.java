package org.somda.sdc.glue.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.device.DeviceSettings;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.glue.UnitTestUtil;
import org.somda.sdc.glue.provider.factory.SdcDeviceFactory;
import org.somda.sdc.glue.provider.sco.OperationInvocationReceiver;
import test.org.somda.common.LoggingTestWatcher;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(LoggingTestWatcher.class)
class SdcDeviceTest {
    private SdcDevice sdcDevice;
    private SdcDevicePlugin plugin;
    private DpwsFramework dpwsFramework;

    @BeforeEach
    void beforeEach() {
        final LocalMdibAccess mdibAccess = mock(LocalMdibAccess.class);
        final List<OperationInvocationReceiver> operationInvocationReceivers = Collections.singletonList(mock(OperationInvocationReceiver.class));
        plugin = mock(SdcDevicePlugin.class);

        final var injector = new UnitTestUtil().getInjector();
        sdcDevice = injector.getInstance(SdcDeviceFactory.class).createSdcDevice(
                new DeviceSettings() {
                    @Override
                    public EndpointReferenceType getEndpointReference() {
                        return injector.getInstance(WsAddressingUtil.class)
                                .createEprWithAddress(injector.getInstance(SoapUtil.class).createRandomUuidUri());
                    }

                    @Override
                    public NetworkInterface getNetworkInterface() {
                        try {
                            return NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, mdibAccess, operationInvocationReceivers, Collections.singletonList(plugin));

        dpwsFramework = injector.getInstance(DpwsFramework.class);
        dpwsFramework.startAsync().awaitRunning();
    }

    @Test
    void pluginBehaviorNoException() throws Exception {
        sdcDevice.startAsync().awaitRunning();
        verify(plugin, times(1)).beforeStartUp(any());
        verify(plugin, times(1)).afterStartUp(any());

        assertTrue(sdcDevice.isRunning());

        sdcDevice.stopAsync().awaitTerminated();
        verify(plugin, times(1)).beforeShutDown(any());
        verify(plugin, times(1)).afterShutDown(any());

        assertFalse(sdcDevice.isRunning());
    }

    @Test
    void pluginExceptionBeforeStartUp() throws Exception {
        doThrow(new Exception()).when(plugin).beforeStartUp(any());

        assertThrows(IllegalStateException.class, () -> sdcDevice.startAsync().awaitRunning());

        verify(plugin, times(1)).beforeStartUp(any());
        verify(plugin, times(0)).afterStartUp(any());
        verify(plugin, times(0)).beforeShutDown(any());
        verify(plugin, times(0)).afterShutDown(any());

        assertFalse(sdcDevice.isRunning());
    }

    @Test
    void pluginExceptionAfterStartUp() throws Exception {
        doThrow(new Exception()).when(plugin).afterStartUp(any());

        assertThrows(IllegalStateException.class, () -> sdcDevice.startAsync().awaitRunning());

        verify(plugin, times(1)).beforeStartUp(any());
        verify(plugin, times(1)).afterStartUp(any());
        verify(plugin, times(0)).beforeShutDown(any());
        verify(plugin, times(0)).afterShutDown(any());

        assertFalse(sdcDevice.isRunning());
    }

    @Test
    void pluginExceptionBeforeShutDown() throws Exception {
        doThrow(new Exception()).when(plugin).beforeShutDown(any());

        sdcDevice.startAsync().awaitRunning();
        verify(plugin, times(1)).beforeStartUp(any());
        verify(plugin, times(1)).afterStartUp(any());

        assertTrue(sdcDevice.isRunning());

        sdcDevice.stopAsync().awaitTerminated();
        verify(plugin, times(1)).beforeShutDown(any());
        verify(plugin, times(1)).afterShutDown(any());

        assertFalse(sdcDevice.isRunning());
    }

    @Test
    void pluginExceptionAfterShutDown() throws Exception {
        doThrow(new Exception()).when(plugin).afterShutDown(any());

        sdcDevice.startAsync().awaitRunning();
        verify(plugin, times(1)).beforeStartUp(any());
        verify(plugin, times(1)).afterStartUp(any());

        assertTrue(sdcDevice.isRunning());

        sdcDevice.stopAsync().awaitTerminated();
        verify(plugin, times(1)).beforeShutDown(any());
        verify(plugin, times(1)).afterShutDown(any());

        assertFalse(sdcDevice.isRunning());
    }
}