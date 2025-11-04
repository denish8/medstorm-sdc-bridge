package org.somda.sdc.common.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import test.org.somda.common.LoggingTestWatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(LoggingTestWatcher.class)
class InstanceLoggerTest {

    private static final String LOGGER_NAME = "com.example.some_logger";
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

    @AfterAll
    static void teardownLogging() {
        logger.removeAppender(appender);
    }

    @BeforeEach
    void setUp() {
        appender.clear();
    }

    @Test
    void testLoggerPassthrough() {
        var testMessage = "Ω≈ç√∫˜µ≤≥÷";
        var logger = spy(InstanceLoggerTest.logger);
        var marker = "CustomInstanceMarker";
        var captor = ArgumentCaptor.forClass(String.class);


        var proxiedLogger = InstanceLogger.wrapLogger(logger, marker);
        proxiedLogger.error(testMessage);

        verify(logger).error(captor.capture());

        assertEquals(1, captor.getAllValues().size());
        assertEquals(testMessage, captor.getValue());
    }

    @Test
    void testLoggerContext() {
        var testMessage = "åß∂ƒ©˙∆˚¬…æ";
        var marker = "CustomInstanceMarker";

        var proxiedLogger = InstanceLogger.wrapLogger(logger, marker);
        proxiedLogger.error(testMessage);

        var events = appender.getEvents();
        assertEquals(1, events.size());

        var event = events.get(0);
        assertEquals(marker, event.getContextData().getValue(InstanceLogger.INSTANCE_ID));
    }

    @Test
    void testLoggerContextMultipleLoggers() {
        var testMessage = "œ∑´®†¥¨ˆøπ“‘";
        var testMessage2 = "¡™£¢∞§¶•ªº–≠";
        var marker = "CustomInstanceMarker";
        var marker2 = "⅛⅜⅝⅞";

        {
            var proxiedLogger = InstanceLogger.wrapLogger(logger, marker);
            proxiedLogger.error(testMessage);
        }
        {
            var proxiedLogger2 = InstanceLogger.wrapLogger(logger, marker2);
            proxiedLogger2.error(testMessage2);
        }

        var events = appender.getEvents();
        assertEquals(2, events.size());

        {
            var event = events.get(0);
            assertEquals(marker, event.getContextData().getValue(InstanceLogger.INSTANCE_ID));
        }
        {
            var event = events.get(1);
            assertEquals(marker2, event.getContextData().getValue(InstanceLogger.INSTANCE_ID));
        }
    }

}
