package org.somda.sdc.common.logging;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Logger which adds instance specific information to every log message using the {@linkplain CloseableThreadContext}.
 */
public class InstanceLogger {

    /**
     * The key which is used in the thread context to communicate the instance identifier.
     *
     * @see org.somda.sdc.common.CommonConfig#INSTANCE_IDENTIFIER
     */
    public static final String INSTANCE_ID = "instanceId";

    /**
     * Wraps a logger into a proxy which adds context information to all messages.
     *
     * @param logger     to wrap
     * @param instanceId to add to all messages
     * @return wrapped logger instance
     */
    public static Logger wrapLogger(Logger logger, String instanceId) {
        return (Logger) Proxy.newProxyInstance(
                InstanceLogger.class.getClassLoader(),
                new Class[]{Logger.class},
                new InstanceLoggerInvocationHandler(logger, instanceId)
        );
    }

    /**
     * {@linkplain InvocationHandler} which adds instance information to all messages.
     */
    private static class InstanceLoggerInvocationHandler implements InvocationHandler {

        private final String instanceId;
        private final Logger logger;

        InstanceLoggerInvocationHandler(Logger logger, String instanceId) {
            this.instanceId = instanceId;
            this.logger = logger;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try (var ignored = CloseableThreadContext.put(INSTANCE_ID, instanceId)) {
                return method.invoke(logger, args);
            }
        }
    }
}
