package com.medstorm.sdcbridge;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import org.somda.sdc.glue.consumer.SdcRemoteDevicesConnector;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

public final class DisableConsumerConnectorModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(SdcRemoteDevicesConnector.class)
            .toProvider(new NoopConnectorProvider())
            .in(Scopes.SINGLETON);
    }

    static final class NoopConnectorProvider implements Provider<SdcRemoteDevicesConnector> {
        private final SdcRemoteDevicesConnector instance = createNoop();

        @Override public SdcRemoteDevicesConnector get() { return instance; }

        private static SdcRemoteDevicesConnector createNoop() {
            return (SdcRemoteDevicesConnector) Proxy.newProxyInstance(
                SdcRemoteDevicesConnector.class.getClassLoader(),
                new Class<?>[]{ SdcRemoteDevicesConnector.class },
                (proxy, method, args) -> handle(method, args, proxy)
            );
        }

        private static Object handle(Method method, Object[] args, Object proxy) {
            String name = method.getName();
            Class<?> rt = method.getReturnType();

            // Guava Service-like methods
            if ("startAsync".equals(name) || "stopAsync".equals(name)) return proxy;
            if ("awaitRunning".equals(name) || "awaitTerminated".equals(name)) return null;
            if ("isRunning".equals(name)) return Boolean.FALSE;
            if ("state".equals(name) && rt.isEnum()) {
                Object[] constants = rt.getEnumConstants();
                for (Object c : constants) if ("TERMINATED".equals(String.valueOf(c))) return c;
                return constants.length > 0 ? constants[0] : null;
            }
            if ("addListener".equals(name)) return null;

            if (Optional.class.equals(rt)) return Optional.empty();
            if (Collection.class.isAssignableFrom(rt)) return List.of();
            if (Map.class.isAssignableFrom(rt)) return Map.of();
            if (Set.class.isAssignableFrom(rt)) return Set.of();

            if (rt == boolean.class) return false;
            if (rt == byte.class)    return (byte)0;
            if (rt == short.class)   return (short)0;
            if (rt == int.class)     return 0;
            if (rt == long.class)    return 0L;
            if (rt == float.class)   return 0f;
            if (rt == double.class)  return 0d;
            if (rt == char.class)    return '\0';

            if ("toString".equals(name)) return "NoopSdcRemoteDevicesConnector";
            if ("hashCode".equals(name)) return System.identityHashCode(proxy);
            if ("equals".equals(name))   return proxy == (args != null && args.length > 0 ? args[0] : null);

            return null;
        }
    }
}
