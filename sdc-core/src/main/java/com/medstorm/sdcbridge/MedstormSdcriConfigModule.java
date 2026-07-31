

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
import org.somda.sdc.biceps.common.storage.DescriptionPreprocessingSegment;
import org.somda.sdc.biceps.common.storage.StatePreprocessingSegment;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.CommunicationLogContext;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.sdc.dpws.device.DeviceSettings;
import org.somda.sdc.dpws.http.HttpConnectionInterceptor;
import org.somda.sdc.dpws.soap.wsaddressing.model.AttributedURIType;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.wsdl.WsdlProvisioningMode;
import org.somda.sdc.glue.provider.sco.OperationInvocationReceiver;


import com.google.inject.Singleton;
// import org.somda.sdc.dpws.soap.SoapUtil;

// import com.medstorm.sdcbridge.patch.PatchedSoapUtil;



import com.medstorm.sdc.core.net.NetworkInterfaceResolver;

import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matchers;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;

import com.google.inject.matcher.Matchers;
import com.google.inject.spi.ProvisionListener;
import com.google.inject.spi.ProvisionListener.ProvisionInvocation;


import java.util.Arrays;



import javax.net.ssl.HostnameVerifier;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.*;
import java.net.*;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

public final class MedstormSdcriConfigModule extends AbstractModule {
    private static final Logger log = LoggerFactory.getLogger(MedstormSdcriConfigModule.class);

    private static final int DEFAULT_HTTP_PORT = 53200;

    @Override
    protected void configure() {
        log.info("[MedstormSdcriConfigModule] installing config module");

        // Windows multicast + IPv4 stability
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv4Addresses", "true");
        System.setProperty("jdk.net.usePlainDatagramSocketImpl", "true");

// ---- JAXB context path (must be computed BEFORE you set/bind it) ----
final List<String> ctx = new ArrayList<>(List.of(
    // DPWS
    "org.somda.sdc.dpws.soap.model",
    "org.somda.sdc.dpws.model",
    "org.somda.sdc.dpws.soap.wsaddressing.model",
    "org.somda.sdc.dpws.soap.wsdiscovery.model",
    "org.somda.sdc.dpws.soap.wseventing.model",
    "org.somda.sdc.dpws.soap.wstransfer.model",
    "org.somda.sdc.dpws.soap.wsmetadataexchange.model",
    "org.somda.sdc.dpws.wsdl.model",

    // BICEPS
    "org.somda.sdc.biceps.model.message",
    "org.somda.sdc.biceps.model.participant",
    "org.somda.sdc.biceps.model.extension"
));

// keep only packages that actually exist
ctx.removeIf(p -> !hasObjectFactory(p));

// de-dup while keeping order
final LinkedHashSet<String> ctxSet = new LinkedHashSet<>(ctx);
final ArrayList<String> ctxList = new ArrayList<>(ctxSet);

// IMPORTANT: this is the value JaxbMarshalling reads (SoapConfig.JaxbContextPath)
final String jaxbContextPath = String.join(":", ctxList);
log.info("[JAXB] SoapConfig.JaxbContextPath={}", jaxbContextPath);

// 1) System property (some builds read this)
setProp("SoapConfig.JaxbContextPath", jaxbContextPath);

// 2) Guice binding (some builds inject this exact @Named key)
bind(String.class).annotatedWith(Names.named("SoapConfig.JaxbContextPath"))
        .toInstance(jaxbContextPath);

// 3) Some builds use a list/set form
bind(new TypeLiteral<Set<String>>() {})
        .annotatedWith(Names.named("SoapConfig.JaxbContextPaths"))
        .toInstance(ctxSet);

bind(new TypeLiteral<List<String>>() {})
        .annotatedWith(Names.named("SoapConfig.JaxbContextPaths"))
        .toInstance(ctxList);

// Optional but safe defaults (avoid nulls in some stacks)
setProp("SoapConfig.NamespaceMappings", "");
setProp("SoapConfig.JaxbSchemaPath", "");
bind(String.class).annotatedWith(Names.named("SoapConfig.NamespaceMappings")).toInstance("");
bind(String.class).annotatedWith(Names.named("SoapConfig.JaxbSchemaPath")).toInstance("");

setProp("SoapConfig.ValidateSoapMessages", "false");
bind(Boolean.class).annotatedWith(Names.named("SoapConfig.ValidateSoapMessages")).toInstance(false);




        final String nicHint = System.getProperty("sdc.nic", "").trim();
        final int httpPort = parseIntOr(System.getProperty("Dpws.HttpServerPort", ""), DEFAULT_HTTP_PORT);

        final NetworkInterface nic = NetworkInterfaceResolver.resolve(nicHint).orElseThrow(() ->
                new IllegalStateException("Could not resolve a usable network interface from sdc.nic='"
                        + nicHint + "'. Usable interfaces on this host: "
                        + NetworkInterfaceResolver.describeUsableInterfaces()));

        // The resolver only returns interfaces that hold an IPv4 address, so this is a
        // guard against a NIC being reconfigured between resolution and use, not the
        // routine "wrong adapter picked" failure it used to be.
        final InetAddress ipv4 = NetworkInterfaceResolver.firstIpv4(nic).orElseThrow(() ->
                new IllegalStateException("NIC '" + nic.getName() + "' lost its IPv4 address during startup"));

        final String nicName = nic.getName();
        final String ip = ipv4.getHostAddress();
        final URI baseUri = URI.create("http://" + ip + ":" + httpPort);
        final InetSocketAddress httpBind = new InetSocketAddress(ip, httpPort);

        log.info("[MedstormSdcriConfigModule] NIC='{}' ip={} httpPort={}", nicName, ip, httpPort);
        log.info("[MedstormSdcriConfigModule] baseUri={}", baseUri);


        final String ANON = "anonymous";

// Keep it minimal: only bind *named* keys, never raw String.class
for (String key : Arrays.asList(
        "Dpws.DeviceDistinguishedName",
        "Dpws.ClientDistinguishedName",
        
        "Dpws.CallerId",
        "CallerId"
)) {
    bindConstant().annotatedWith(Names.named(key)).to(ANON);
}

// If you really need it as named (safe):
bindConstant().annotatedWith(Names.named("Dpws.CommunicationLogContext")).to("medstorm");



bindListener(Matchers.any(), new ProvisionListener() {
    @Override
    public <T> void onProvision(ProvisionInvocation<T> provision) {
        T obj = provision.provision(); // IMPORTANT: call through
        if (obj != null && obj.getClass().getName().equals(
                "org.somda.sdc.dpws.soap.wseventing.SourceSubscriptionManagerImpl")) {

            log.warn("[Guice] Provisioned {}", obj.getClass().getName());

            try {
                // dump a few private fields you care about
                dumpField(obj, "callerId");
                dumpField(obj, "subscriptionId");
                dumpField(obj, "notifyToUri");
                dumpField(obj, "notificationQueue");
                dumpField(obj, "notifyToSender");
                dumpField(obj, "endToSender");
            } catch (Exception e) {
                log.warn("[Guice] dump failed", e);
            }
        }
    }

    private void dumpField(Object obj, String fieldName) throws Exception {
        Field f = obj.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        Object v = f.get(obj);
        log.warn("[Guice]   {} = {}", fieldName, v);
    }
});







        // --- System properties (SDCri reads some settings directly) ---
        // NIC / UDP
        setProp("Dpws.Udp.NetworkInterfaceName", nicName);
        setProp("Dpws.NetworkInterfaceName", nicName);
        setProp("Dpws.NetworkInterface", nicName);
        setProp("Dpws.Udp.AdapterName", nicName);
        setProp("WsDiscovery.NetworkInterfaceName", nicName);
        setProp("WsDiscovery.AdapterName", nicName);

        setProp("org.somda.sdc.dpws.udp.NetworkInterfaceName", nicName);
        setProp("org.somda.sdc.dpws.NetworkInterfaceName", nicName);
        setProp("org.somda.sdc.dpws.wsdiscovery.NetworkInterfaceName", nicName);

        setProp("Dpws.Udp.BindAddress", ip);
        setProp("org.somda.sdc.dpws.udp.BindAddress", ip);
        setProp("dpws.udp.bindaddress", ip);
        setProp("dpws.networkinterfacename", nicName);

        // HTTP (pin port & URI)
        setProp("Dpws.HttpHost", ip);
        setProp("Dpws.HttpPort", Integer.toString(httpPort));
        setProp("Dpws.HttpServerPort", Integer.toString(httpPort));
        setProp("Dpws.HttpServerBindAddress", ip + ":" + httpPort);
        setProp("Dpws.HttpServerUri", baseUri.toString());

        // --- Guice bindings for the same values (newer builds often prefer DI) ---
        bindCommonNetworkBindings(nic, nicName, ipv4, ip);
        bindCommonHttpBindings(ip, httpPort, httpBind, baseUri);

    //     bind(SoapUtil.class)
    // .to(org.somda.sdc.dpws.soap.PatchedSoapUtil.class)
    // .in(Singleton.class);

    // bind(org.somda.sdc.dpws.soap.SoapUtil.class)
    //     .to(com.medstorm.sdcbridge.PatchedSoapUtil.class)
    //     .in(com.google.inject.Singleton.class);


    // bind(SoapUtil.class).to(PatchedSoapUtil.class).in(com.google.inject.Singleton.class);


        // Minimal HTTP flags you already want
        bind(Boolean.class).annotatedWith(Names.named("Dpws.EnableHttp")).toInstance(true);
        bind(Boolean.class).annotatedWith(Names.named("Dpws.EnableHttps")).toInstance(false);
        bind(Boolean.class).annotatedWith(Names.named("Dpws.EnforceHttpChunked")).toInstance(false);
        bind(Boolean.class).annotatedWith(Names.named("Dpws.GzipCompression")).toInstance(false);
        bind(Boolean.class).annotatedWith(Names.named("Dpws.ServerEnableJmx")).toInstance(false);

        // Communication log toggles (prevents null factory paths in some builds)
        setProp("Dpws.ServerCommlogInHandler", "true");
        bind(Boolean.class).annotatedWith(Names.named("Dpws.ServerCommlogInHandler")).toInstance(true);

        bind(Boolean.class).annotatedWith(Names.named("Dpws.CommunicationLogPrettyPrintXml")).toInstance(false);
        bind(Boolean.class).annotatedWith(Names.named("Dpws.CommunicationLogWithHttpHeaders")).toInstance(false);
        bind(Boolean.class).annotatedWith(Names.named("Dpws.CommunicationLogWithRequestResponseId")).toInstance(false);

        // SOAP/JAXB basics
        bind(String.class).annotatedWith(Names.named("Dpws.HttpCharset")).toInstance("UTF-8");
        bind(String.class).annotatedWith(Names.named("Common.InstanceIdentifier")).toInstance("medstorm-sdcbridge-instance");
        bind(File.class).annotatedWith(Names.named("Dpws.CommunicationLogSinkDirectory")).toInstance(new File("logs/sdc"));

        bind(Boolean.class).annotatedWith(Names.named("SoapConfig.MetadataComment")).toInstance(false);
        // bind(Boolean.class).annotatedWith(Names.named("SoapConfig.ValidateSoapMessages")).toInstance(false);

        // TLS placeholders (HTTPS disabled)
        bind(String[].class).annotatedWith(Names.named("Dpws.Crypto.TlsEnabledVersions"))
                .toInstance(new String[]{"TLSv1.2", "TLSv1.3"});
        bind(String[].class).annotatedWith(Names.named("Dpws.Crypto.TlsEnabledCiphers"))
                .toInstance(new String[]{
                        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"
                });
        bind(CryptoSettings.class).annotatedWith(Names.named("Dpws.Crypto.Settings")).toInstance(new NoTlsCryptoSettings());
        bind(HostnameVerifier.class).annotatedWith(Names.named("Dpws.Crypto.ClientHostnameVerifier")).toInstance((h, s) -> true);
        bind(HostnameVerifier.class).annotatedWith(Names.named("Dpws.Crypto.DeviceHostnameVerifier")).toInstance((h, s) -> true);

        // Threading / timeouts (keep small but sane)
        bind(Integer.class).annotatedWith(Names.named("Dpws.ServerThreadPoolSize")).toInstance(4);
        bind(Integer.class).annotatedWith(Names.named("Dpws.MulticastTtl")).toInstance(4);

        bind(Duration.class).annotatedWith(Names.named("Dpws.HttpClientConnectTimeout")).toInstance(Duration.ofSeconds(10));
        bind(Duration.class).annotatedWith(Names.named("Dpws.HttpClientReadTimeout")).toInstance(Duration.ofSeconds(15));
        bind(Duration.class).annotatedWith(Names.named("Dpws.HttpServerConnectionTimeout")).toInstance(Duration.ofSeconds(30));

        bind(SslContextFactory.Server.class).toInstance(new SslContextFactory.Server());

        // Interceptor shapes (avoid missing multi-bindings)
        final HttpConnectionInterceptor noop = new HttpConnectionInterceptor() {};
        bind(HttpConnectionInterceptor.class).toInstance(noop);
        bind(HttpConnectionInterceptor.class).annotatedWith(Names.named("Dpws.HttpConnectionInterceptor")).toInstance(noop);

        final List<HttpConnectionInterceptor> list = Collections.singletonList(noop);
        bind(new TypeLiteral<List<HttpConnectionInterceptor>>() {}).toInstance(list);
        bind(new TypeLiteral<List<HttpConnectionInterceptor>>() {})
                .annotatedWith(Names.named("Dpws.HttpConnectionInterceptors"))
                .toInstance(list);

        final Set<HttpConnectionInterceptor> set = new LinkedHashSet<>(list);
        bind(new TypeLiteral<Set<HttpConnectionInterceptor>>() {}).toInstance(set);
        bind(new TypeLiteral<Set<HttpConnectionInterceptor>>() {})
                .annotatedWith(Names.named("Dpws.HttpConnectionInterceptors"))
                .toInstance(set);

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

        // ---- These were in  older version; keep them to satisfy DefaultDpwsModule ----
bind(Boolean.class).annotatedWith(Names.named("Dpws.ClientRetryPost")).toInstance(false);

bind(Integer.class).annotatedWith(Names.named("Dpws.ClientPoolSize")).toInstance(4);
bind(Integer.class).annotatedWith(Names.named("Dpws.GzipCompressionMinSize")).toInstance(1024);

bind(Boolean.class).annotatedWith(Names.named("WsAddressing.IgnoreMessageIds")).toInstance(false);
bind(Integer.class).annotatedWith(Names.named("WsAddressing.MessageIdCacheSize")).toInstance(100);

bind(Duration.class).annotatedWith(Names.named("Dpws.MaxWaitForFutures")).toInstance(Duration.ofSeconds(10));

// --- Missing bindings required by DefaultDpwsModule (must exist even if feature disabled) ---
bind(Boolean.class).annotatedWith(Names.named("Dpws.ClientRetryPost")).toInstance(false);
bind(Boolean.class).annotatedWith(Names.named("WsAddressing.IgnoreMessageIds")).toInstance(false);

bind(Integer.class).annotatedWith(Names.named("Dpws.ClientPoolSize")).toInstance(4);
bind(Integer.class).annotatedWith(Names.named("Dpws.GzipCompressionMinSize")).toInstance(1024);
bind(Integer.class).annotatedWith(Names.named("WsAddressing.MessageIdCacheSize")).toInstance(100);

bind(Duration.class).annotatedWith(Names.named("Dpws.MaxWaitForFutures")).toInstance(Duration.ofSeconds(10));


// WsDiscovery buffer sizes (some builds require these)
bind(Integer.class).annotatedWith(Names.named("WsDiscovery.MaxProbeMatchesBufferSize")).toInstance(64);
bind(Integer.class).annotatedWith(Names.named("WsDiscovery.MaxResolveMatchesBufferSize")).toInstance(64);

// Some builds also want these timeouts
bind(Duration.class).annotatedWith(Names.named("WsDiscovery.MaxWaitForProbeMatches")).toInstance(Duration.ofSeconds(5));
bind(Duration.class).annotatedWith(Names.named("WsDiscovery.MaxWaitForResolveMatches")).toInstance(Duration.ofSeconds(5));
bind(Duration.class).annotatedWith(Names.named("Dpws.Client.MaxWaitForResolveMatches")).toInstance(Duration.ofSeconds(5));


// ---- BICEPS defaults required by DefaultBicepsModule ----
bind(Boolean.class).annotatedWith(Names.named("Biceps.Common.CopyMdibInput")).toInstance(true);
bind(Boolean.class).annotatedWith(Names.named("Biceps.Common.CopyMdibOutput")).toInstance(true);
bind(Boolean.class).annotatedWith(Names.named("Biceps.Common.StoreNotAssociatedContextStates")).toInstance(true);

// ---- DPWS client helper defaults ----
bind(Boolean.class).annotatedWith(Names.named("Dpws.Client.AutoResolve")).toInstance(false);

// ---- WS-Eventing defaults required by DefaultDpwsModule ----
// ---- WS-Eventing defaults required by DefaultDpwsModule ----
bindConstant()
    .annotatedWith(Names.named("SoapConfig.NotificationQueueCapacity"))
    .to(1024);


setProp("SoapConfig.NotificationQueueCapacity", "1024");



// ---- FORCE enable WS-Eventing ----
setProp("Dpws.EnableEventing", "true");
setProp("WsEventing.Enable", "true");
setProp("WsEventing.Source.Enable", "true");

// Guice bindings (some builds read from DI, not System.getProperty)
bind(Boolean.class).annotatedWith(Names.named("Dpws.EnableEventing")).toInstance(true);
bind(Boolean.class).annotatedWith(Names.named("WsEventing.Enable")).toInstance(true);
bind(Boolean.class).annotatedWith(Names.named("WsEventing.Source.Enable")).toInstance(true);

// IMPORTANT: no leading "/" here
bind(String.class)
    .annotatedWith(Names.named("WsEventing.Source.SubscriptionManagerPath"))
    .toInstance("SubscriptionManager");

// Also publish as system property (some SDCri builds read it from System.getProperty)
setProp("WsEventing.Source.SubscriptionManagerPath", "SubscriptionManager");






bind(Duration.class).annotatedWith(Names.named("WsEventing.Source.MaxExpires")).toInstance(Duration.ofMinutes(10));

// ---- Glue consumer defaults required by DefaultGlueModule ----
bind(Duration.class).annotatedWith(Names.named("SdcGlue.Consumer.AwaitingTransactionTimeout")).toInstance(Duration.ofSeconds(30));
bind(Duration.class).annotatedWith(Names.named("SdcGlue.Consumer.WatchdogPeriod")).toInstance(Duration.ofSeconds(10));




        // WSDL provisioning mode (robust)
        bind(WsdlProvisioningMode.class)
                .annotatedWith(Names.named("Dpws.WsdlProvisioningMode"))
                .toInstance(resolveWsdlProvisioningMode());

        log.info("[MedstormSdcriConfigModule] configured.");
    }

    // ---------------- Providers ----------------

    @Provides @Singleton
    OperationInvocationReceiver provideOperationInvocationReceiver() {
        return nopProxy(OperationInvocationReceiver.class);
    }

    @Provides @Singleton
    CommunicationLog provideCommunicationLog() {
        return nopProxy(CommunicationLog.class);
    }

    @Provides @Singleton
    CommunicationLogContext provideCommunicationLogContext() {
        final String name = System.getProperty("Dpws.CommunicationLogContext", "medstorm");
        return new CommunicationLogContext(name);
    }

    @Provides @Singleton @Named("Dpws.CommunicationLog")
    CommunicationLog provideNamedCommunicationLog(CommunicationLog cl) { return cl; }

    @Provides @Singleton @Named("Dpws.CommunicationLogContext")
    CommunicationLogContext provideNamedCommunicationLogContext(CommunicationLogContext ctx) { return ctx; }

    @Provides @Singleton
    Optional<CommunicationLog> provideOptCommlog(CommunicationLog cl) { return Optional.of(cl); }

    @Provides @Singleton
    Supplier<CommunicationLog> provideCommlogSupplier(CommunicationLog cl) { return () -> cl; }

    @Provides @Singleton
    Optional<CommunicationLogContext> provideOptCommlogCtx(CommunicationLogContext ctx) { return Optional.of(ctx); }

    @Provides @Singleton
    Supplier<CommunicationLogContext> provideCommlogCtxSupplier(CommunicationLogContext ctx) { return () -> ctx; }

    /**
     * CRITICAL: This provider pins the HTTP server port/URI even when SDCri calls different getters
     * across versions. This is the piece that should stop "http://IP:0".
     */
    @Provides @Singleton
    DeviceSettings provideDeviceSettings(
            @Named("Dpws.HttpServerPort") Integer httpPort,
            @Named("org.somda.sdc.dpws.udp.BindAddress") String ip,
            @Named("org.somda.sdc.dpws.NetworkInterface") NetworkInterface nic) {

        final String eprUri = System.getProperty("sdc.epr", "urn:uuid:medstorm-sensor-1").trim();
        final EndpointReferenceType epr = buildEpr(eprUri);
        final String contextPath = deriveContextFromEpr(eprUri);

        final int port = (httpPort != null && httpPort > 0) ? httpPort : DEFAULT_HTTP_PORT;
        final InetSocketAddress bind = new InetSocketAddress(ip, port);
        final URI uri = URI.create("http://" + ip + ":" + port);

        // re-publish to be loud & consistent
        setProp("Dpws.HttpHost", ip);
        setProp("Dpws.HttpPort", Integer.toString(port));
        setProp("Dpws.HttpServerPort", Integer.toString(port));
        setProp("Dpws.HttpServerBindAddress", ip + ":" + port);
        setProp("Dpws.HttpServerUri", uri.toString());

        return (DeviceSettings) java.lang.reflect.Proxy.newProxyInstance(
                DeviceSettings.class.getClassLoader(),
                new Class<?>[]{DeviceSettings.class},
                (proxy, method, args) -> {
                    final String n = method.getName();
                    final String nl = n.toLowerCase(Locale.ROOT);
                    final Class<?> rt = method.getReturnType();

                    // exact getters (common)
                    if (n.equals("getHttpServerPort") || n.equals("getHttpPort")) return coerceNumber(rt, port);
                    if (n.equals("getHttpServerUri") || n.equals("getHttpBaseUri") || n.equals("getHttpServerAddress")) return coerceUri(rt, uri);
                    if (n.equals("getHttpBindAddress") || n.equals("getHttpBindSocketAddress")) return bind;
                    if (n.equals("getNetworkInterface")) return nic;
                    if (n.equals("getEprAddress") || n.equals("getEndpointReference") || n.equals("getEpr")) {
                        if (rt == String.class) return eprUri;
                        if (rt == EndpointReferenceType.class) return epr;
                        if (Optional.class.isAssignableFrom(rt)) return Optional.of(epr);
                    }

                    // fuzzy matches (different SDCri versions)
                    if (isNumber(rt) && nl.contains("port")) return coerceNumber(rt, port);

                    if (rt == URI.class || rt == URL.class) return coerceUri(rt, uri);
                    if (rt == String.class) {
                        if (nl.contains("uri") || nl.contains("address")) return uri.toString();
                        if (nl.contains("scheme")) return "http";
                        if (nl.contains("host")) return ip;
                        if (nl.contains("context") || nl.contains("basepath") || nl.contains("path")) return contextPath;
                        if (nl.contains("epr") || nl.contains("endpoint")) return eprUri;
                        return "";
                    }

                    if (rt == InetSocketAddress.class) return bind;
                    if (rt == InetAddress.class) return InetAddress.getByName(ip);
                    if (rt == EndpointReferenceType.class) return epr;
                    if (rt == NetworkInterface.class) return nic;

                    if (Optional.class.isAssignableFrom(rt)) {
                        if (nl.contains("port")) return Optional.of(port);
                        if (nl.contains("uri") || nl.contains("address")) return Optional.of(uri);
                        if (nl.contains("bind")) return Optional.of(bind);
                        if (nl.contains("epr") || nl.contains("endpoint")) return Optional.of(epr);
                        if (nl.contains("networkinterface")) return Optional.of(nic);
                        return Optional.empty();
                    }

                    if (rt == boolean.class || rt == Boolean.class) {
                        if (nl.contains("https")) return false;
                        if (nl.contains("http")) return true;
                        return false;
                    }

                    return safeDefaultFor(rt);
                }
        );
    }







    // ---------------- helpers ----------------

    // private static void bindCommonNetworkBindings(NetworkInterface nic, String nicName, InetAddress ipv4, String ip) {
    //     // NOTE: called from configure() – uses the module's binder()
    // }


private static boolean hasObjectFactory(String pkg) {
    String res = pkg.replace('.', '/') + "/ObjectFactory.class";
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    return cl != null && cl.getResource(res) != null;
}





    private void bindCommonNetworkBindings(NetworkInterface nic, String nicName, InetAddress ipv4, String ip) {
        // names for NIC
        bind(String.class).annotatedWith(Names.named("Dpws.NetworkInterfaceName")).toInstance(nicName);
        bind(String.class).annotatedWith(Names.named("Dpws.Udp.NetworkInterfaceName")).toInstance(nicName);
        bind(String.class).annotatedWith(Names.named("WsDiscovery.NetworkInterfaceName")).toInstance(nicName);

        bind(String.class).annotatedWith(Names.named("org.somda.sdc.dpws.NetworkInterfaceName")).toInstance(nicName);
        bind(String.class).annotatedWith(Names.named("org.somda.sdc.dpws.udp.NetworkInterfaceName")).toInstance(nicName);
        bind(String.class).annotatedWith(Names.named("org.somda.sdc.dpws.wsdiscovery.NetworkInterfaceName")).toInstance(nicName);

        // bind address
        bind(String.class).annotatedWith(Names.named("Dpws.BindAddress")).toInstance(ip);
        bind(String.class).annotatedWith(Names.named("Dpws.Udp.BindAddress")).toInstance(ip);
        bind(String.class).annotatedWith(Names.named("org.somda.sdc.dpws.BindAddress")).toInstance(ip);
        bind(String.class).annotatedWith(Names.named("org.somda.sdc.dpws.udp.BindAddress")).toInstance(ip);

        // NIC objects
        bind(NetworkInterface.class).annotatedWith(Names.named("Dpws.NetworkInterface")).toInstance(nic);
        bind(NetworkInterface.class).annotatedWith(Names.named("Dpws.Udp.NetworkInterface")).toInstance(nic);
        bind(NetworkInterface.class).annotatedWith(Names.named("org.somda.sdc.dpws.NetworkInterface")).toInstance(nic);
        bind(NetworkInterface.class).annotatedWith(Names.named("org.somda.sdc.dpws.udp.NetworkInterface")).toInstance(nic);

        bind(new TypeLiteral<Optional<NetworkInterface>>(){}).annotatedWith(Names.named("Dpws.NetworkInterface")).toInstance(Optional.of(nic));
        bind(new TypeLiteral<Optional<NetworkInterface>>(){}).annotatedWith(Names.named("Dpws.Udp.NetworkInterface")).toInstance(Optional.of(nic));
        bind(new TypeLiteral<Optional<NetworkInterface>>(){}).annotatedWith(Names.named("org.somda.sdc.dpws.NetworkInterface")).toInstance(Optional.of(nic));
        bind(new TypeLiteral<Optional<NetworkInterface>>(){}).annotatedWith(Names.named("org.somda.sdc.dpws.udp.NetworkInterface")).toInstance(Optional.of(nic));

        bind(new TypeLiteral<Optional<InetAddress>>(){}).annotatedWith(Names.named("Dpws.Udp.BindAddress")).toInstance(Optional.of(ipv4));
    }

    private void bindCommonHttpBindings(String ip, int port, InetSocketAddress bind, URI baseUri) {
        // common DPWS keys (String + Integer + int)
        bind(String.class).annotatedWith(Names.named("Dpws.HttpHost")).toInstance(ip);
        bind(Integer.class).annotatedWith(Names.named("Dpws.HttpPort")).toInstance(port);
        bind(Integer.class).annotatedWith(Names.named("Dpws.HttpServerPort")).toInstance(port);
        bind(int.class).annotatedWith(Names.named("Dpws.HttpPort")).toInstance(port);
        bind(int.class).annotatedWith(Names.named("Dpws.HttpServerPort")).toInstance(port);

        bind(String.class).annotatedWith(Names.named("Dpws.HttpServerBindAddress")).toInstance(ip + ":" + port);
        bind(InetSocketAddress.class).annotatedWith(Names.named("Dpws.HttpServerBindSocketAddress")).toInstance(bind);

        bind(URI.class).annotatedWith(Names.named("Dpws.HttpServerUri")).toInstance(baseUri);
        bind(String.class).annotatedWith(Names.named("Dpws.HttpServerUri")).toInstance(baseUri.toString());

        // org.somda.* variants some builds use
        bind(String.class).annotatedWith(Names.named("org.somda.sdc.dpws.HttpHost")).toInstance(ip);
        bind(Integer.class).annotatedWith(Names.named("org.somda.sdc.dpws.HttpServerPort")).toInstance(port);
        bind(String.class).annotatedWith(Names.named("org.somda.sdc.dpws.HttpServerPort")).toInstance(Integer.toString(port));

        bind(URI.class).annotatedWith(Names.named("org.somda.sdc.dpws.HttpServerUri")).toInstance(baseUri);
        bind(String.class).annotatedWith(Names.named("org.somda.sdc.dpws.HttpServerUri")).toInstance(baseUri.toString());
    }

    private static void setProp(String k, String v) {
        if (v == null) return;
        System.setProperty(k, v);
    }

    private static <T> T nopProxy(Class<T> iface) {
        return iface.cast(java.lang.reflect.Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class<?>[]{iface},
                (p, m, a) -> safeDefaultFor(m.getReturnType())
        ));
    }

    private static Object safeDefaultFor(Class<?> rt) {
        if (rt == void.class) return null;
        if (rt.isPrimitive()) {
            if (rt == boolean.class) return false;
            if (rt == byte.class) return (byte) 0;
            if (rt == short.class) return (short) 0;
            if (rt == int.class) return 0;
            if (rt == long.class) return 0L;
            if (rt == float.class) return 0f;
            if (rt == double.class) return 0d;
            if (rt == char.class) return '\0';
        }
        if (Optional.class.isAssignableFrom(rt)) return Optional.empty();
        if (List.class.isAssignableFrom(rt)) return Collections.emptyList();
        if (Set.class.isAssignableFrom(rt)) return Collections.emptySet();
        if (Map.class.isAssignableFrom(rt)) return Collections.emptyMap();
        if (CharSequence.class.isAssignableFrom(rt)) return "";
        return null;
    }

    private static boolean isNumber(Class<?> rt) {
        return rt == int.class || rt == Integer.class || rt == long.class || rt == Long.class;
    }

    private static Object coerceNumber(Class<?> rt, int port) {
        if (rt == int.class || rt == Integer.class) return port;
        if (rt == long.class || rt == Long.class) return (long) port;
        return port;
    }

    private static Object coerceUri(Class<?> rt, URI uri) throws MalformedURLException {
        if (rt == URI.class) return uri;
        if (rt == URL.class) return uri.toURL();
        return uri;
    }

    // private static Optional<NetworkInterface> resolveNic(String hintRaw) {
    //     final String hint = hintRaw == null ? "" : hintRaw.trim();
    //     if (hint.isEmpty()) return Optional.empty();
    //     try {
    //         NetworkInterface byName = NetworkInterface.getByName(hint);
    //         if (byName != null) return Optional.of(byName);
    //     } catch (Exception ignore) {}

    //     try {
    //         for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
    //             if (hint.equalsIgnoreCase(Optional.ofNullable(ni.getDisplayName()).orElse(""))) return Optional.of(ni);
    //         }
    //     } catch (SocketException ignore) {}

    //     // try IP match
    //     try {
    //         for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
    //             for (Enumeration<InetAddress> e = ni.getInetAddresses(); e.hasMoreElements();) {
    //                 InetAddress a = e.nextElement();
    //                 if (a instanceof Inet4Address && a.getHostAddress().equals(hint)) return Optional.of(ni);
    //             }
    //         }
    //     } catch (SocketException ignore) {}

    //     return Optional.empty();
    // }

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
        WsdlProvisioningMode[] vals = WsdlProvisioningMode.values();
        return vals.length > 0 ? vals[0] : null;
    }

    private static final class NoTlsCryptoSettings implements CryptoSettings {
        @Override public Optional<InputStream> getKeyStoreStream() { return Optional.empty(); }
        @Override public String getKeyStorePassword() { return null; }
        @Override public Optional<InputStream> getTrustStoreStream() { return Optional.empty(); }
        @Override public String getTrustStorePassword() { return null; }
    }
}
