// package com.medstorm.sdcbridge;

// import com.google.inject.CreationException;
// import com.google.inject.Guice;
// import com.google.inject.Injector;
// import com.google.inject.Module;
// import jakarta.annotation.PostConstruct;
// import jakarta.annotation.PreDestroy;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.stereotype.Component;
// import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
// import org.somda.sdc.biceps.provider.access.factory.LocalMdibAccessFactory;
// import org.somda.sdc.dpws.DpwsFramework;
// import org.somda.sdc.glue.provider.SdcDevice;
// import org.somda.sdc.glue.provider.factory.SdcDeviceFactory;

// // add at top with other imports
// import com.google.inject.util.Modules;
// import org.somda.sdc.dpws.guice.DefaultDpwsModule;

// import com.google.inject.Module;
// import com.google.inject.Guice;
// import com.google.inject.Injector;


// import org.somda.sdc.common.guice.DefaultCommonModule;
// import org.somda.sdc.dpws.guice.DefaultDpwsModule;
// import org.somda.sdc.glue.guice.DefaultGlueModule;





// import java.lang.reflect.InvocationTargetException;
// import java.lang.reflect.Method;
// import java.lang.reflect.Proxy;
// import java.net.Inet4Address;
// import java.net.InetAddress;
// import java.net.NetworkInterface;
// import java.util.*;

// @Component
// public class SDCStackProvider {
//     private static final Logger log = LoggerFactory.getLogger(SDCStackProvider.class);

//     private static final String DEVICE_SETTINGS_CLASS = "org.somda.sdc.dpws.device.DeviceSettings";
//     private static final String OIR_CLASS = "org.somda.sdc.glue.provider.sco.OperationInvocationReceiver";
//     private static final String LOCALIZATION_STORAGE_CLASS =
//             "org.somda.sdc.glue.provider.localization.LocalizationStorage";

//     private Injector injector;
//     private DpwsFramework dpws;
//     private SdcDevice device;

//     // Remember the NIC we actually bind to so DeviceSettings can expose it if asked.
//     private NetworkInterface selectedNic;

//     @PostConstruct
//     public void start() {
//         final String nicHint = System.getProperty("sdc.nic", "").trim();
//         final String epr = System.getProperty("sdc.epr", "urn:uuid:medstorm-sensor-1");

//         // Ensure we always use a stable HTTP port unless overridden by -D at runtime
//         System.setProperty("Dpws.HttpServerPort",
//                 System.getProperty("Dpws.HttpServerPort", "53200"));

//         final boolean hasGlueProvider = isPresent("org.somda.sdc.glue.provider.factory.SdcDeviceFactory");
//         final boolean hasBiceps = isPresent("org.somda.sdc.biceps.provider.access.LocalMdibAccess");
//         log.info("SDCStackProvider: classpath -> glueProvider={}, biceps={}", hasGlueProvider, hasBiceps);

//         // Diagnostics
//         debugDumpNicProps();

//         // 1) Build injector
//         // 1) Build injector
// try {
//     // Keep Jetty HTTP server override for port/NIC pinning
//     Module dpwsWithJettyOverride =
//         com.google.inject.util.Modules.override(new org.somda.sdc.dpws.guice.DefaultDpwsModule())
//             .with(new com.medstorm.sdcbridge.MedstormJettyFactoryOverride());

//     // Provider-only override for Glue: neuters consumer graph (e.g., RemoteMdibAccessFactory)
//     Module glueProviderOnlyOverride = null;
//     if (hasGlueProvider) {
//         glueProviderOnlyOverride =
//             com.google.inject.util.Modules.override(new org.somda.sdc.glue.guice.DefaultGlueModule())
//                 .with(new com.medstorm.sdcbridge.MedstormProviderOnlyGlueModule());
//     }

//     java.util.List<com.google.inject.Module> modules = new java.util.ArrayList<>();
//     modules.add(new org.somda.sdc.common.guice.DefaultCommonModule());
//     modules.add(dpwsWithJettyOverride);
//     if (hasBiceps) {
//         modules.add(new org.somda.sdc.biceps.guice.DefaultBicepsModule());
//     }
//     if (glueProviderOnlyOverride != null) {
//         modules.add(glueProviderOnlyOverride);
//     }
//     modules.add(new com.medstorm.sdcbridge.MedstormSdcriConfigModule());

//     // IMPORTANT: assign to the field, not a local var, because it's used below.
//     this.injector = com.google.inject.Guice.createInjector(modules);

//     log.info("SDCStackProvider: Guice injector created with {} module(s).", modules.size());
// } catch (com.google.inject.CreationException ce) {
//     log.error("Failed to start SDC stack (Guice creation failed)", ce);
//     return;
// } catch (Exception ex) {
//     log.error("Failed to start SDC stack (unexpected during Guice creation)", ex);
//     return;
// }



//         // 2) Start DPWS and explicitly set NIC
//         try {
//             dpws = injector.getInstance(DpwsFramework.class);

//             NetworkInterface ni = null;
//             if (!nicHint.isBlank()) {
//                 ni = resolveNic(nicHint);
//                 if (ni == null) {
//                     log.error("NIC hint '{}' could not be resolved; not starting DPWS to avoid [name:null] NPE.", nicHint);
//                     return;
//                 }
//                 selectedNic = ni;
//                 dpws.setNetworkInterface(ni);
//                 log.info("DPWS will bind to {} ({})", ni.getName(), ni.getDisplayName());
//             } else {
//                 log.warn("No sdc.nic provided; DPWS will pick the default NIC.");
//                 selectedNic = null;
//             }

//             dpws.startAsync().awaitRunning();
//             log.info("DPWS framework is RUNNING.");
//         } catch (Exception ex) {
//             log.error("Could not start DPWS framework", ex);
//             return;
//         }

//         // 3) Discovery without local device is fine, but we want a provider if possible
//         if (!(hasGlueProvider && hasBiceps)) {
//             log.warn("GLUE or BICEPS not on classpath -> will not create local SDC device. Discovery still runs.");
//             return;
//         }

//         // 4) MDIB
//         final LocalMdibAccess localMdib;
//         try {
//             LocalMdibAccessFactory mdibFactory = injector.getInstance(LocalMdibAccessFactory.class);
//             localMdib = mdibFactory.createLocalMdibAccess();
//         } catch (Exception ex) {
//             log.error("Could not create LocalMdibAccess", ex);
//             return;
//         }

//         // 5) Factory, DeviceSettings (best-effort), and OIR
//         final SdcDeviceFactory factory;
//         try {
//             factory = injector.getInstance(SdcDeviceFactory.class);
//         } catch (Exception ex) {
//             log.error("Could not get SdcDeviceFactory from Guice", ex);
//             return;
//         }

//         Object devSettings = null;
//         try {
//             Class<?> dsClz = Class.forName(DEVICE_SETTINGS_CLASS);
//             try {
//                 devSettings = injector.getInstance(dsClz);
//             } catch (Throwable t) {
//                 log.warn("DeviceSettings not available from injector; using NO-OP fallback. ({})", t.toString());
//                 devSettings = createNoopDeviceSettings(dsClz, epr, selectedNic);
//             }
//         } catch (Throwable t) {
//             log.warn("DeviceSettings class not found; proceeding WITHOUT it. ({})", t.toString());
//         }

//         Object oir;
//         try {
//             Class<?> oirIface = Class.forName(OIR_CLASS);
//             try {
//                 oir = injector.getInstance(oirIface);
//             } catch (Throwable t) {
//                 log.warn("No Guice binding for OperationInvocationReceiver; using NO-OP fallback. ({})", t.toString());
//                 oir = createNoopOir(oirIface);
//             }
//         } catch (Throwable t) {
//             log.warn("OIR class not found; creating NO-OP OIR. ({})", t.toString());
//             oir = createNoopOir(null);
//         }

//         // 6) Create + start device
//         try {
//             device = createDeviceCompat(factory, localMdib, devSettings, oir);
//             if (device != null) {
//                 device.startAsync().awaitRunning();
//                 log.info("✅ SDC device started, EPR={}", epr);
//             } else {
//                 log.warn("SDC device not created (no matching factory method).");
//             }
//         } catch (Exception ex) {
//             log.error("Failed to create/start SDC device", ex);
//         }
//     }

//     @PreDestroy
//     public void stop() {
//         try { if (device != null) device.stopAsync().awaitTerminated(); } catch (Exception ignore) {}
//         try { if (dpws != null) dpws.stopAsync().awaitTerminated(); } catch (Exception ignore) {}
//         log.info("SDC stack stopped.");
//     }

//     // ---------------------------------------------------------------------
//     // Helpers
//     // ---------------------------------------------------------------------

//     private SdcDevice unwrapToSdcDevice(Object ret, Class<?> sdcDeviceType) throws Exception {
//         if (ret == null) return null;
//         if (sdcDeviceType.isInstance(ret)) return (SdcDevice) ret;

//         // javax.inject.Provider / com.google.inject.Provider
//         for (String provName : new String[]{"javax.inject.Provider", "com.google.inject.Provider"}) {
//             try {
//                 Class<?> provClz = Class.forName(provName);
//                 if (provClz.isInstance(ret)) {
//                     java.lang.reflect.Method get = provClz.getMethod("get");
//                     Object v = get.invoke(ret);
//                     if (sdcDeviceType.isInstance(v)) return (SdcDevice) v;
//                 }
//             } catch (Throwable ignore) {}
//         }
//         return null;
//     }

//     private void logFactorySignatureHints(Object factory) {
//         try {
//             StringBuilder sb = new StringBuilder("Available factory methods on ")
//                     .append(factory.getClass()).append(":\n");
//             for (java.lang.reflect.Method m : factory.getClass().getMethods()) {
//                 sb.append("  ").append(sig(m)).append("\n");
//             }
//             log.error(sb.toString());
//         } catch (Throwable ignore) {}
//     }

//     private Object createNoopLocalizationStorage(Class<?> lsClz) {
//         try {
//             return Proxy.newProxyInstance(
//                     lsClz.getClassLoader(),
//                     new Class[]{lsClz},
//                     (proxy, method, args) -> {
//                         Class<?> rt = method.getReturnType();
//                         if (Optional.class.isAssignableFrom(rt)) return Optional.empty();
//                         if (Collection.class.isAssignableFrom(rt)) return List.of();
//                         if (Map.class.isAssignableFrom(rt)) return Map.of();
//                         if (rt == boolean.class || rt == Boolean.class) return false;
//                         if (rt == int.class || rt == Integer.class) return 0;
//                         if (rt == long.class || rt == Long.class) return 0L;
//                         if (rt == double.class || rt == Double.class) return 0.0d;
//                         if (rt == float.class || rt == Float.class) return 0.0f;
//                         if (rt == short.class || rt == Short.class) return (short) 0;
//                         if (rt == byte.class || rt == Byte.class) return (byte) 0;
//                         if (rt == char.class || rt == Character.class) return '\0';
//                         if (rt == String.class) return "";
//                         if ("java.time.Duration".equals(rt.getName())) return java.time.Duration.ZERO;
//                         if (rt.isEnum()) {
//                             Object[] consts = rt.getEnumConstants();
//                             return (consts != null && consts.length > 0) ? consts[0] : null;
//                         }
//                         return null;
//                     }
//             );
//         } catch (Throwable t) {
//             log.warn("Could not construct NO-OP LocalizationStorage: {}", t.toString());
//             return null;
//         }
//     }

//     private Object buildWsAddressingEpr(String epr) {
//         try {
//             Class<?> eprClz = Class.forName("org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType");
//             Class<?> attrUriClz = Class.forName("org.somda.sdc.dpws.soap.wsaddressing.model.AttributedURIType");

//             Object eprObj = eprClz.getConstructor().newInstance();
//             Object addr   = attrUriClz.getConstructor().newInstance();

//             attrUriClz.getMethod("setValue", String.class).invoke(addr, epr);
//             eprClz.getMethod("setAddress", attrUriClz).invoke(eprObj, addr);
//             return eprObj;
//         } catch (Throwable t) {
//             log.warn("Could not construct WS-Addressing EPR, returning null: {}", t.toString());
//             return null;
//         }
//     }

//     private static Object toJavaUri(String epr) {
//         try { return java.net.URI.create(epr); } catch (Throwable ignore) { return null; }
//     }

//     private static boolean isPresent(String cn) {
//         try { Class.forName(cn); return true; } catch (Throwable t) { return false; }
//     }

//     @SuppressWarnings("unchecked")
//     private SdcDevice createDeviceCompat(
//             SdcDeviceFactory factory,
//             LocalMdibAccess mdib,
//             Object devSettings,
//             Object oir) throws Exception {

//         final Class<?> sdcDeviceType = org.somda.sdc.glue.provider.SdcDevice.class;
//         final Class<?> mdibType      = org.somda.sdc.biceps.provider.access.LocalMdibAccess.class;
//         final Class<?> oirType       = Class.forName("org.somda.sdc.glue.provider.sco.OperationInvocationReceiver");

//         // Gather candidates
//         final List<Method> candidates = new ArrayList<>();
//         for (Class<?> itf : factory.getClass().getInterfaces()) {
//             for (Method m : itf.getMethods()) {
//                 if (returnsSdcDeviceOrProvider(m, sdcDeviceType)) candidates.add(m);
//             }
//         }
//         for (Method m : factory.getClass().getMethods()) {
//             if (returnsSdcDeviceOrProvider(m, sdcDeviceType)) candidates.add(m);
//         }

//         // Prefer create*, then build*, then get*, longer params first
//         candidates.sort((a, b) -> Integer.compare(methodScore(b), methodScore(a)));

//         List<String> tried = new ArrayList<>();
//         for (Method m : candidates) {
//             final Class<?>[] p = m.getParameterTypes();
//             final Object[] args = new Object[p.length];
//             boolean ok = true;

//             for (int i = 0; i < p.length; i++) {
//                 Class<?> t = p[i];

//                 if (t.isAssignableFrom(mdibType)) {
//                     if (mdib == null) { ok = false; break; }
//                     args[i] = mdib;

//                 } else if (t.isAssignableFrom(oirType)) {
//                     if (oir == null) { ok = false; break; }
//                     args[i] = oir;

//                 } else if (Collection.class.isAssignableFrom(t)) {
//                     args[i] = List.of();

//                 } else if (t.getName().equals(LOCALIZATION_STORAGE_CLASS)
//                         || t.getSimpleName().equals("LocalizationStorage")) {
//                     args[i] = createNoopLocalizationStorage(t);

//                 } else if (devSettings != null &&
//                         (t.isInstance(devSettings) || t.getName().endsWith("DeviceSettings"))) {
//                     args[i] = devSettings;

//                 } else if (List.class.isAssignableFrom(t)) {
//                     args[i] = List.of();

//                 } else {
//                     ok = false;
//                     break;
//                 }
//             }

//             if (!ok) { tried.add(sig(m)); continue; }

//             try {
//                 Object ret = m.invoke(factory, args);
//                 SdcDevice dev = unwrapToSdcDevice(ret, sdcDeviceType);
//                 if (dev != null) {
//                     log.info("Using SDC factory method: {}", sig(m));
//                     return dev;
//                 }
//             } catch (InvocationTargetException ite) {
//                 Throwable cause = ite.getTargetException();
//                 log.warn("Factory method {} threw {}: {} (will try next candidate)",
//                         sig(m), cause.getClass().getSimpleName(), cause.getMessage());
//                 tried.add(sig(m) + " -> " + cause.getClass().getSimpleName());
//             } catch (Throwable t) {
//                 tried.add(sig(m) + " -> " + t.getClass().getSimpleName());
//             }
//         }

//         log.error("No usable SDC device factory method. Tried:\n  {}", String.join("\n  ", tried));
//         logFactorySignatureHints(factory);
//         throw new IllegalStateException("Could not obtain SdcDevice from factory " + factory.getClass());
//     }

//     private boolean returnsSdcDeviceOrProvider(Method m, Class<?> sdcDeviceType) {
//         Class<?> rt = m.getReturnType();
//         if (sdcDeviceType.isAssignableFrom(rt)) return true;
//         String rn = rt.getName();
//         return rn.equals("javax.inject.Provider") || rn.equals("com.google.inject.Provider");
//     }

//     private int methodScore(Method m) {
//         String n = m.getName().toLowerCase(Locale.ROOT);
//         int score = 0;
//         if (n.startsWith("create")) score += 100;
//         if (n.startsWith("build"))  score += 50;
//         if (n.startsWith("get"))    score += 25;
//         score += m.getParameterCount();
//         if (m.getDeclaringClass().getName().startsWith("org.somda.sdc")) score += 5;
//         return score;
//     }

//     private String sig(Method m) {
//         StringBuilder sb = new StringBuilder();
//         sb.append(m.getDeclaringClass().getSimpleName()).append(".").append(m.getName()).append("(");
//         Class<?>[] p = m.getParameterTypes();
//         for (int i = 0; i < p.length; i++) {
//             if (i > 0) sb.append(", ");
//             sb.append(p[i].getSimpleName());
//         }
//         sb.append(") -> ").append(m.getReturnType().getSimpleName());
//         return sb.toString();
//     }

//     @SuppressWarnings("unchecked")
//     private Object createNoopOir(Class<?> oirClz) {
//         Class<?> intf = oirClz;
//         if (intf == null) {
//             try { intf = Class.forName(OIR_CLASS); }
//             catch (ClassNotFoundException e) { log.error("OIR interface not found; returning null"); return null; }
//         }
//         return Proxy.newProxyInstance(
//                 intf.getClassLoader(),
//                 new Class[]{intf},
//                 (proxy, method, mArgs) -> {
//                     log.debug("NO-OP OIR invoked: {}()", method.getName());
//                     Class<?> rt = method.getReturnType();
//                     if (rt == boolean.class) return false;
//                     if (rt == void.class) return null;
//                     if (rt == byte.class) return (byte) 0;
//                     if (rt == short.class) return (short) 0;
//                     if (rt == int.class) return 0;
//                     if (rt == long.class) return 0L;
//                     if (rt == float.class) return 0f;
//                     if (rt == double.class) return 0d;
//                     if (rt == char.class) return (char) 0;
//                     return null;
//                 }
//         );
//     }

//     private Object createNoopDeviceSettings(Class<?> dsClz, String epr, NetworkInterface nicFromDpws) {
//         if (dsClz == null) return null;

//         NetworkInterface nicEff = nicFromDpws;
//         if (nicEff == null) {
//             String h = System.getProperty("Dpws.NetworkInterfaceName",
//                     System.getProperty("Dpws.Udp.NetworkInterfaceName",
//                             System.getProperty("Dpws.NetworkInterface",
//                                     System.getProperty("sdc.nic", ""))));
//             nicEff = resolveNic(h);
//         }
//         final NetworkInterface nicFinal = nicEff;

//         return Proxy.newProxyInstance(
//                 dsClz.getClassLoader(),
//                 new Class[]{dsClz},
//                 (proxy, method, args) -> {
//                     String name = method.getName().toLowerCase(Locale.ROOT);
//                     Class<?> rt = method.getReturnType();

//                     // NetworkInterface wiring
//                     if (rt == NetworkInterface.class || name.equals("getnetworkinterface")) {
//                         return nicFinal;
//                     }
//                     if (rt == Optional.class && (name.contains("networkinterface") || name.contains("nic"))) {
//                         return (nicFinal != null) ? Optional.of(nicFinal) : Optional.empty();
//                     }
//                     if (rt == String.class && (name.contains("networkinterfacename") || name.endsWith("nicname"))) {
//                         return (nicFinal != null) ? nicFinal.getName() : "";
//                     }

//                     // EPR / Endpoint
//                     if (rt.getName().equals("org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType")
//                             || (name.contains("endpoint") || name.contains("epr")) && rt.getName().endsWith("EndpointReferenceType")) {
//                         Object val = buildWsAddressingEpr(epr);
//                         if (val != null) return val;
//                         return null;
//                     }
//                     if (rt == Optional.class && (name.contains("endpoint") || name.contains("epr"))) {
//                         Object val = buildWsAddressingEpr(epr);
//                         return (val != null) ? Optional.of(val) : Optional.empty();
//                     }
//                     if (name.contains("endpoint") || name.contains("epr")) {
//                         if (rt == String.class) return epr;
//                         if (rt == java.net.URI.class) return toJavaUri(epr);
//                     }

//                     // Generic defaults
//                     if (Optional.class.isAssignableFrom(rt)) return Optional.empty();
//                     if (Collection.class.isAssignableFrom(rt)) return List.of();
//                     if (Map.class.isAssignableFrom(rt)) return Map.of();
//                     if (rt == boolean.class || rt == Boolean.class) return false;
//                     if (rt == int.class || rt == Integer.class) return 0;
//                     if (rt == long.class || rt == Long.class) return 0L;
//                     if (rt == double.class || rt == Double.class) return 0.0d;
//                     if (rt == float.class || rt == Float.class) return 0.0f;
//                     if (rt == short.class || rt == Short.class) return (short) 0;
//                     if (rt == byte.class || rt == Byte.class) return (byte) 0;
//                     if (rt == char.class || rt == Character.class) return '\0';
//                     if (rt == String.class) return "";
//                     if ("java.time.Duration".equals(rt.getName())) return java.time.Duration.ZERO;
//                     if (rt.isEnum()) {
//                         Object[] consts = rt.getEnumConstants();
//                         return (consts != null && consts.length > 0) ? consts[0] : null;
//                     }
//                     return null;
//                 }
//         );
//     }

//     /** purely diagnostic */
//     private static void debugDumpNicProps() {
//         String[] keys = new String[] {
//                 "Dpws.Udp.NetworkInterfaceName",
//                 "Dpws.NetworkInterfaceName",
//                 "Dpws.NetworkInterface",
//                 "Dpws.Udp.AdapterName",
//                 "WsDiscovery.NetworkInterfaceName",
//                 "WsDiscovery.AdapterName",
//                 "Dpws.Udp.BindAddress",
//                 "org.somda.sdc.dpws.udp.NetworkInterfaceName",
//                 "org.somda.sdc.dpws.NetworkInterfaceName",
//                 "org.somda.sdc.dpws.wsdiscovery.NetworkInterfaceName",
//                 "org.somda.sdc.dpws.udp.BindAddress",
//                 "dpws.udp.networkinterfacename",
//                 "dpws.networkinterfacename",
//                 "dpws.networkinterface",
//                 "dpws.udp.bindaddress",
//                 "sdc.nic"
//         };
//         StringBuilder sb = new StringBuilder("[NIC] ");
//         for (String k : keys) sb.append(k).append("=").append(System.getProperty(k, "∅")).append("; ");
//         log.info(sb.toString());
//     }

//     /** resolve NIC by name/IP/display name */
//     private static NetworkInterface resolveNic(String hint) {
//         if (hint == null || hint.isBlank()) return null;
//         try {
//             NetworkInterface ni = NetworkInterface.getByName(hint);
//             if (ni != null) return ni;
//         } catch (Exception ignore) {}
//         try {
//             if (hint.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
//                 InetAddress addr = InetAddress.getByName(hint);
//                 NetworkInterface ni = NetworkInterface.getByInetAddress(addr);
//                 if (ni != null) return ni;
//             }
//         } catch (Exception ignore) {}
//         try {
//             Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
//             String h = hint.toLowerCase(Locale.ROOT);
//             while (en.hasMoreElements()) {
//                 NetworkInterface ni = en.nextElement();
//                 String dn = ni.getDisplayName();
//                 String n = ni.getName();
//                 if (hint.equalsIgnoreCase(n) || (dn != null && hint.equalsIgnoreCase(dn))) return ni;
//                 if (dn != null && dn.toLowerCase(Locale.ROOT).contains(h)) return ni;
//             }
//         } catch (Exception ignore) {}
//         return null;
//     }
// }







package com.medstorm.sdcbridge;

import com.google.inject.Injector;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.provider.access.factory.LocalMdibAccessFactory;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.glue.provider.SdcDevice;
import org.somda.sdc.glue.provider.factory.SdcDeviceFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;

import com.medstorm.sdcbridge.MedstormNicResolver;

import com.google.inject.util.Modules;
import org.somda.sdc.biceps.guice.DefaultBicepsModule; // added





/**
 * Compile-safe SDC provider starter:
 * - builds Guice injector
 * - starts DPWS on a pinned NIC
 * - creates SdcDevice with explicit HP/LP service split (Get on LP)
 * - resolves service classes reflectively to tolerate package name differences
 */
@Component
public class SDCStackProvider {
    private static final Logger log = LoggerFactory.getLogger(SDCStackProvider.class);

    private static final String DEVICE_SETTINGS_CLASS = "org.somda.sdc.dpws.device.DeviceSettings";
    private static final String OIR_CLASS             = "org.somda.sdc.glue.provider.sco.OperationInvocationReceiver";
    private static final String LOCALIZATION_STORAGE_CLASS =
            "org.somda.sdc.glue.provider.localization.LocalizationStorage";

    private Injector injector;
    private DpwsFramework dpws;
    private SdcDevice device;
    private NetworkInterface selectedNic;

    @PostConstruct
    public void start() {
        final String nicHint = System.getProperty("sdc.nic", "").trim();
        final String epr     = System.getProperty("sdc.epr", "urn:uuid:medstorm-sensor-1");

        // Keep port stable unless overridden
        System.setProperty("Dpws.HttpServerPort",
                System.getProperty("Dpws.HttpServerPort", "53200"));

        final boolean hasGlueProvider = isPresent("org.somda.sdc.glue.provider.factory.SdcDeviceFactory");
        final boolean hasBiceps       = isPresent("org.somda.sdc.biceps.provider.access.LocalMdibAccess");
        log.info("SDCStackProvider: classpath -> glueProvider={}, biceps={}", hasGlueProvider, hasBiceps);
        debugDumpNicProps();

        // 1) Guice injector
        try {
            this.injector = com.google.inject.Guice.createInjector(
                    new org.somda.sdc.common.guice.DefaultCommonModule(),

                    com.google.inject.util.Modules.override(
                new org.somda.sdc.dpws.guice.DefaultDpwsModule()
        ).with(new com.medstorm.sdcbridge.MedstormJettyFactoryOverride()),

                    new org.somda.sdc.biceps.guice.DefaultBicepsModule(), 
                    Modules.override(new org.somda.sdc.glue.guice.DefaultGlueModule())
           .with(new com.medstorm.sdcbridge.MedstormProviderOnlyGlueModule()),
                    new com.medstorm.sdcbridge.MedstormSdcriConfigModule()
            );
            log.info("SDCStackProvider: Guice injector created.");
        } catch (Exception ex) {
            log.error("Failed to create Guice injector", ex);
            return;
        }

        // 2) Start DPWS on the requested NIC
        try {
            dpws = injector.getInstance(DpwsFramework.class);
            if (!nicHint.isBlank()) {
                selectedNic = MedstormNicResolver.resolveNic(nicHint).orElse(null);
                if (selectedNic == null) {
                    log.error("NIC hint '{}' could not be resolved; aborting DPWS start.", nicHint);
                    return;
                }
                dpws.setNetworkInterface(selectedNic);
                log.info("DPWS will bind to {} ({})", selectedNic.getName(), selectedNic.getDisplayName());
            } else {
                log.warn("No sdc.nic provided; DPWS will pick the default NIC.");
            }
            dpws.startAsync().awaitRunning();
            log.info("DPWS framework is RUNNING.");
        } catch (Exception ex) {
            log.error("Could not start DPWS framework", ex);
            return;
        }

        if (!(hasGlueProvider && hasBiceps)) {
            log.warn("GLUE or BICEPS missing -> provider not created; discovery still runs.");
            return;
        }

        // 3) MDIB access
        final LocalMdibAccess localMdib;
        try {
            LocalMdibAccessFactory mdibFactory = injector.getInstance(LocalMdibAccessFactory.class);
            localMdib = mdibFactory.createLocalMdibAccess();
        } catch (Exception ex) {
            log.error("Could not create LocalMdibAccess", ex);
            return;
        }

        // 4) Factory, DeviceSettings, OIR
        final SdcDeviceFactory factory;
        try {
            factory = injector.getInstance(SdcDeviceFactory.class);
        } catch (Exception ex) {
            log.error("Could not get SdcDeviceFactory", ex);
            return;
        }

        Object devSettings = null;
        try {
            Class<?> dsClz = Class.forName(DEVICE_SETTINGS_CLASS);
            try {
                devSettings = injector.getInstance(dsClz);
            } catch (Throwable t) {
                log.warn("DeviceSettings not in injector; using NO-OP fallback ({})", t.toString());
                devSettings = createNoopDeviceSettings(dsClz, epr, selectedNic);
            }
        } catch (Throwable t) {
            log.warn("DeviceSettings class not found; proceeding WITHOUT it. ({})", t.toString());
        }

        Object oir;
        try {
            Class<?> oirIface = Class.forName(OIR_CLASS);
            try {
                oir = injector.getInstance(oirIface);
            } catch (Throwable t) {
                log.warn("No Guice binding for OperationInvocationReceiver; using NO-OP fallback. ({})", t.toString());
                oir = createNoopOir(oirIface);
            }
        } catch (Throwable t) {
            log.warn("OIR class not found; creating NO-OP OIR. ({})", t.toString());
            oir = createNoopOir(null);
        }

        // 5) Explicit HP/LP service split (resolved reflectively; no hard imports)
        final List<Class<?>> hp = new ArrayList<>();
        addIfPresent(hp,
                // common SDCri locations across versions:
                "org.somda.sdc.biceps.provider.components.StateEventService",
                "org.somda.sdc.glue.provider.components.StateEventService"
        );
        addIfPresent(hp,
                "org.somda.sdc.biceps.provider.components.DescriptionEventService",
                "org.somda.sdc.glue.provider.components.DescriptionEventService"
        );
        addIfPresent(hp,
                "org.somda.sdc.biceps.provider.components.ContextService",
                "org.somda.sdc.glue.provider.components.ContextService"
        );
        addIfPresent(hp,
                "org.somda.sdc.biceps.provider.components.WaveformService",
                "org.somda.sdc.glue.provider.components.WaveformService"
        );
        addIfPresent(hp,
                "org.somda.sdc.biceps.provider.components.SetService",
                "org.somda.sdc.glue.provider.components.SetService"
        );

        final List<Class<?>> lp = new ArrayList<>();
        addIfPresent(lp,
                "org.somda.sdc.biceps.provider.components.GetService",
                "org.somda.sdc.glue.provider.components.GetService"
        );

        if (lp.isEmpty()) {
            log.warn("GetService class not found on classpath; LP will be empty. GetMdib may fail.");
        }

        // 6) Create + start device
        try {
            device = createDeviceWithServiceSplit(factory, localMdib, devSettings, oir, hp, lp);
            if (device != null) {
                device.startAsync().awaitRunning();
                log.info("✅ SDC device started, EPR={}", epr);
            } else {
                log.warn("SDC device not created (no matching factory method).");
            }
        } catch (Exception ex) {
            log.error("Failed to create/start SDC device", ex);
        }
    }

    @PreDestroy
    public void stop() {
        try { if (device != null) device.stopAsync().awaitTerminated(); } catch (Exception ignore) {}
        try { if (dpws != null) dpws.stopAsync().awaitTerminated(); } catch (Exception ignore) {}
        log.info("SDC stack stopped.");
    }

    // --------------------------- helpers ---------------------------

    private static boolean isPresent(String cn) {
        try { Class.forName(cn); return true; } catch (Throwable t) { return false; }
    }

    private static Optional<Class<?>> tryLoad(String... fqns) {
        for (String n : fqns) {
            try { return Optional.of(Class.forName(n)); } catch (Throwable ignore) {}
        }
        return Optional.empty();
    }

    private static void addIfPresent(List<Class<?>> sink, String... candidates) {
        tryLoad(candidates).ifPresent(sink::add);
    }

    private SdcDevice createDeviceWithServiceSplit(
            SdcDeviceFactory factory,
            LocalMdibAccess mdib,
            Object devSettings,
            Object oir,
            List<Class<?>> hp,
            List<Class<?>> lp) throws Exception {

        final Class<?> sdcDeviceType = org.somda.sdc.glue.provider.SdcDevice.class;

        // Prefer the 6-arg split method used by newer SDCri:
        // createSdcDevice(DeviceSettings, LocalMdibAccess, OperationInvocationReceiver, Collection, LocalizationStorage, List)
        for (Method m : factory.getClass().getMethods()) {
            if (!m.getName().equals("createSdcDevice")) continue;
            Class<?>[] p = m.getParameterTypes();
            
            if (p.length == 6
        && p[0].getName().endsWith("DeviceSettings")
        && p[1].isAssignableFrom(LocalMdibAccess.class)
        && p[2].getName().endsWith("OperationInvocationReceiver")
        && java.util.Collection.class.isAssignableFrom(p[3])
        && java.util.List.class.isAssignableFrom(p[5])) {

    // Build a non-null LocalizationStorage if required by param #5
    Object locStorage = null;
    try {
        if (p[4].getName().equals(LOCALIZATION_STORAGE_CLASS)
                || p[4].getSimpleName().equals("LocalizationStorage")) {
            locStorage = createNoopLocalizationStorage(p[4]); // <- non-null proxy
        }
    } catch (Throwable t) {
        log.warn("Could not construct LocalizationStorage: {}", t.toString());
    }
    if (locStorage == null && (p[4].getName().endsWith("LocalizationStorage")
            || p[4].getSimpleName().equals("LocalizationStorage"))) {
        // Absolute last resort: fail fast with a clear message
        throw new IllegalStateException("LocalizationStorage parameter is required by factory but could not be created");
    }

    Object ret = m.invoke(factory, devSettings, mdib, oir, hp,
            locStorage /* was null */, lp);

    SdcDevice dev = unwrapToSdcDevice(ret, sdcDeviceType);
    if (dev != null) {
        log.info("Using SDC factory method: {}", sig(m));
        return dev;
    }
}

        }

        log.warn("Split createSdcDevice(...) not found; falling back to generic factory search.");
        return createDeviceCompat(factory, mdib, devSettings, oir);
    }

    private SdcDevice createDeviceCompat(
            SdcDeviceFactory factory,
            LocalMdibAccess mdib,
            Object devSettings,
            Object oir) throws Exception {

        final Class<?> sdcDeviceType = org.somda.sdc.glue.provider.SdcDevice.class;
        final Class<?> mdibType      = org.somda.sdc.biceps.provider.access.LocalMdibAccess.class;
        final Class<?> oirType       = Class.forName(OIR_CLASS);

        final List<Method> candidates = new ArrayList<>();
        for (Class<?> itf : factory.getClass().getInterfaces()) {
            for (Method m : itf.getMethods()) {
                if (returnsSdcDeviceOrProvider(m, sdcDeviceType)) candidates.add(m);
            }
        }
        for (Method m : factory.getClass().getMethods()) {
            if (returnsSdcDeviceOrProvider(m, sdcDeviceType)) candidates.add(m);
        }
        candidates.sort((a, b) -> Integer.compare(methodScore(b), methodScore(a)));

        List<String> tried = new ArrayList<>();
        for (Method m : candidates) {
            final Class<?>[] p = m.getParameterTypes();
            final Object[] args = new Object[p.length];
            boolean ok = true;

            for (int i = 0; i < p.length; i++) {
                Class<?> t = p[i];

                if (t.isAssignableFrom(mdibType)) {
                    if (mdib == null) { ok = false; break; }
                    args[i] = mdib;
                } else if (t.isAssignableFrom(oirType)) {
                    if (oir == null) { ok = false; break; }
                    args[i] = oir;
                } else if (java.util.Collection.class.isAssignableFrom(t)) {
                    args[i] = List.of(); // HP services (unknown signature) -> empty
                } else if (t.getName().equals(LOCALIZATION_STORAGE_CLASS)
                        || t.getSimpleName().equals("LocalizationStorage")) {
                    args[i] = createNoopLocalizationStorage(t);
                } else if (devSettings != null &&
                        (t.isInstance(devSettings) || t.getName().endsWith("DeviceSettings"))) {
                    args[i] = devSettings;
                } else if (java.util.List.class.isAssignableFrom(t)) {
                    args[i] = List.of(); // LP services (unknown signature) -> empty
                } else {
                    ok = false;
                    break;
                }
            }

            if (!ok) { tried.add(sig(m)); continue; }

            try {
                Object ret = m.invoke(factory, args);
                SdcDevice dev = unwrapToSdcDevice(ret, sdcDeviceType);
                if (dev != null) {
                    log.info("Using SDC factory method: {}", sig(m));
                    return dev;
                }
            } catch (InvocationTargetException ite) {
                Throwable cause = ite.getTargetException();
                log.warn("Factory {} threw {}: {} (trying next)",
                        sig(m), cause.getClass().getSimpleName(), cause.getMessage());
                tried.add(sig(m) + " -> " + cause.getClass().getSimpleName());
            } catch (Throwable t) {
                tried.add(sig(m) + " -> " + t.getClass().getSimpleName());
            }
        }

        log.error("No usable SDC device factory method. Tried:\n  {}", String.join("\n  ", tried));
        logFactorySignatureHints(factory);
        throw new IllegalStateException("Could not obtain SdcDevice from factory " + factory.getClass());
    }

    private SdcDevice unwrapToSdcDevice(Object ret, Class<?> sdcDeviceType) throws Exception {
        if (ret == null) return null;
        if (sdcDeviceType.isInstance(ret)) return (SdcDevice) ret;
        for (String provName : new String[]{"javax.inject.Provider", "com.google.inject.Provider"}) {
            try {
                Class<?> provClz = Class.forName(provName);
                if (provClz.isInstance(ret)) {
                    Method get = provClz.getMethod("get");
                    Object v = get.invoke(ret);
                    if (sdcDeviceType.isInstance(v)) return (SdcDevice) v;
                }
            } catch (Throwable ignore) {}
        }
        return null;
    }

    private boolean returnsSdcDeviceOrProvider(Method m, Class<?> sdcDeviceType) {
        Class<?> rt = m.getReturnType();
        if (rt != null && sdcDeviceType.isAssignableFrom(rt)) return true;
        String rn = rt.getName();
        return rn.equals("javax.inject.Provider") || rn.equals("com.google.inject.Provider");
    }

    private int methodScore(Method m) {
        String n = m.getName().toLowerCase(Locale.ROOT);
        int score = 0;
        if (n.startsWith("create")) score += 100;
        if (n.startsWith("build"))  score += 50;
        if (n.startsWith("get"))    score += 25;
        score += m.getParameterCount();
        if (m.getDeclaringClass().getName().startsWith("org.somda.sdc")) score += 5;
        return score;
    }

    private String sig(Method m) {
        StringBuilder sb = new StringBuilder();
        sb.append(m.getDeclaringClass().getSimpleName()).append(".").append(m.getName()).append("(");
        Class<?>[] p = m.getParameterTypes();
        for (int i = 0; i < p.length; i++) { if (i > 0) sb.append(", "); sb.append(p[i].getSimpleName()); }
        sb.append(") -> ").append(m.getReturnType().getSimpleName());
        return sb.toString();
    }

    private void logFactorySignatureHints(Object factory) {
        try {
            StringBuilder sb = new StringBuilder("Available factory methods on ")
                    .append(factory.getClass()).append(":\n");
            for (Method m : factory.getClass().getMethods()) {
                sb.append("  ").append(sig(m)).append("\n");
            }
            log.error(sb.toString());
        } catch (Throwable ignore) {}
    }

    private Object createNoopLocalizationStorage(Class<?> lsClz) {
        try {
            return Proxy.newProxyInstance(
                    lsClz.getClassLoader(),
                    new Class[]{lsClz},
                    (proxy, method, args) -> {
                        Class<?> rt = method.getReturnType();
                        if (Optional.class.isAssignableFrom(rt)) return Optional.empty();
                        if (Collection.class.isAssignableFrom(rt)) return List.of();
                        if (Map.class.isAssignableFrom(rt)) return Map.of();
                        if (rt == boolean.class || rt == Boolean.class) return false;
                        if (rt == int.class || rt == Integer.class) return 0;
                        if (rt == long.class || rt == Long.class) return 0L;
                        if (rt == double.class || rt == Double.class) return 0.0d;
                        if (rt == float.class || rt == Float.class) return 0.0f;
                        if (rt == short.class || rt == Short.class) return (short) 0;
                        if (rt == byte.class || rt == Byte.class) return (byte) 0;
                        if (rt == char.class || rt == Character.class) return '\0';
                        if (rt == String.class) return "";
                        if ("java.time.Duration".equals(rt.getName())) return java.time.Duration.ZERO;
                        if (rt.isEnum()) {
                            Object[] consts = rt.getEnumConstants();
                            return (consts != null && consts.length > 0) ? consts[0] : null;
                        }
                        return null;
                    }
            );
        } catch (Throwable t) {
            log.warn("Could not construct NO-OP LocalizationStorage: {}", t.toString());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Object createNoopOir(Class<?> oirClzOrNull) {
        Class<?> intf = oirClzOrNull;
        if (intf == null) {
            try { intf = Class.forName(OIR_CLASS); }
            catch (ClassNotFoundException e) { log.error("OIR interface not found; returning null"); return null; }
        }
        return Proxy.newProxyInstance(
                intf.getClassLoader(),
                new Class[]{intf},
                (proxy, method, mArgs) -> {
                    log.debug("NO-OP OIR invoked: {}()", method.getName());
                    Class<?> rt = method.getReturnType();
                    if (rt == boolean.class) return false;
                    if (rt == void.class) return null;
                    if (rt == byte.class) return (byte) 0;
                    if (rt == short.class) return (short) 0;
                    if (rt == int.class) return 0;
                    if (rt == long.class) return 0L;
                    if (rt == float.class) return 0f;
                    if (rt == double.class) return 0d;
                    if (rt == char.class) return (char) 0;
                    return null;
                }
        );
    }

    private Object createNoopDeviceSettings(Class<?> dsClz, String epr, NetworkInterface nicFromDpws) {
        if (dsClz == null) return null;
        final NetworkInterface nicFinal = nicFromDpws;

        return Proxy.newProxyInstance(
                dsClz.getClassLoader(),
                new Class[]{dsClz},
                (proxy, method, args) -> {
                    String name = method.getName().toLowerCase(Locale.ROOT);
                    Class<?> rt = method.getReturnType();

                    if (rt == NetworkInterface.class || name.equals("getnetworkinterface")) {
                        return nicFinal;
                    }
                    if (rt == Optional.class && (name.contains("networkinterface") || name.contains("nic"))) {
                        return Optional.ofNullable(nicFinal);
                    }
                    if (rt == String.class && (name.contains("networkinterfacename") || name.endsWith("nicname"))) {
                        return (nicFinal != null) ? nicFinal.getName() : "";
                    }

                    // EPR / Endpoint
                    if (name.contains("endpoint") || name.contains("epr")) {
                        if (rt == String.class) return epr;
                        if (rt == java.net.URI.class) return toJavaUri(epr);
                        Object val = buildWsAddressingEpr(epr);
                        if (rt.getName().endsWith("EndpointReferenceType")) return val;
                        if (rt == Optional.class) return Optional.ofNullable(val);
                    }

                    // Generic defaults
                    if (Optional.class.isAssignableFrom(rt)) return Optional.empty();
                    if (Collection.class.isAssignableFrom(rt)) return List.of();
                    if (Map.class.isAssignableFrom(rt)) return Map.of();
                    if (rt == boolean.class || rt == Boolean.class) return false;
                    if (rt == int.class || rt == Integer.class) return 0;
                    if (rt == long.class || rt == Long.class) return 0L;
                    if (rt == double.class || rt == Double.class) return 0.0d;
                    if (rt == float.class || rt == Float.class) return 0.0f;
                    if (rt == short.class || rt == Short.class) return (short) 0;
                    if (rt == byte.class || rt == Byte.class) return (byte) 0;
                    if (rt == char.class || rt == Character.class) return '\0';
                    if (rt == String.class) return "";
                    if ("java.time.Duration".equals(rt.getName())) return java.time.Duration.ZERO;
                    if (rt.isEnum()) {
                        Object[] consts = rt.getEnumConstants();
                        return (consts != null && consts.length > 0) ? consts[0] : null;
                    }
                    return null;
                }
        );
    }

    private Object buildWsAddressingEpr(String epr) {
        try {
            Class<?> eprClz = Class.forName("org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType");
            Class<?> attrUriClz = Class.forName("org.somda.sdc.dpws.soap.wsaddressing.model.AttributedURIType");

            Object eprObj = eprClz.getConstructor().newInstance();
            Object addr   = attrUriClz.getConstructor().newInstance();

            attrUriClz.getMethod("setValue", String.class).invoke(addr, epr);
            eprClz.getMethod("setAddress", attrUriClz).invoke(eprObj, addr);
            return eprObj;
        } catch (Throwable t) {
            log.warn("Could not construct WS-Addressing EPR: {}", t.toString());
            return null;
        }
    }

    private static Object toJavaUri(String epr) {
        try { return java.net.URI.create(epr); } catch (Throwable ignore) { return null; }
    }

    /** purely diagnostic */
    private static void debugDumpNicProps() {
        String[] keys = new String[] {
                "Dpws.Udp.NetworkInterfaceName",
                "Dpws.NetworkInterfaceName",
                "Dpws.NetworkInterface",
                "Dpws.Udp.AdapterName",
                "WsDiscovery.NetworkInterfaceName",
                "WsDiscovery.AdapterName",
                "Dpws.Udp.BindAddress",
                "org.somda.sdc.dpws.udp.NetworkInterfaceName",
                "org.somda.sdc.dpws.NetworkInterfaceName",
                "org.somda.sdc.dpws.wsdiscovery.NetworkInterfaceName",
                "org.somda.sdc.dpws.udp.BindAddress",
                "dpws.udp.networkinterfacename",
                "dpws.networkinterfacename",
                "dpws.networkinterface",
                "dpws.udp.bindaddress",
                "sdc.nic"
        };
        StringBuilder sb = new StringBuilder("[NIC] ");
        for (String k : keys) sb.append(k).append("=").append(System.getProperty(k, "∅")).append("; ");
        log.info(sb.toString());
    }
}
