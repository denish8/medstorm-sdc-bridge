package org.somda.sdc.glue.consumer.helper;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import test.org.somda.common.LoggingTestWatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(LoggingTestWatcher.class)
class HostingServiceLoggerTest {

    private static final String LOGGER_NAME = "com.example.HostingServiceLoggerTest.some_logger";
    private static Logger logger;
    private static ListAppender appender;

    @BeforeAll
    static void setupLogging() {
        appender = new ListAppender("ListAppender");
        appender.start();
        LoggerContext context = LoggerContext.getContext(false);
        logger = context.getLogger(LOGGER_NAME);
        logger.addAppender(appender);
        logger.setLevel(Level.TRACE);
    }

    @Test
    void testHostingServiceInfoPresent() {
        var instanceId = "instanzio";

        var testMessage = "٠١٢٣٤٥٦٧٨٩";
        var testMessage2 = "表ポあA鷗ŒéＢ逍Üßªąñ丂㐀\uD840\uDC00";

        var mockProxyEpr = "abc";
        var mockProxyXAddr = "bcd";
        var mockProxy = mock(HostingServiceProxy.class);
        when(mockProxy.getEndpointReferenceAddress()).thenReturn(mockProxyEpr);
        when(mockProxy.getActiveXAddr()).thenReturn(mockProxyXAddr);

        var mockProxy2Epr = "zyx";
        var mockProxy2XAddr = "yxw";
        var mockProxy2 = mock(HostingServiceProxy.class);
        when(mockProxy2.getEndpointReferenceAddress()).thenReturn(mockProxy2Epr);
        when(mockProxy2.getActiveXAddr()).thenReturn(mockProxy2XAddr);

        var logger1 = HostingServiceLogger.getLogger(logger, mockProxy, instanceId);
        var logger2 = HostingServiceLogger.getLogger(logger, mockProxy2, instanceId);

        logger1.error(testMessage);
        logger2.error(testMessage2);

        var events = appender.getEvents();
        assertEquals(2, events.size());

        {
            var event = events.get(0);
            assertEquals(event.getMessage().getFormattedMessage(), testMessage);
            assertEquals(instanceId, event.getContextData().getValue(InstanceLogger.INSTANCE_ID));
            assertEquals(
                    mockProxyEpr + "," + mockProxyXAddr,
                    event.getContextData().getValue(HostingServiceLogger.HOSTING_SERVICE_INFO)
            );
        }
        {
            var event = events.get(1);
            assertEquals(event.getMessage().getFormattedMessage(), testMessage2);
            assertEquals(instanceId, event.getContextData().getValue(InstanceLogger.INSTANCE_ID));
            assertEquals(
                    mockProxy2Epr + "," + mockProxy2XAddr,
                    event.getContextData().getValue(HostingServiceLogger.HOSTING_SERVICE_INFO)
            );
        }
    }

}
