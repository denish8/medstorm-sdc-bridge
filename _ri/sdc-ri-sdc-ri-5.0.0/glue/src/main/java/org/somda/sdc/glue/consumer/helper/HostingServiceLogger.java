package org.somda.sdc.glue.consumer.helper;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.service.HostingServiceProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Logger which adds instance and hosting service specific information to every log message
 * using the {@linkplain CloseableThreadContext}.
 */
public class HostingServiceLogger {

    /**
     * The key which is used in the thread context to communicate the hosting service info.
     */
    public static final String HOSTING_SERVICE_INFO = "hostingServiceInfo";

    /**
     * Gets a logger.
     * <p>
     * Use this function as a replacement for {@link LogManager#getLogger(Class)}.
     *
     * @param logger              logger to wrap
     * @param hostingService      the hosting service information that is added to log messages
     * @param frameworkIdentifier identifier of the current framework instance
     * @return a child logger adapter that contains instance and hosting service information in the log context
     */
    public static Logger getLogger(Logger logger, HostingServiceProxy hostingService, String frameworkIdentifier) {
        return (Logger) Proxy.newProxyInstance(
                InstanceLogger.class.getClassLoader(),
                new Class[]{Logger.class},
                new HostingServiceLoggerLoggerInvocationHandler(
                        logger, generatePrefix(hostingService),
                        frameworkIdentifier
                )
        );
    }

    /**
     * {@linkplain InvocationHandler} which adds instance and hosting service information to all messages.
     */
    private static class HostingServiceLoggerLoggerInvocationHandler implements InvocationHandler {

        private final String instanceId;
        private final Logger logger;
        private final String hostingServiceInfo;

        HostingServiceLoggerLoggerInvocationHandler(Logger logger, String hostingServiceInfo, String instanceId) {
            this.instanceId = instanceId;
            this.hostingServiceInfo = hostingServiceInfo;
            this.logger = logger;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try (var ignored = CloseableThreadContext
                    .put(InstanceLogger.INSTANCE_ID, instanceId)
                    .put(HOSTING_SERVICE_INFO, hostingServiceInfo)
            ) {
                return method.invoke(logger, args);
            }
        }
    }

    private static String generatePrefix(HostingServiceProxy hostingServiceProxy) {
        return hostingServiceProxy.getEndpointReferenceAddress()
                + ','
                + hostingServiceProxy.getActiveXAddr();
    }
}
