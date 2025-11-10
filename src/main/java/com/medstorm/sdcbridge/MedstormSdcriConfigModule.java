


// package com.medstorm.sdcbridge;

// import com.google.inject.AbstractModule;
// import com.google.inject.Provides;
// import com.google.inject.Singleton;
// import com.google.inject.TypeLiteral;
// import com.google.inject.name.Named;
// import com.google.inject.name.Names;
// import org.eclipse.jetty.util.ssl.SslContextFactory;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import javax.net.ssl.HostnameVerifier;
// import java.io.File;
// import java.io.InputStream;
// import java.lang.reflect.Constructor;
// import java.lang.reflect.Method;
// import java.lang.reflect.Proxy;
// import java.net.*;
// import java.time.Duration;
// import java.util.*;
// import java.util.function.Supplier;


// import org.somda.sdc.dpws.CommunicationLogContext;

// import org.somda.sdc.biceps.common.storage.DescriptionPreprocessingSegment;
// import org.somda.sdc.biceps.common.storage.StatePreprocessingSegment;
// import org.somda.sdc.dpws.CommunicationLog;
// import org.somda.sdc.dpws.CommunicationLogContext;
// import org.somda.sdc.dpws.crypto.CryptoSettings;
// import org.somda.sdc.dpws.device.DeviceSettings;
// import org.somda.sdc.dpws.http.HttpConnectionInterceptor;
// import org.somda.sdc.dpws.http.jetty.JettyHttpServerHandler;
// import org.somda.sdc.dpws.http.jetty.factory.JettyHttpServerHandlerFactory;
// import org.somda.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
// import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
// import org.somda.sdc.dpws.wsdl.WsdlProvisioningMode;

// public final class MedstormSdcriConfigModule extends AbstractModule {
//     private static final Logger log = LoggerFactory.getLogger(MedstormSdcriConfigModule.class);

//     @Override
//     protected void configure() {
//         log.info("[MedstormSdcriConfigModule] installing config module");

//         // IPv4 + Datagram on Windows
//         System.setProperty("java.net.preferIPv4Stack", "true");
//         System.setProperty("java.net.preferIPv4Addresses", "true");
//         System.setProperty("jdk.net.usePlainDatagramSocketImpl", "true");

//         final String nicHintRaw = System.getProperty("sdc.nic", "").trim();
//         final int httpPort = parseIntOr(System.getProperty("Dpws.HttpServerPort", ""), 53200);

//         final NetworkInterface nic = resolveNic(nicHintRaw).orElseThrow(() -> {
//             final String msg = "Could not resolve NetworkInterface from sdc.nic='" + nicHintRaw + "'";
//             log.error("[MedstormSdcriConfigModule] {}", msg);
//             return new IllegalStateException(msg);
//         });
//         final String nicName = nic.getName();
//         final String nicDisplay = Optional.ofNullable(nic.getDisplayName()).orElse(nicName);
//         final InetAddress ipv4 = firstIpv4(nic).orElseThrow(() -> {
//             final String msg = "No IPv4 address found on NIC '" + nicName + "' (" + nicDisplay + ")";
//             log.error("[MedstormSdcriConfigModule] {}", msg);
//             return new IllegalStateException(msg);
//         });

//         log.info("[MedstormSdcriConfigModule] binding NIC '{}' (ipv4={})", nicName, ipv4.getHostAddress());
//         log.info("[MedstormSdcriConfigModule] Jetty HTTP bind pinned to {}:{}", ipv4.getHostAddress(), httpPort);

//         // System properties some SDCri parts read directly
//         System.setProperty("Dpws.Udp.NetworkInterfaceName", nicName);
//         System.setProperty("Dpws.NetworkInterfaceName", nicName);
//         System.setProperty("Dpws.NetworkInterface", nicName);
//         System.setProperty("Dpws.Udp.AdapterName", nicName);
//         System.setProperty("WsDiscovery.NetworkInterfaceName", nicName);
//         System.setProperty("WsDiscovery.AdapterName", nicName);

//         System.setProperty("org.somda.sdc.dpws.udp.NetworkInterfaceName", nicName);
//         System.setProperty("org.somda.sdc.dpws.NetworkInterfaceName", nicName);
//         System.setProperty("org.somda.sdc.dpws.wsdiscovery.NetworkInterfaceName", nicName);

//         System.setProperty("Dpws.Udp.BindAddress", ipv4.getHostAddress());
//         System.setProperty("org.somda.sdc.dpws.udp.BindAddress", ipv4.getHostAddress());
//         System.setProperty("dpws.udp.bindaddress", ipv4.getHostAddress());
//         System.setProperty("dpws.networkinterfacename", nicName);

//         System.setProperty("Dpws.HttpHost", ipv4.getHostAddress());
//         System.setProperty("Dpws.HttpPort", Integer.toString(httpPort));
//         System.setProperty("Dpws.HttpServerBindAddress", ipv4.getHostAddress() + ":" + httpPort);

//         // Bind strings/addresses/nic
//         bind(String.class).annotatedWith(Names.named("Dpws.NetworkInterfaceName")).toInstance(nicName);
//         bind(String.class).annotatedWith(Names.named("org.somda.sdc.dpws.NetworkInterfaceName")).toInstance(nicName);
//         bind(String.class).annotatedWith(Names.named("Dpws.Udp.NetworkInterfaceName")).toInstance(nicName);
//         bind(String.class).annotatedWith(Names.named("org.somda.sdc.dpws.udp.NetworkInterfaceName")).toInstance(nicName);
//         bind(String.class).annotatedWith(Names.named("WsDiscovery.NetworkInterfaceName")).toInstance(nicName);
//         bind(String.class).annotatedWith(Names.named("org.somda.sdc.dpws.wsdiscovery.NetworkInterfaceName")).toInstance(nicName);

//         bind(String.class).annotatedWith(Names.named("Dpws.BindAddress")).toInstance(ipv4.getHostAddress());
//         bind(String.class).annotatedWith(Names.named("org.somda.sdc.dpws.BindAddress")).toInstance(ipv4.getHostAddress());
//         bind(String.class).annotatedWith(Names.named("Dpws.Udp.BindAddress")).toInstance(ipv4.getHostAddress());
//         bind(String.class).annotatedWith(Names.named("org.somda.sdc.dpws.udp.BindAddress")).toInstance(ipv4.getHostAddress());

//         bind(NetworkInterface.class).annotatedWith(Names.named("Dpws.NetworkInterface")).toInstance(nic);
//         bind(NetworkInterface.class).annotatedWith(Names.named("org.somda.sdc.dpws.NetworkInterface")).toInstance(nic);
//         bind(NetworkInterface.class).annotatedWith(Names.named("Dpws.Udp.NetworkInterface")).toInstance(nic);
//         bind(NetworkInterface.class).annotatedWith(Names.named("org.somda.sdc.dpws.udp.NetworkInterface")).toInstance(nic);

//         bind(new TypeLiteral<Optional<NetworkInterface>>(){}).annotatedWith(Names.named("Dpws.NetworkInterface")).toInstance(Optional.of(nic));
//         bind(new TypeLiteral<Optional<NetworkInterface>>(){}).annotatedWith(Names.named("org.somda.sdc.dpws.NetworkInterface")).toInstance(Optional.of(nic));
//         bind(new TypeLiteral<Optional<NetworkInterface>>(){}).annotatedWith(Names.named("Dpws.Udp.NetworkInterface")).toInstance(Optional.of(nic));
//         bind(new TypeLiteral<Optional<NetworkInterface>>(){}).annotatedWith(Names.named("org.somda.sdc.dpws.udp.NetworkInterface")).toInstance(Optional.of(nic));

//         bind(new TypeLiteral<Optional<InetAddress>>(){}).annotatedWith(Names.named("Dpws.Udp.BindAddress")).toInstance(Optional.of(ipv4));

//         bind(Integer.class).annotatedWith(Names.named("Dpws.HttpServerPort")).toInstance(httpPort);

//         // HTTP flags
//         bind(Boolean.class).annotatedWith(Names.named("Dpws.EnableHttp")).toInstance(true);
//         bind(Boolean.class).annotatedWith(Names.named("Dpws.EnableHttps")).toInstance(false);
//         bind(Boolean.class).annotatedWith(Names.named("Dpws.EnforceHttpChunked")).toInstance(false);
//         bind(Boolean.class).annotatedWith(Names.named("Dpws.GzipCompression")).toInstance(false);
//         bind(Boolean.class).annotatedWith(Names.named("Dpws.ServerEnableJmx")).toInstance(false);
//         bind(Boolean.class).annotatedWith(Names.named("Dpws.ClientRetryPost")).toInstance(false);
//         bind(Boolean.class).annotatedWith(Names.named("Dpws.Client.AutoResolve")).toInstance(false);

//         // Communication log toggles
//         // Enable comm-log so factory supplies non-null CommunicationLog to Jetty handler
// System.setProperty("Dpws.ServerCommlogInHandler", "true");
// bind(Boolean.class).annotatedWith(Names.named("Dpws.ServerCommlogInHandler")).toInstance(true);





//         bind(Boolean.class).annotatedWith(Names.named("Dpws.CommunicationLogPrettyPrintXml")).toInstance(false);
//         bind(Boolean.class).annotatedWith(Names.named("Dpws.CommunicationLogWithHttpHeaders")).toInstance(false);
//         bind(Boolean.class).annotatedWith(Names.named("Dpws.CommunicationLogWithRequestResponseId")).toInstance(false);

//         // SOAP/JAXB
//         bind(String.class).annotatedWith(Names.named("Dpws.HttpCharset")).toInstance("UTF-8");
//         bind(String.class).annotatedWith(Names.named("Common.InstanceIdentifier")).toInstance("medstorm-sdcbridge-instance");
//         bind(File.class).annotatedWith(Names.named("Dpws.CommunicationLogSinkDirectory")).toInstance(new File("logs/sdc"));

//         bind(String.class).annotatedWith(Names.named("SoapConfig.JaxbContextPath")).toInstance("org.somda.sdc.dpws.soap.model");
//         bind(String.class).annotatedWith(Names.named("SoapConfig.JaxbSchemaPath")).toInstance("");
//         bind(String.class).annotatedWith(Names.named("SoapConfig.NamespaceMappings")).toInstance("");
//         bind(Boolean.class).annotatedWith(Names.named("SoapConfig.MetadataComment")).toInstance(false);
//         bind(Boolean.class).annotatedWith(Names.named("SoapConfig.ValidateSoapMessages")).toInstance(false);

//         // TLS placeholders (HTTPS disabled)
//         bind(String[].class).annotatedWith(Names.named("Dpws.Crypto.TlsEnabledVersions"))
//                 .toInstance(new String[]{"TLSv1.2", "TLSv1.3"});
//         bind(String[].class).annotatedWith(Names.named("Dpws.Crypto.TlsEnabledCiphers"))
//                 .toInstance(new String[]{"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"});
//         bind(CryptoSettings.class).annotatedWith(Names.named("Dpws.Crypto.Settings")).toInstance(new NoTlsCryptoSettings());
//         bind(HostnameVerifier.class).annotatedWith(Names.named("Dpws.Crypto.ClientHostnameVerifier")).toInstance((h, s) -> true);
//         bind(HostnameVerifier.class).annotatedWith(Names.named("Dpws.Crypto.DeviceHostnameVerifier")).toInstance((h, s) -> true);

//         // Pools, TTL, queue sizes
//         bind(Integer.class).annotatedWith(Names.named("Dpws.ClientPoolSize")).toInstance(4);
//         bind(Integer.class).annotatedWith(Names.named("Dpws.GzipCompressionMinSize")).toInstance(1024);
//         bind(Integer.class).annotatedWith(Names.named("Dpws.ServerThreadPoolSize")).toInstance(4);
//         bind(Integer.class).annotatedWith(Names.named("Dpws.MulticastTtl")).toInstance(4);
//         bind(Integer.class).annotatedWith(Names.named("SoapConfig.NotificationQueueCapacity")).toInstance(1024);
//         bind(Integer.class).annotatedWith(Names.named("WsAddressing.MessageIdCacheSize")).toInstance(100);
//         bind(Integer.class).annotatedWith(Names.named("WsDiscovery.MaxProbeMatchesBufferSize")).toInstance(64);
//         bind(Integer.class).annotatedWith(Names.named("WsDiscovery.MaxResolveMatchesBufferSize")).toInstance(64);

//         bind(Duration.class).annotatedWith(Names.named("Dpws.HttpClientConnectTimeout")).toInstance(Duration.ofSeconds(10));
//         bind(Duration.class).annotatedWith(Names.named("Dpws.HttpClientReadTimeout")).toInstance(Duration.ofSeconds(15));
//         bind(Duration.class).annotatedWith(Names.named("Dpws.HttpServerConnectionTimeout")).toInstance(Duration.ofSeconds(30));
//         bind(Duration.class).annotatedWith(Names.named("Dpws.MaxWaitForFutures")).toInstance(Duration.ofSeconds(10));
//         bind(Duration.class).annotatedWith(Names.named("Dpws.Client.MaxWaitForResolveMatches")).toInstance(Duration.ofSeconds(5));
//         bind(Duration.class).annotatedWith(Names.named("WsDiscovery.MaxWaitForProbeMatches")).toInstance(Duration.ofSeconds(5));
//         bind(Duration.class).annotatedWith(Names.named("WsDiscovery.MaxWaitForResolveMatches")).toInstance(Duration.ofSeconds(5));

//         bind(String.class).annotatedWith(Names.named("WsEventing.Source.SubscriptionManagerPath")).toInstance("/SubscriptionManager");
//         bind(Duration.class).annotatedWith(Names.named("WsEventing.Source.MaxExpires")).toInstance(Duration.ofMinutes(10));
//         bind(Boolean.class).annotatedWith(Names.named("WsAddressing.IgnoreMessageIds")).toInstance(false);

//         bind(SslContextFactory.Server.class).toInstance(new SslContextFactory.Server());

//         // ===== HTTP connection interceptor shapes =====
//         final HttpConnectionInterceptor noopInterceptor = new HttpConnectionInterceptor() {};
//         // bind(HttpConnectionInterceptor.class).toInstance(noopInterceptor);
//         // bind(HttpConnectionInterceptor.class)
//         //         .annotatedWith(Names.named("Dpws.HttpConnectionInterceptor"))
//         //         .toInstance(noopInterceptor);

//         List<HttpConnectionInterceptor> interceptorList = Collections.singletonList(noopInterceptor);
//         bind(new TypeLiteral<List<HttpConnectionInterceptor>>() {}).toInstance(interceptorList);
//         bind(new TypeLiteral<List<HttpConnectionInterceptor>>() {})
//                 .annotatedWith(Names.named("Dpws.HttpConnectionInterceptors"))
//                 .toInstance(interceptorList);

//         Set<HttpConnectionInterceptor> interceptorSet = new LinkedHashSet<>(interceptorList);
//         bind(new TypeLiteral<Set<HttpConnectionInterceptor>>() {}).toInstance(interceptorSet);
//         bind(new TypeLiteral<Set<HttpConnectionInterceptor>>() {})
//                 .annotatedWith(Names.named("Dpws.HttpConnectionInterceptors"))
//                 .toInstance(interceptorSet);

//         // Glue/BICEPS toggles
//         bind(Boolean.class).annotatedWith(Names.named("Biceps.Common.CopyMdibInput")).toInstance(true);
//         bind(Boolean.class).annotatedWith(Names.named("Biceps.Common.CopyMdibOutput")).toInstance(true);
//         bind(Boolean.class).annotatedWith(Names.named("Biceps.Common.StoreNotAssociatedContextStates")).toInstance(true);
//         bind(Boolean.class).annotatedWith(Names.named("SdcGlue.Consumer.ApplyReportsWithSameMdibVersion")).toInstance(true);

//         // Glue timeouts
//         bind(Duration.class).annotatedWith(Names.named("SdcGlue.Consumer.RequestedExpires")).toInstance(Duration.ofMinutes(1));
//         bind(Duration.class).annotatedWith(Names.named("SdcGlue.Consumer.AwaitingTransactionTimeout")).toInstance(Duration.ofSeconds(30));
//         bind(Duration.class).annotatedWith(Names.named("SdcGlue.Consumer.WatchdogPeriod")).toInstance(Duration.ofSeconds(10));

//         // Preprocessing segments
//         bind(new TypeLiteral<List<Class<? extends DescriptionPreprocessingSegment>>>() {})
//                 .annotatedWith(Names.named("Biceps.Common.ConsumerDescriptionPreprocessingSegments"))
//                 .toInstance(Collections.emptyList());
//         bind(new TypeLiteral<List<Class<? extends DescriptionPreprocessingSegment>>>() {})
//                 .annotatedWith(Names.named("Biceps.Common.ProviderDescriptionPreprocessingSegments"))
//                 .toInstance(Collections.emptyList());
//         bind(new TypeLiteral<List<Class<? extends StatePreprocessingSegment>>>() {})
//                 .annotatedWith(Names.named("Biceps.Common.ConsumerStatePreprocessingSegments"))
//                 .toInstance(Collections.emptyList());
//         bind(new TypeLiteral<List<Class<? extends StatePreprocessingSegment>>>() {})
//                 .annotatedWith(Names.named("Biceps.Common.ProviderStatePreprocessingSegments"))
//                 .toInstance(Collections.emptyList());

//                 // --- Ensure Jetty has a non-null HttpConnectionInterceptor (SDCri 6 expects one)
// try {
//     Class<?> intf = Class.forName("org.somda.sdc.dpws.http.HttpConnectionInterceptor");
//     Object noop = java.lang.reflect.Proxy.newProxyInstance(
//             intf.getClassLoader(),
//             new Class[]{intf},
//             (proxy, method, args) -> null // no-op for all hooks
//     );
//     // Use raw bind to avoid generics issues at compile time
//     @SuppressWarnings({"unchecked","rawtypes"})
//     com.google.inject.binder.LinkedBindingBuilder lb = bind((Class) intf);
//     lb.toInstance(noop);
//     System.out.println("[MedstormSdcriConfigModule] Bound NO-OP HttpConnectionInterceptor");
// } catch (ClassNotFoundException ignore) {
//     // Older builds may not have the interface; nothing to do.
// }


//         // WSDL provisioning (robust to enum diffs)
//         bind(WsdlProvisioningMode.class)
//                 .annotatedWith(Names.named("Dpws.WsdlProvisioningMode"))
//                 .toInstance(resolveWsdlProvisioningMode());

//         log.info("[MedstormSdcriConfigModule] All required Guice bindings configured.");
//     }

//     // ========= Providers =========

    

//     @Provides @Singleton
//     CommunicationLog provideCommunicationLog() {
//         return (CommunicationLog) Proxy.newProxyInstance(
//                 CommunicationLog.class.getClassLoader(),
//                 new Class[]{CommunicationLog.class},
//                 (Object p, Method m, Object[] a) -> {
//                     Class<?> rt = m.getReturnType();
//                     if (!rt.isPrimitive()) return null;
//                     if (rt == boolean.class) return false;
//                     if (rt == byte.class)    return (byte) 0;
//                     if (rt == short.class)   return (short) 0;
//                     if (rt == int.class)     return 0;
//                     if (rt == long.class)    return 0L;
//                     if (rt == float.class)   return 0f;
//                     if (rt == double.class)  return 0d;
//                     if (rt == char.class)    return '\0';
//                     return null; // void
//                 }
//         );
//     }

//     @Provides @Singleton @Named("Dpws.CommunicationLog")
//     CommunicationLog provideNamedCommunicationLog(CommunicationLog cl) { return cl; }

// //     @Provides
// // @Singleton
// // public CommunicationLogContext provideCommunicationLogContext() {
// //     // Prefer a plain instance if the class has a public or package-private no-arg ctor.
// //     try {
// //         var ctor = CommunicationLogContext.class.getDeclaredConstructor();
// //         ctor.setAccessible(true);
// //         return ctor.newInstance();
// //     } catch (NoSuchMethodException ignored) {
// //         // Older/newer SDCri may not expose a no-arg ctor; fall through.
// //     } catch (Throwable ctorFail) {
// //         // If a ctor exists but failed, rethrow with context.
// //         throw new IllegalStateException("Could not instantiate CommunicationLogContext via default constructor", ctorFail);
// //     }

// //     // If the type is (in some versions) an interface, only then use a proxy as a last resort.
// //     if (CommunicationLogContext.class.isInterface()) {
// //         return (CommunicationLogContext) java.lang.reflect.Proxy.newProxyInstance(
// //             CommunicationLogContext.class.getClassLoader(),
// //             new Class<?>[]{CommunicationLogContext.class},
// //             (proxy, method, args) -> null  // no-op context
// //         );
// //     }










// @Provides
// @Singleton
// CommunicationLogContext provideCommunicationLogContext() {
//     // Allow override via -DDpws.CommunicationLogContext=...
//     final String name = System.getProperty("Dpws.CommunicationLogContext", "medstorm");
//     return new CommunicationLogContext(name);
// }

// @Provides
// @Singleton
// @Named("Dpws.CommunicationLogContext")
// CommunicationLogContext provideNamedCommunicationLogContext(CommunicationLogContext ctx) {
//     return ctx;
// }



//     @Provides @Singleton @Named("Dpws.CommunicationLog")
//     CommunicationLogContext provideContextAliasUnderCommlogName(CommunicationLogContext ctx) { return ctx; }

//     // Optionals / Suppliers
//     @Provides @Singleton
//     Optional<CommunicationLog> provideOptCommlog(CommunicationLog cl) { return Optional.of(cl); }

//     @Provides @Singleton @Named("Dpws.CommunicationLog")
//     Optional<CommunicationLog> provideOptCommlogNamed(@Named("Dpws.CommunicationLog") CommunicationLog cl) { return Optional.of(cl); }

//     @Provides @Singleton
//     Supplier<CommunicationLog> provideCommlogSupplier(CommunicationLog cl) { return () -> cl; }

//     @Provides @Singleton @Named("Dpws.CommunicationLog")
//     Supplier<CommunicationLog> provideCommlogSupplierNamed(@Named("Dpws.CommunicationLog") CommunicationLog cl) { return () -> cl; }

//     @Provides @Singleton
//     Optional<CommunicationLogContext> provideOptCommlogCtx(CommunicationLogContext ctx) { return Optional.of(ctx); }

//     @Provides @Singleton @Named("Dpws.CommunicationLogContext")
//     Optional<CommunicationLogContext> provideOptNamedOptCommlogCtx(@Named("Dpws.CommunicationLogContext") CommunicationLogContext ctx) { return Optional.of(ctx); }

//     @Provides @Singleton @Named("Dpws.CommunicationLog")
//     Optional<CommunicationLogContext> provideOptAliasOptCommlogCtx(@Named("Dpws.CommunicationLog") CommunicationLogContext ctx) { return Optional.of(ctx); }

//     @Provides @Singleton
//     Supplier<CommunicationLogContext> provideSuppCommlogCtx(CommunicationLogContext ctx) { return () -> ctx; }

//     @Provides @Singleton @Named("Dpws.CommunicationLogContext")
//     Supplier<CommunicationLogContext> provideSuppNamedCommlogCtx(@Named("Dpws.CommunicationLogContext") CommunicationLogContext ctx) { return () -> ctx; }

//     @Provides @Singleton @Named("Dpws.CommunicationLog")
//     Supplier<CommunicationLogContext> provideSuppAliasCommlogCtx(@Named("Dpws.CommunicationLog") CommunicationLogContext ctx) { return () -> ctx; }

//     @Provides @Singleton
//     DeviceSettings provideDeviceSettings(@Named("Dpws.HttpServerPort") Integer httpPort,
//                                          @Named("org.somda.sdc.dpws.udp.BindAddress") String ip,
//                                          @Named("org.somda.sdc.dpws.NetworkInterface") NetworkInterface nic) {
//         final String eprUri = System.getProperty("sdc.epr", "urn:uuid:medstorm-sensor-1").trim();
//         final EndpointReferenceType epr = buildEpr(eprUri);
//         final String contextPath = deriveContextFromEpr(eprUri);
//         final int port = (httpPort != null && httpPort >= 0) ? httpPort : 0;
//         final InetSocketAddress httpBind = new InetSocketAddress(ip, port);

//         System.setProperty("Dpws.HttpHost", ip);
//         System.setProperty("Dpws.HttpPort", Integer.toString(port));
//         System.setProperty("Dpws.HttpServerBindAddress", ip + ":" + port);

//         return (DeviceSettings) Proxy.newProxyInstance(
//                 DeviceSettings.class.getClassLoader(),
//                 new Class[]{DeviceSettings.class},
//                 (p, m, a) -> {
//                     final String name = m.getName().toLowerCase(Locale.ROOT);
//                     final Class<?> rt = m.getReturnType();

//                     if (rt == Integer.class || rt == int.class) {
//                         if (name.contains("port")) return httpBind.getPort();
//                         return 0;
//                     }
//                     if (rt == Boolean.class || rt == boolean.class) {
//                         if (name.contains("https")) return false;
//                         if (name.contains("http"))  return true;
//                         return false;
//                     }
//                     if (rt == String.class) {
//                         if (name.contains("scheme")) return "http";
//                         if (name.contains("host"))   return ip;
//                         if (name.contains("context") || name.contains("basepath") || name.contains("path"))
//                             return contextPath;
//                         if (name.contains("epr") || name.contains("endpoint")) return eprUri;
//                         return "";
//                     }
//                     if (rt == InetSocketAddress.class) return httpBind;
//                     if (rt == InetAddress.class) {
//                         try { return InetAddress.getByName(ip); } catch (Exception ignored) { return null; }
//                     }
//                     if (rt == EndpointReferenceType.class) return epr;
//                     if (rt == NetworkInterface.class) return nic;

//                     if (Optional.class.isAssignableFrom(rt)) {
//                         if (name.contains("epr") || name.contains("endpoint")) return Optional.of(epr);
//                         if (name.contains("bind") || name.contains("addr") || name.contains("http")) return Optional.of(httpBind);
//                         if (name.contains("context") || name.contains("path")) return Optional.of(contextPath);
//                         if (name.contains("networkinterface") || name.equals("getnetworkinterface")) return Optional.of(nic);
//                         return Optional.empty();
//                     }
//                     return null;
//                 }
//         );
//     }

//     // ========= Helpers =========

    

//     private static Optional<NetworkInterface> resolveNic(String hintRaw) {
//         final String hint = hintRaw == null ? "" : hintRaw.trim();
//         if (hint.isEmpty()) return Optional.empty();

//         try {
//             NetworkInterface byName = NetworkInterface.getByName(hint);
//             if (byName != null) return Optional.of(byName);
//         } catch (Exception ignore) {}

//         try {
//             for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
//                 if (hint.equalsIgnoreCase(Optional.ofNullable(ni.getDisplayName()).orElse("")))
//                     return Optional.of(ni);
//             }
//         } catch (SocketException ignore) {}

//         try {
//             for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
//                 for (Enumeration<InetAddress> e = ni.getInetAddresses(); e.hasMoreElements();) {
//                     InetAddress a = e.nextElement();
//                     if (a instanceof Inet4Address && a.getHostAddress().equals(hint)) return Optional.of(ni);
//                 }
//             }
//         } catch (SocketException ignore) {}

//         try {
//             final String h = hint.toLowerCase(Locale.ROOT);
//             for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
//                 final String name = Optional.ofNullable(ni.getName()).orElse("").toLowerCase(Locale.ROOT);
//                 final String disp = Optional.ofNullable(ni.getDisplayName()).orElse("").toLowerCase(Locale.ROOT);
//                 final boolean wifiish = name.startsWith("wlan") || disp.contains("wi-fi") || disp.contains("wifi") || disp.contains("wireless");
//                 if (wifiish && (h.equals("wlan0") || h.equals("wi-fi") || h.equals("wifi"))) return Optional.of(ni);
//             }
//         } catch (SocketException ignore) {}

//         return Optional.empty();
//     }

//     private static Optional<InetAddress> firstIpv4(NetworkInterface nic) {
//         for (Enumeration<InetAddress> addrs = nic.getInetAddresses(); addrs.hasMoreElements();) {
//             InetAddress a = addrs.nextElement();
//             if (a instanceof Inet4Address && !a.isLoopbackAddress()) return Optional.of(a);
//         }
//         return Optional.empty();
//     }

//     private static int parseIntOr(String s, int fallback) {
//         try { return (s == null || s.isBlank()) ? fallback : Integer.parseInt(s.trim()); }
//         catch (NumberFormatException nfe) { return fallback; }
//     }

//     private static EndpointReferenceType buildEpr(String eprUri) {
//         final EndpointReferenceType epr = new EndpointReferenceType();
//         final AttributedURIType addr = new AttributedURIType();
//         addr.setValue(eprUri);
//         epr.setAddress(addr);
//         return epr;
//     }

//     private static String deriveContextFromEpr(String eprUri) {
//         if (eprUri == null || eprUri.isBlank()) return "/";
//         String tail = eprUri;
//         int i = tail.lastIndexOf(':');
//         if (i >= 0 && i < tail.length() - 1) tail = tail.substring(i + 1);
//         tail = tail.replaceAll("[^A-Za-z0-9._-]", "-");
//         if (!tail.startsWith("/")) tail = "/" + tail;
//         return tail;
//     }

//     private static WsdlProvisioningMode resolveWsdlProvisioningMode() {
//         final String[] candidates = {"STATIC", "EMBEDDED", "INLINE", "INLINED", "INCLUDE", "EXTERNAL"};
//         for (String c : candidates) {
//             try { return WsdlProvisioningMode.valueOf(c); }
//             catch (IllegalArgumentException ignored) {}
//         }
//         final WsdlProvisioningMode[] vals = WsdlProvisioningMode.values();
//         return vals.length > 0 ? vals[0] : null;
//     }

//     private static final class NoTlsCryptoSettings implements CryptoSettings {
//         @Override public Optional<InputStream> getKeyStoreStream() { return Optional.empty(); }
//         @Override public String getKeyStorePassword() { return null; }
//         @Override public Optional<InputStream> getTrustStoreStream() { return Optional.empty(); }
//         @Override public String getTrustStorePassword() { return null; }
//     }
// }



package com.medstorm.sdcbridge;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.*;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;


import org.somda.sdc.dpws.CommunicationLogContext;

import org.somda.sdc.biceps.common.storage.DescriptionPreprocessingSegment;
import org.somda.sdc.biceps.common.storage.StatePreprocessingSegment;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.CommunicationLogContext;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.sdc.dpws.device.DeviceSettings;
import org.somda.sdc.dpws.http.HttpConnectionInterceptor;
import org.somda.sdc.dpws.http.jetty.JettyHttpServerHandler;
import org.somda.sdc.dpws.http.jetty.factory.JettyHttpServerHandlerFactory;
import org.somda.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.wsdl.WsdlProvisioningMode;

import org.eclipse.jetty.util.ssl.SslContextFactory.Server;






public final class MedstormSdcriConfigModule extends AbstractModule {
    private static final Logger log = LoggerFactory.getLogger(MedstormSdcriConfigModule.class);

    @Override
    protected void configure() {
        log.info("[MedstormSdcriConfigModule] installing config module");

        // IPv4 + Datagram on Windows
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv4Addresses", "true");
        System.setProperty("jdk.net.usePlainDatagramSocketImpl", "true");

        final String nicHintRaw = System.getProperty("sdc.nic", "").trim();
        final int httpPort = parseIntOr(System.getProperty("Dpws.HttpServerPort", ""), 53200);

        final NetworkInterface nic = resolveNic(nicHintRaw).orElseThrow(() -> {
            final String msg = "Could not resolve NetworkInterface from sdc.nic='" + nicHintRaw + "'";
            log.error("[MedstormSdcriConfigModule] {}", msg);
            return new IllegalStateException(msg);
        });
        final String nicName = nic.getName();
        final String nicDisplay = Optional.ofNullable(nic.getDisplayName()).orElse(nicName);
        final InetAddress ipv4 = firstIpv4(nic).orElseThrow(() -> {
            final String msg = "No IPv4 address found on NIC '" + nicName + "' (" + nicDisplay + ")";
            log.error("[MedstormSdcriConfigModule] {}", msg);
            return new IllegalStateException(msg);
        });

        log.info("[MedstormSdcriConfigModule] binding NIC '{}' (ipv4={})", nicName, ipv4.getHostAddress());
        log.info("[MedstormSdcriConfigModule] Jetty HTTP bind pinned to {}:{}", ipv4.getHostAddress(), httpPort);

        // System properties some SDCri parts read directly
        System.setProperty("Dpws.Udp.NetworkInterfaceName", nicName);
        System.setProperty("Dpws.NetworkInterfaceName", nicName);
        System.setProperty("Dpws.NetworkInterface", nicName);
        System.setProperty("Dpws.Udp.AdapterName", nicName);
        System.setProperty("WsDiscovery.NetworkInterfaceName", nicName);
        System.setProperty("WsDiscovery.AdapterName", nicName);

        System.setProperty("org.somda.sdc.dpws.udp.NetworkInterfaceName", nicName);
        System.setProperty("org.somda.sdc.dpws.NetworkInterfaceName", nicName);
        System.setProperty("org.somda.sdc.dpws.wsdiscovery.NetworkInterfaceName", nicName);

        System.setProperty("Dpws.Udp.BindAddress", ipv4.getHostAddress());
        System.setProperty("org.somda.sdc.dpws.udp.BindAddress", ipv4.getHostAddress());
        System.setProperty("dpws.udp.bindaddress", ipv4.getHostAddress());
        System.setProperty("dpws.networkinterfacename", nicName);

        System.setProperty("Dpws.HttpHost", ipv4.getHostAddress());
        System.setProperty("Dpws.HttpPort", Integer.toString(httpPort));
        System.setProperty("Dpws.HttpServerBindAddress", ipv4.getHostAddress() + ":" + httpPort);

        // Bind strings/addresses/nic
        bind(String.class).annotatedWith(Names.named("Dpws.NetworkInterfaceName")).toInstance(nicName);
        bind(String.class).annotatedWith(Names.named("org.somda.sdc.dpws.NetworkInterfaceName")).toInstance(nicName);
        bind(String.class).annotatedWith(Names.named("Dpws.Udp.NetworkInterfaceName")).toInstance(nicName);
        bind(String.class).annotatedWith(Names.named("org.somda.sdc.dpws.udp.NetworkInterfaceName")).toInstance(nicName);
        bind(String.class).annotatedWith(Names.named("WsDiscovery.NetworkInterfaceName")).toInstance(nicName);
        bind(String.class).annotatedWith(Names.named("org.somda.sdc.dpws.wsdiscovery.NetworkInterfaceName")).toInstance(nicName);

        bind(String.class).annotatedWith(Names.named("Dpws.BindAddress")).toInstance(ipv4.getHostAddress());
        bind(String.class).annotatedWith(Names.named("org.somda.sdc.dpws.BindAddress")).toInstance(ipv4.getHostAddress());
        bind(String.class).annotatedWith(Names.named("Dpws.Udp.BindAddress")).toInstance(ipv4.getHostAddress());
        bind(String.class).annotatedWith(Names.named("org.somda.sdc.dpws.udp.BindAddress")).toInstance(ipv4.getHostAddress());

        bind(NetworkInterface.class).annotatedWith(Names.named("Dpws.NetworkInterface")).toInstance(nic);
        bind(NetworkInterface.class).annotatedWith(Names.named("org.somda.sdc.dpws.NetworkInterface")).toInstance(nic);
        bind(NetworkInterface.class).annotatedWith(Names.named("Dpws.Udp.NetworkInterface")).toInstance(nic);
        bind(NetworkInterface.class).annotatedWith(Names.named("org.somda.sdc.dpws.udp.NetworkInterface")).toInstance(nic);

        bind(new TypeLiteral<Optional<NetworkInterface>>(){}).annotatedWith(Names.named("Dpws.NetworkInterface")).toInstance(Optional.of(nic));
        bind(new TypeLiteral<Optional<NetworkInterface>>(){}).annotatedWith(Names.named("org.somda.sdc.dpws.NetworkInterface")).toInstance(Optional.of(nic));
        bind(new TypeLiteral<Optional<NetworkInterface>>(){}).annotatedWith(Names.named("Dpws.Udp.NetworkInterface")).toInstance(Optional.of(nic));
        bind(new TypeLiteral<Optional<NetworkInterface>>(){}).annotatedWith(Names.named("org.somda.sdc.dpws.udp.NetworkInterface")).toInstance(Optional.of(nic));

        bind(new TypeLiteral<Optional<InetAddress>>(){}).annotatedWith(Names.named("Dpws.Udp.BindAddress")).toInstance(Optional.of(ipv4));

        bind(Integer.class).annotatedWith(Names.named("Dpws.HttpServerPort")).toInstance(httpPort);

        
        

        // HTTP flags
        bind(Boolean.class).annotatedWith(Names.named("Dpws.EnableHttp")).toInstance(true);
        bind(Boolean.class).annotatedWith(Names.named("Dpws.EnableHttps")).toInstance(false);
        bind(Boolean.class).annotatedWith(Names.named("Dpws.EnforceHttpChunked")).toInstance(false);
        bind(Boolean.class).annotatedWith(Names.named("Dpws.GzipCompression")).toInstance(false);
        bind(Boolean.class).annotatedWith(Names.named("Dpws.ServerEnableJmx")).toInstance(false);
        bind(Boolean.class).annotatedWith(Names.named("Dpws.ClientRetryPost")).toInstance(false);
        bind(Boolean.class).annotatedWith(Names.named("Dpws.Client.AutoResolve")).toInstance(false);

        // Communication log toggles
        // Enable comm-log so factory supplies non-null CommunicationLog to Jetty handler
System.setProperty("Dpws.ServerCommlogInHandler", "true");
bind(Boolean.class).annotatedWith(Names.named("Dpws.ServerCommlogInHandler")).toInstance(true);





        bind(Boolean.class).annotatedWith(Names.named("Dpws.CommunicationLogPrettyPrintXml")).toInstance(false);
        bind(Boolean.class).annotatedWith(Names.named("Dpws.CommunicationLogWithHttpHeaders")).toInstance(false);
        bind(Boolean.class).annotatedWith(Names.named("Dpws.CommunicationLogWithRequestResponseId")).toInstance(false);

        // SOAP/JAXB
        bind(String.class).annotatedWith(Names.named("Dpws.HttpCharset")).toInstance("UTF-8");
        bind(String.class).annotatedWith(Names.named("Common.InstanceIdentifier")).toInstance("medstorm-sdcbridge-instance");
        bind(File.class).annotatedWith(Names.named("Dpws.CommunicationLogSinkDirectory")).toInstance(new File("logs/sdc"));

        bind(String.class).annotatedWith(Names.named("SoapConfig.JaxbContextPath")).toInstance("org.somda.sdc.dpws.soap.model");
        bind(String.class).annotatedWith(Names.named("SoapConfig.JaxbSchemaPath")).toInstance("");
        bind(String.class).annotatedWith(Names.named("SoapConfig.NamespaceMappings")).toInstance("");
        bind(Boolean.class).annotatedWith(Names.named("SoapConfig.MetadataComment")).toInstance(false);
        bind(Boolean.class).annotatedWith(Names.named("SoapConfig.ValidateSoapMessages")).toInstance(false);

        // TLS placeholders (HTTPS disabled)
        bind(String[].class).annotatedWith(Names.named("Dpws.Crypto.TlsEnabledVersions"))
                .toInstance(new String[]{"TLSv1.2", "TLSv1.3"});
        bind(String[].class).annotatedWith(Names.named("Dpws.Crypto.TlsEnabledCiphers"))
                .toInstance(new String[]{"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"});
        bind(CryptoSettings.class).annotatedWith(Names.named("Dpws.Crypto.Settings")).toInstance(new NoTlsCryptoSettings());
        bind(HostnameVerifier.class).annotatedWith(Names.named("Dpws.Crypto.ClientHostnameVerifier")).toInstance((h, s) -> true);
        bind(HostnameVerifier.class).annotatedWith(Names.named("Dpws.Crypto.DeviceHostnameVerifier")).toInstance((h, s) -> true);

        // Pools, TTL, queue sizes
        bind(Integer.class).annotatedWith(Names.named("Dpws.ClientPoolSize")).toInstance(4);
        bind(Integer.class).annotatedWith(Names.named("Dpws.GzipCompressionMinSize")).toInstance(1024);
        bind(Integer.class).annotatedWith(Names.named("Dpws.ServerThreadPoolSize")).toInstance(4);
        bind(Integer.class).annotatedWith(Names.named("Dpws.MulticastTtl")).toInstance(4);
        bind(Integer.class).annotatedWith(Names.named("SoapConfig.NotificationQueueCapacity")).toInstance(1024);
        bind(Integer.class).annotatedWith(Names.named("WsAddressing.MessageIdCacheSize")).toInstance(100);
        bind(Integer.class).annotatedWith(Names.named("WsDiscovery.MaxProbeMatchesBufferSize")).toInstance(64);
        bind(Integer.class).annotatedWith(Names.named("WsDiscovery.MaxResolveMatchesBufferSize")).toInstance(64);

        bind(Duration.class).annotatedWith(Names.named("Dpws.HttpClientConnectTimeout")).toInstance(Duration.ofSeconds(10));
        bind(Duration.class).annotatedWith(Names.named("Dpws.HttpClientReadTimeout")).toInstance(Duration.ofSeconds(15));
        bind(Duration.class).annotatedWith(Names.named("Dpws.HttpServerConnectionTimeout")).toInstance(Duration.ofSeconds(30));
        bind(Duration.class).annotatedWith(Names.named("Dpws.MaxWaitForFutures")).toInstance(Duration.ofSeconds(10));
        bind(Duration.class).annotatedWith(Names.named("Dpws.Client.MaxWaitForResolveMatches")).toInstance(Duration.ofSeconds(5));
        bind(Duration.class).annotatedWith(Names.named("WsDiscovery.MaxWaitForProbeMatches")).toInstance(Duration.ofSeconds(5));
        bind(Duration.class).annotatedWith(Names.named("WsDiscovery.MaxWaitForResolveMatches")).toInstance(Duration.ofSeconds(5));

        bind(String.class).annotatedWith(Names.named("WsEventing.Source.SubscriptionManagerPath")).toInstance("/SubscriptionManager");
        bind(Duration.class).annotatedWith(Names.named("WsEventing.Source.MaxExpires")).toInstance(Duration.ofMinutes(10));
        bind(Boolean.class).annotatedWith(Names.named("WsAddressing.IgnoreMessageIds")).toInstance(false);

        bind(SslContextFactory.Server.class).toInstance(new SslContextFactory.Server());

        // ===== HTTP connection interceptor shapes =====
        final HttpConnectionInterceptor noopInterceptor = new HttpConnectionInterceptor() {};
        bind(HttpConnectionInterceptor.class).toInstance(noopInterceptor);
        bind(HttpConnectionInterceptor.class)
                .annotatedWith(Names.named("Dpws.HttpConnectionInterceptor"))
                .toInstance(noopInterceptor);

        List<HttpConnectionInterceptor> interceptorList = Collections.singletonList(noopInterceptor);
        bind(new TypeLiteral<List<HttpConnectionInterceptor>>() {}).toInstance(interceptorList);
        bind(new TypeLiteral<List<HttpConnectionInterceptor>>() {})
                .annotatedWith(Names.named("Dpws.HttpConnectionInterceptors"))
                .toInstance(interceptorList);

        Set<HttpConnectionInterceptor> interceptorSet = new LinkedHashSet<>(interceptorList);
        bind(new TypeLiteral<Set<HttpConnectionInterceptor>>() {}).toInstance(interceptorSet);
        bind(new TypeLiteral<Set<HttpConnectionInterceptor>>() {})
                .annotatedWith(Names.named("Dpws.HttpConnectionInterceptors"))
                .toInstance(interceptorSet);

        // Glue/BICEPS toggles
        bind(Boolean.class).annotatedWith(Names.named("Biceps.Common.CopyMdibInput")).toInstance(true);
        bind(Boolean.class).annotatedWith(Names.named("Biceps.Common.CopyMdibOutput")).toInstance(true);
        bind(Boolean.class).annotatedWith(Names.named("Biceps.Common.StoreNotAssociatedContextStates")).toInstance(true);
        bind(Boolean.class).annotatedWith(Names.named("SdcGlue.Consumer.ApplyReportsWithSameMdibVersion")).toInstance(true);

        // Glue timeouts
        bind(Duration.class).annotatedWith(Names.named("SdcGlue.Consumer.RequestedExpires")).toInstance(Duration.ofMinutes(1));
        bind(Duration.class).annotatedWith(Names.named("SdcGlue.Consumer.AwaitingTransactionTimeout")).toInstance(Duration.ofSeconds(30));
        bind(Duration.class).annotatedWith(Names.named("SdcGlue.Consumer.WatchdogPeriod")).toInstance(Duration.ofSeconds(10));

        // Preprocessing segments
        bind(new TypeLiteral<List<Class<? extends DescriptionPreprocessingSegment>>>() {})
                .annotatedWith(Names.named("Biceps.Common.ConsumerDescriptionPreprocessingSegments"))
                .toInstance(Collections.emptyList());
        bind(new TypeLiteral<List<Class<? extends DescriptionPreprocessingSegment>>>() {})
                .annotatedWith(Names.named("Biceps.Common.ProviderDescriptionPreprocessingSegments"))
                .toInstance(Collections.emptyList());
        bind(new TypeLiteral<List<Class<? extends StatePreprocessingSegment>>>() {})
                .annotatedWith(Names.named("Biceps.Common.ConsumerStatePreprocessingSegments"))
                .toInstance(Collections.emptyList());
        bind(new TypeLiteral<List<Class<? extends StatePreprocessingSegment>>>() {})
                .annotatedWith(Names.named("Biceps.Common.ProviderStatePreprocessingSegments"))
                .toInstance(Collections.emptyList());

        // WSDL provisioning (robust to enum diffs)
        bind(WsdlProvisioningMode.class)
                .annotatedWith(Names.named("Dpws.WsdlProvisioningMode"))
                .toInstance(resolveWsdlProvisioningMode());

        log.info("[MedstormSdcriConfigModule] All required Guice bindings configured.");
    }

    // ========= Providers =========

    

    @Provides @Singleton
    CommunicationLog provideCommunicationLog() {
        return (CommunicationLog) Proxy.newProxyInstance(
                CommunicationLog.class.getClassLoader(),
                new Class[]{CommunicationLog.class},
                (Object p, Method m, Object[] a) -> {
                    Class<?> rt = m.getReturnType();
                    if (!rt.isPrimitive()) return null;
                    if (rt == boolean.class) return false;
                    if (rt == byte.class)    return (byte) 0;
                    if (rt == short.class)   return (short) 0;
                    if (rt == int.class)     return 0;
                    if (rt == long.class)    return 0L;
                    if (rt == float.class)   return 0f;
                    if (rt == double.class)  return 0d;
                    if (rt == char.class)    return '\0';
                    return null; // void
                }
        );
    }

    @Provides @Singleton @Named("Dpws.CommunicationLog")
    CommunicationLog provideNamedCommunicationLog(CommunicationLog cl) { return cl; }






@Provides
@Singleton
CommunicationLogContext provideCommunicationLogContext() {
    // Allow override via -DDpws.CommunicationLogContext=...
    final String name = System.getProperty("Dpws.CommunicationLogContext", "medstorm");
    return new CommunicationLogContext(name);
}

@Provides
@Singleton
@Named("Dpws.CommunicationLogContext")
CommunicationLogContext provideNamedCommunicationLogContext(CommunicationLogContext ctx) {
    return ctx;
}



    @Provides @Singleton @Named("Dpws.CommunicationLog")
    CommunicationLogContext provideContextAliasUnderCommlogName(CommunicationLogContext ctx) { return ctx; }

    // Optionals / Suppliers
    @Provides @Singleton
    Optional<CommunicationLog> provideOptCommlog(CommunicationLog cl) { return Optional.of(cl); }

    @Provides @Singleton @Named("Dpws.CommunicationLog")
    Optional<CommunicationLog> provideOptCommlogNamed(@Named("Dpws.CommunicationLog") CommunicationLog cl) { return Optional.of(cl); }

    @Provides @Singleton
    Supplier<CommunicationLog> provideCommlogSupplier(CommunicationLog cl) { return () -> cl; }

    @Provides @Singleton @Named("Dpws.CommunicationLog")
    Supplier<CommunicationLog> provideCommlogSupplierNamed(@Named("Dpws.CommunicationLog") CommunicationLog cl) { return () -> cl; }

    @Provides @Singleton
    Optional<CommunicationLogContext> provideOptCommlogCtx(CommunicationLogContext ctx) { return Optional.of(ctx); }

    @Provides @Singleton @Named("Dpws.CommunicationLogContext")
    Optional<CommunicationLogContext> provideOptNamedOptCommlogCtx(@Named("Dpws.CommunicationLogContext") CommunicationLogContext ctx) { return Optional.of(ctx); }

    @Provides @Singleton @Named("Dpws.CommunicationLog")
    Optional<CommunicationLogContext> provideOptAliasOptCommlogCtx(@Named("Dpws.CommunicationLog") CommunicationLogContext ctx) { return Optional.of(ctx); }

    @Provides @Singleton
    Supplier<CommunicationLogContext> provideSuppCommlogCtx(CommunicationLogContext ctx) { return () -> ctx; }

    @Provides @Singleton @Named("Dpws.CommunicationLogContext")
    Supplier<CommunicationLogContext> provideSuppNamedCommlogCtx(@Named("Dpws.CommunicationLogContext") CommunicationLogContext ctx) { return () -> ctx; }

    @Provides @Singleton @Named("Dpws.CommunicationLog")
    Supplier<CommunicationLogContext> provideSuppAliasCommlogCtx(@Named("Dpws.CommunicationLog") CommunicationLogContext ctx) { return () -> ctx; }

    @Provides @Singleton
    DeviceSettings provideDeviceSettings(@Named("Dpws.HttpServerPort") Integer httpPort,
                                         @Named("org.somda.sdc.dpws.udp.BindAddress") String ip,
                                         @Named("org.somda.sdc.dpws.NetworkInterface") NetworkInterface nic) {
        final String eprUri = System.getProperty("sdc.epr", "urn:uuid:medstorm-sensor-1").trim();
        final EndpointReferenceType epr = buildEpr(eprUri);
        final String contextPath = deriveContextFromEpr(eprUri);
        final int port = (httpPort != null && httpPort >= 0) ? httpPort : 0;
        final InetSocketAddress httpBind = new InetSocketAddress(ip, port);

        System.setProperty("Dpws.HttpHost", ip);
        System.setProperty("Dpws.HttpPort", Integer.toString(port));
        System.setProperty("Dpws.HttpServerBindAddress", ip + ":" + port);

        return (DeviceSettings) Proxy.newProxyInstance(
                DeviceSettings.class.getClassLoader(),
                new Class[]{DeviceSettings.class},
                (p, m, a) -> {
                    final String name = m.getName().toLowerCase(Locale.ROOT);
                    final Class<?> rt = m.getReturnType();

                    if (rt == Integer.class || rt == int.class) {
                        if (name.contains("port")) return httpBind.getPort();
                        return 0;
                    }
                    if (rt == Boolean.class || rt == boolean.class) {
                        if (name.contains("https")) return false;
                        if (name.contains("http"))  return true;
                        return false;
                    }
                    if (rt == String.class) {
                        if (name.contains("scheme")) return "http";
                        if (name.contains("host"))   return ip;
                        if (name.contains("context") || name.contains("basepath") || name.contains("path"))
                            return contextPath;
                        if (name.contains("epr") || name.contains("endpoint")) return eprUri;
                        return "";
                    }
                    if (rt == InetSocketAddress.class) return httpBind;
                    if (rt == InetAddress.class) {
                        try { return InetAddress.getByName(ip); } catch (Exception ignored) { return null; }
                    }
                    if (rt == EndpointReferenceType.class) return epr;
                    if (rt == NetworkInterface.class) return nic;

                    if (Optional.class.isAssignableFrom(rt)) {
                        if (name.contains("epr") || name.contains("endpoint")) return Optional.of(epr);
                        if (name.contains("bind") || name.contains("addr") || name.contains("http")) return Optional.of(httpBind);
                        if (name.contains("context") || name.contains("path")) return Optional.of(contextPath);
                        if (name.contains("networkinterface") || name.equals("getnetworkinterface")) return Optional.of(nic);
                        return Optional.empty();
                    }
                    return null;
                }
        );
    }

    // ========= Helpers =========

    

    private static Optional<NetworkInterface> resolveNic(String hintRaw) {
        final String hint = hintRaw == null ? "" : hintRaw.trim();
        if (hint.isEmpty()) return Optional.empty();

        try {
            NetworkInterface byName = NetworkInterface.getByName(hint);
            if (byName != null) return Optional.of(byName);
        } catch (Exception ignore) {}

        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (hint.equalsIgnoreCase(Optional.ofNullable(ni.getDisplayName()).orElse("")))
                    return Optional.of(ni);
            }
        } catch (SocketException ignore) {}

        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (Enumeration<InetAddress> e = ni.getInetAddresses(); e.hasMoreElements();) {
                    InetAddress a = e.nextElement();
                    if (a instanceof Inet4Address && a.getHostAddress().equals(hint)) return Optional.of(ni);
                }
            }
        } catch (SocketException ignore) {}

        try {
            final String h = hint.toLowerCase(Locale.ROOT);
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                final String name = Optional.ofNullable(ni.getName()).orElse("").toLowerCase(Locale.ROOT);
                final String disp = Optional.ofNullable(ni.getDisplayName()).orElse("").toLowerCase(Locale.ROOT);
                final boolean wifiish = name.startsWith("wlan") || disp.contains("wi-fi") || disp.contains("wifi") || disp.contains("wireless");
                if (wifiish && (h.equals("wlan0") || h.equals("wi-fi") || h.equals("wifi"))) return Optional.of(ni);
            }
        } catch (SocketException ignore) {}

        return Optional.empty();
    }

    private static Optional<InetAddress> firstIpv4(NetworkInterface nic) {
        for (Enumeration<InetAddress> addrs = nic.getInetAddresses(); addrs.hasMoreElements();) {
            InetAddress a = addrs.nextElement();
            if (a instanceof Inet4Address && !a.isLoopbackAddress()) return Optional.of(a);
        }
        return Optional.empty();
    }

    private static int parseIntOr(String s, int fallback) {
        try { return (s == null || s.isBlank()) ? fallback : Integer.parseInt(s.trim()); }
        catch (NumberFormatException nfe) { return fallback; }
    }

    private static EndpointReferenceType buildEpr(String eprUri) {
        final EndpointReferenceType epr = new EndpointReferenceType();
        final AttributedURIType addr = new AttributedURIType();
        addr.setValue(eprUri);
        epr.setAddress(addr);
        return epr;
    }

    private static String deriveContextFromEpr(String eprUri) {
        if (eprUri == null || eprUri.isBlank()) return "/";
        String tail = eprUri;
        int i = tail.lastIndexOf(':');
        if (i >= 0 && i < tail.length() - 1) tail = tail.substring(i + 1);
        tail = tail.replaceAll("[^A-Za-z0-9._-]", "-");
        if (!tail.startsWith("/")) tail = "/" + tail;
        return tail;
    }

    private static WsdlProvisioningMode resolveWsdlProvisioningMode() {
        final String[] candidates = {"STATIC", "EMBEDDED", "INLINE", "INLINED", "INCLUDE", "EXTERNAL"};
        for (String c : candidates) {
            try { return WsdlProvisioningMode.valueOf(c); }
            catch (IllegalArgumentException ignored) {}
        }
        final WsdlProvisioningMode[] vals = WsdlProvisioningMode.values();
        return vals.length > 0 ? vals[0] : null;
    }

    private static final class NoTlsCryptoSettings implements CryptoSettings {
        @Override public Optional<InputStream> getKeyStoreStream() { return Optional.empty(); }
        @Override public String getKeyStorePassword() { return null; }
        @Override public Optional<InputStream> getTrustStoreStream() { return Optional.empty(); }
        @Override public String getTrustStorePassword() { return null; }
    }
}