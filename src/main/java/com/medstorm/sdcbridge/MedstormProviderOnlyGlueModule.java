package com.medstorm.sdcbridge;

import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.Provider;
import org.somda.sdc.glue.consumer.SdcRemoteDevicesConnector;

import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Executor;

/**
 * Provider-only Glue override:
 * - Replaces the consumer connector graph with a NO-OP dynamic proxy.
 * - Does NOT touch BICEPS factories (e.g., RemoteMdibAccessFactory) to avoid binding collisions.
 */
public final class MedstormProviderOnlyGlueModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(SdcRemoteDevicesConnector.class)
            .toProvider(new NoopConnectorProvider())
            .in(Scopes.SINGLETON);

        // IMPORTANT: Do NOT bind RemoteMdibAccessFactory or other BICEPS factories here.
        // DefaultBicepsModule already binds them; rebinding causes BindingAlreadySet errors.
    }

    /** Supplies a single no-op proxy instance of SdcRemoteDevicesConnector. */
    static final class NoopConnectorProvider implements Provider<SdcRemoteDevicesConnector> {
        private final SdcRemoteDevicesConnector instance = createNoopConnector();

        @Override
        public SdcRemoteDevicesConnector get() {
            return instance;
        }

        @SuppressWarnings("unchecked")
        private static SdcRemoteDevicesConnector createNoopConnector() {
            final ClassLoader cl = SdcRemoteDevicesConnector.class.getClassLoader();
            final Class<?>[] ifaces = new Class<?>[] { SdcRemoteDevicesConnector.class };

            return (SdcRemoteDevicesConnector) Proxy.newProxyInstance(
                cl, ifaces,
                (proxy, method, args) -> handleInvocation(proxy, method, args)
            );
        }

        private static Object handleInvocation(Object proxy, Method method, Object[] args) {
            final String name = method.getName();
            final Class<?> ret = method.getReturnType();

            // --- Lifecycle / Service methods: be a "started" idle service that never does work.
            if ("startAsync".equals(name) || "stopAsync".equals(name)) {
                return proxy; // Guava Service returns Service (fluently); proxy implements it via the extended interface
            }
            if ("awaitRunning".equals(name) || "awaitTerminated".equals(name)) {
                return null; // no-op
            }
            if ("isRunning".equals(name)) {
                return Boolean.FALSE;
            }
            if ("state".equals(name) && ret.isEnum()) {
                // com.google.common.util.concurrent.Service.State
                Object[] constants = ret.getEnumConstants();
                // Prefer TERMINATED if present; otherwise use first constant as safe default
                for (Object c : constants) {
                    if (String.valueOf(c).equals("TERMINATED")) return c;
                }
                return constants.length > 0 ? constants[0] : null;
            }
            if ("addListener".equals(name) && method.getParameterCount() == 2) {
                // addListener(Service.Listener, Executor)
                return null;
            }

            // --- Object basics
            if ("toString".equals(name)) {
                return "NoopSdcRemoteDevicesConnector";
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }

            // --- Common collections & optionals: return empties
            if (Optional.class.equals(ret)) return Optional.empty();
            if (Collection.class.isAssignableFrom(ret)) return Collections.emptyList();
            if (Set.class.equals(ret)) return Collections.emptySet();
            if (Map.class.isAssignableFrom(ret)) return Collections.emptyMap();

            // --- Primitive defaults
            if (ret.equals(boolean.class)) return false;
            if (ret.equals(byte.class))    return (byte)0;
            if (ret.equals(short.class))   return (short)0;
            if (ret.equals(int.class))     return 0;
            if (ret.equals(long.class))    return 0L;
            if (ret.equals(float.class))   return 0f;
            if (ret.equals(double.class))  return 0d;
            if (ret.equals(char.class))    return '\0';

            // --- For any other reference type, return null (no-op)
            return null;
        }
    }
}
