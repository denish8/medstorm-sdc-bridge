package org.somda.sdc.common.util;

import com.google.common.util.concurrent.AbstractIdleService;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.model.ObjectStringifier;
import org.somda.sdc.common.model.Stringified;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Wraps an {@linkplain ExecutorService} into a guava {@linkplain AbstractIdleService}.
 * <p>
 * Wrapping {@linkplain ExecutorService}s into guava services allows orchestrating thread pool instances, especially
 * shutting them down properly when shutting down a parent service instance.
 *
 * @param <T> actual type of the {@linkplain ExecutorService}.
 */
public class ExecutorWrapperService<T extends ExecutorService> extends AbstractIdleService {
    private static final Logger LOG = LogManager.getLogger(ExecutorWrapperService.class);
    private static final long STOP_TIMEOUT = 5;
    private static final TimeUnit STOP_TIMEUNIT = TimeUnit.SECONDS;

    private final Callable<T> serviceCreator;
    @Stringified
    private final String serviceName;
    private final Logger instanceLogger;
    @Stringified
    private T executorService;

    /**
     * Creates a wrapper around an {@linkplain ExecutorService}.
     *
     * @param serviceCreator {@linkplain Callable} which returns an {@linkplain ExecutorService}.
     * @param serviceName name for the service, used in logging.
     * @param frameworkIdentifier identifier used for logging
     */
    public ExecutorWrapperService(Callable<T> serviceCreator, String serviceName, String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.serviceCreator = serviceCreator;
        this.serviceName = serviceName;
    }

    @Override
    protected void startUp() throws Exception {
        instanceLogger.debug("[{}] Starting executor service wrapper", serviceName);
        this.executorService = serviceCreator.call();
    }

    @Override
    protected void shutDown() throws Exception {
        instanceLogger.info("[{}] Stopping executor service wrapper", serviceName);
        if (this.executorService != null) {
            executorService.shutdown();
            try {
                executorService.awaitTermination(STOP_TIMEOUT, STOP_TIMEUNIT);
            } catch (InterruptedException e) {
                instanceLogger.error("[{}] Could not stop all threads!", serviceName);
                throw e;
            }
        }
    }

    /**
     * Gets the {@linkplain ExecutorService} instance when service is running.
     * <p>
     * <em>Only ever access this once the service has been started!</em>
     *
     * @return wrapped {@linkplain ExecutorService} instance.
     * @throws RuntimeException when the service isn't running yet.
     */
    public T get() {
        if (isRunning()) {
            return executorService;
        } else {
            instanceLogger.error("[{}] get was called on a service which was not running", serviceName);
            throw new RuntimeException(String.format(
                    "ExecutorWrapperService.get() called on %s service which was not running",
                    serviceName
            ));
        }
    }

    @Override
    public String toString() {
        return ObjectStringifier.stringify(this) + " [" + state() + "]";
    }
}
