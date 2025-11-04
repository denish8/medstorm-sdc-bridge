package org.somda.sdc.glue.provider.helper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.glue.provider.SdcDeviceContext;
import org.somda.sdc.glue.provider.SdcDevicePlugin;
import test.org.somda.common.LoggingTestWatcher;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(LoggingTestWatcher.class)
class SdcDevicePluginProcessorTest {

    private List<SdcDevicePlugin> plugins;
    private SdcDeviceContext context;
    private SdcDevicePluginProcessor pluginProcessor;

    @BeforeEach
    void beforeEach() {
        plugins = Arrays.asList(
                mock(SdcDevicePlugin.class),
                mock(SdcDevicePlugin.class),
                mock(SdcDevicePlugin.class)
        );

        context = mock(SdcDeviceContext.class);

        pluginProcessor = new SdcDevicePluginProcessor(plugins, context);
    }

    @Test
    void noException() throws Exception {
        callPlugins(true, true);
        verifyInteraction(1, 1);
    }

    @Test
    void exceptionBeforeStartUp() throws Exception {
        doThrow(new Exception()).when(plugins.get(1)).beforeStartUp(context);
        callPlugins(false, true);

        verifyInteraction(0, 1);
    }

    @Test
    void exceptionAfterStartUp() throws Exception {
        doThrow(new Exception()).when(plugins.get(1)).afterStartUp(context);
        callPlugins(true, false);

        verifyInteraction(1, 0);
    }

    @Test
    void exceptionBeforeShutDown() throws Exception {
        doThrow(new Exception()).when(plugins.get(1)).beforeShutDown(context);
        callPlugins(true, true);

        verifyInteraction(1, 1);
    }

    @Test
    void exceptionAfterShutDown() throws Exception {
        doThrow(new Exception()).when(plugins.get(1)).beforeShutDown(context);
        callPlugins(true, true);

        verifyInteraction(1, 1);
    }

    private void callPlugins(boolean noExceptionBeforeStartUpResult, boolean noExceptionAfterStartUpResult) {
        if (noExceptionBeforeStartUpResult) {
            assertDoesNotThrow(() -> pluginProcessor.beforeStartUp());
        } else {
            assertThrows(Exception.class, () -> pluginProcessor.beforeStartUp());
        }
        if (noExceptionAfterStartUpResult) {
            assertDoesNotThrow(() -> pluginProcessor.afterStartUp());
        } else {
            assertThrows(Exception.class, () -> pluginProcessor.afterStartUp());
        }

        pluginProcessor.beforeShutDown();
        pluginProcessor.afterShutDown();
    }

    private void verifyInteraction(int interactionBeforeStartUp, int interactionAfterStartUp) throws Exception {
        for (int i = 0; i < plugins.size() - 1; ++i) {
            verify(plugins.get(i), times(1)).beforeStartUp(context);
            verify(plugins.get(i), times(1)).afterStartUp(context);
            verify(plugins.get(i), times(1)).beforeShutDown(context);
            verify(plugins.get(i), times(1)).afterShutDown(context);
        }

        verify(plugins.get(2), times(interactionBeforeStartUp)).beforeStartUp(context);
        verify(plugins.get(2), times(interactionAfterStartUp)).afterStartUp(context);
        verify(plugins.get(2), times(1)).beforeShutDown(context);
        verify(plugins.get(2), times(1)).afterShutDown(context);
    }
}