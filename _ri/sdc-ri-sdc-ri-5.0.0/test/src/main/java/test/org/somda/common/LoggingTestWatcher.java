package test.org.somda.common;

import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class LoggingTestWatcher implements TestWatcher {

    static {
        TestLogging.configure();
    }

    private static final Logger LOG = LogManager.getLogger(LoggingTestWatcher.class);

    @Override
    public void testAborted(ExtensionContext extensionContext, Throwable throwable) {
        LOG.info("");
        LOG.info(
                "Test {}.{} has been aborted",
                extensionContext.getRequiredTestClass().getSimpleName(),
                extensionContext.getRequiredTestMethod().getName()
        );
        LOG.info("");
    }

    @Override
    public void testDisabled(ExtensionContext extensionContext, Optional<String> optional) {
        LOG.info("");
        LOG.info(
                "Test {}.{} was disabled",
                extensionContext.getRequiredTestClass().getSimpleName(),
                extensionContext.getRequiredTestMethod().getName()
        );
        LOG.info("");
    }

    @Override
    public void testFailed(ExtensionContext extensionContext, Throwable throwable) {
        LOG.info("");
        LOG.info(
                "Test {}.{} has failed",
                extensionContext.getRequiredTestClass().getSimpleName(),
                extensionContext.getRequiredTestMethod().getName()
        );
        LOG.info("");
    }

    @Override
    public void testSuccessful(ExtensionContext extensionContext) {
        LOG.info("");
        LOG.info(
                "Test {}.{} has been successful",
                extensionContext.getRequiredTestClass().getSimpleName(),
                extensionContext.getRequiredTestMethod().getName()
        );
        LOG.info("");
    }
}