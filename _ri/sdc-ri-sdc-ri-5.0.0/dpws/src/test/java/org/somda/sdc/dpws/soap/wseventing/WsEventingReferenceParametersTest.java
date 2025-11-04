package org.somda.sdc.dpws.soap.wseventing;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.common.util.JaxbUtil;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.HttpServerRegistryMock;
import org.somda.sdc.dpws.LocalAddressResolverMock;
import org.somda.sdc.dpws.TransportBindingFactoryMock;
import org.somda.sdc.dpws.device.helper.RequestResponseServerHttpHandler;
import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.guice.NetworkJobThreadPool;
import org.somda.sdc.dpws.helper.JaxbMarshalling;
import org.somda.sdc.dpws.http.HttpException;
import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.http.HttpServerRegistry;
import org.somda.sdc.dpws.model.HostedServiceType;
import org.somda.sdc.dpws.model.ObjectFactory;
import org.somda.sdc.dpws.network.LocalAddressResolver;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.NotificationSink;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.dpws.soap.RequestResponseServer;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.factory.NotificationSinkFactory;
import org.somda.sdc.dpws.soap.factory.RequestResponseClientFactory;
import org.somda.sdc.dpws.soap.interception.Direction;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingConstants;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingServerInterceptor;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.ReferenceParametersType;
import org.somda.sdc.dpws.soap.wseventing.factory.EventSourceInterceptorDispatcherFactory;
import org.somda.sdc.dpws.soap.wseventing.factory.SubscriptionManagerFactory;
import org.somda.sdc.dpws.soap.wseventing.factory.SubscriptionRegistryFactory;
import org.somda.sdc.dpws.soap.wseventing.factory.WsEventingEventSinkFactory;
import org.somda.sdc.dpws.soap.wseventing.factory.WsEventingFaultFactory;
import org.somda.sdc.dpws.soap.wseventing.helper.EventSourceUtil;
import org.somda.sdc.dpws.soap.wseventing.helper.SubscriptionRegistry;
import org.somda.sdc.dpws.soap.wseventing.model.SubscribeResponse;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WsEventingReferenceParametersTest extends DpwsTest {

    private static final String HOST = "mock-host";
    private static final Integer PORT = 8080;
    private static final String HOSTED_SERVICE_PATH = "/hosted-service";
    private static final String ACTION = "http://action";
    private static final Duration MAX_EXPIRES = Duration.ofHours(3);
    private static final Duration FUTURE_WAIT = Duration.ofSeconds(1);

    private static final String REFERENCE = "my_secret_is_cofveve";


    private EventSink wseSink;
    private NotificationSink notificationSink;

    @BeforeEach
    public void setUp() throws Exception {
        overrideBindings(List.of(new DpwsModuleReplacements()));
        super.setUp();

        // start required thread pool(s)
        getInjector().getInstance(Key.get(
                new TypeLiteral<ExecutorWrapperService<ListeningExecutorService>>() {
                },
                NetworkJobThreadPool.class
        )).startAsync().awaitRunning();
        getInjector().getInstance(JaxbMarshalling.class).startAsync().awaitRunning();
        getInjector().getInstance(SoapMarshalling.class).startAsync().awaitRunning();

        WsAddressingUtil wsaUtil = getInjector().getInstance(WsAddressingUtil.class);
        ObjectFactory dpwsFactory = getInjector().getInstance(ObjectFactory.class);
        final var wseSource = getInjector()
                .getInstance(EventSourceInterceptorDispatcherFactory.class)
                .create(List.of(getInjector().getInstance(ActionBasedEventSource.class)));
        RequestResponseServer reqResSrv = getInjector().getInstance(RequestResponseServer.class);
        reqResSrv.register(wseSource);
        notificationSink = getInjector().getInstance(NotificationSinkFactory.class).createNotificationSink(
                getInjector().getInstance(WsAddressingServerInterceptor.class));

        HttpServerRegistry httpSrvRegisty = getInjector().getInstance(HttpServerRegistry.class);

        var uri = "http://" + HOST + ":" + PORT;
        var hostedServiceUri = httpSrvRegisty.registerContext(uri, true, HOSTED_SERVICE_PATH, null, null, new HttpHandler() {
            @Override
            public void handle(InputStream inStream, OutputStream outStream, CommunicationContext communicationContext) throws HttpException {
                MarshallingHelper.handleRequestResponse(getInjector(), reqResSrv, inStream, outStream, communicationContext);
            }
        });

        HostedServiceType hst = dpwsFactory.createHostedServiceType();
        hst.getEndpointReference().add(wsaUtil.createEprWithAddress(hostedServiceUri));

        RequestResponseClientFactory rrcFactory = getInjector().getInstance(RequestResponseClientFactory.class);
        TransportBindingFactory tbFactory = getInjector().getInstance(TransportBindingFactory.class);
        RequestResponseClient rrc = rrcFactory.createRequestResponseClient(
                tbFactory.createTransportBinding(hostedServiceUri, null));

        wseSink = getInjector().getInstance(WsEventingEventSinkFactory.class)
                .createWsEventingEventSink(rrc, "http://localhost:1234", null);

    }

    void verifyReferenceParam(SettableFuture<Collection<Element>> future) throws Exception {
        Collection<Element> incomingRefParm;
        incomingRefParm = future.get(FUTURE_WAIT.toSeconds(), TimeUnit.SECONDS);

        assertEquals(1, incomingRefParm.size());
        var firstFind = incomingRefParm.stream().findFirst();
        assertTrue(firstFind.isPresent());
        Element element = firstFind.get();

        assertEquals(1, element.getChildNodes().getLength());
        assertEquals(REFERENCE, element.getTextContent());
        assertTrue(element.hasAttributeNS(
                        WsAddressingConstants.IS_REFERENCE_PARAMETER.getNamespaceURI(),
                        WsAddressingConstants.IS_REFERENCE_PARAMETER.getLocalPart()
                )
        );
    }

    @Test
    void referenceParameterInUnsubscribe() throws Exception {
        assertFalse(EventSourceInterceptorDispatcherMock.unsubscribeRefParam.isDone());

        Duration expectedExpires = Duration.ofHours(1);
        ListenableFuture<SubscribeResult> resInfo = wseSink.subscribe(
                DpwsConstants.WS_EVENTING_SUPPORTED_DIALECT,
                Collections.singletonList(ACTION),
                expectedExpires,
                notificationSink
        );
        assertThat("Granted expires duration", resInfo.get().getGrantedExpires(), is(expectedExpires));

        wseSink.getStatus(resInfo.get().getSubscriptionId()).get();

        wseSink.unsubscribe(resInfo.get().getSubscriptionId()).get();

        verifyReferenceParam(EventSourceInterceptorDispatcherMock.unsubscribeRefParam);
    }

    @Test
    void referenceParameterInGetStatus() throws Exception {
        assertFalse(EventSourceInterceptorDispatcherMock.getStatusRefParam.isDone());

        Duration expectedExpires = Duration.ofHours(1);
        ListenableFuture<SubscribeResult> resInfo = wseSink.subscribe(
                DpwsConstants.WS_EVENTING_SUPPORTED_DIALECT,
                Collections.singletonList(ACTION),
                expectedExpires,
                notificationSink
        );
        assertThat("Granted expires duration", resInfo.get().getGrantedExpires(), is(expectedExpires));

        wseSink.getStatus(resInfo.get().getSubscriptionId()).get();

        wseSink.unsubscribe(resInfo.get().getSubscriptionId()).get();

        verifyReferenceParam(EventSourceInterceptorDispatcherMock.getStatusRefParam);
    }

    @Test
    void referenceParameterInRenew() throws Exception {
        assertFalse(EventSourceInterceptorDispatcherMock.renewRefParam.isDone());

        Duration expectedExpires = Duration.ofHours(1);
        ListenableFuture<SubscribeResult> resInfo = wseSink.subscribe(
                DpwsConstants.WS_EVENTING_SUPPORTED_DIALECT,
                Collections.singletonList(ACTION),
                expectedExpires,
                notificationSink
        );
        assertThat("Granted expires duration", resInfo.get().getGrantedExpires(), is(expectedExpires));

        wseSink.getStatus(resInfo.get().getSubscriptionId()).get();

        wseSink.renew(resInfo.get().getSubscriptionId(), MAX_EXPIRES).get();

        wseSink.unsubscribe(resInfo.get().getSubscriptionId()).get();

        verifyReferenceParam(EventSourceInterceptorDispatcherMock.renewRefParam);
    }

    public static class EventSourceInterceptorDispatcherMock extends EventSourceInterceptorDispatcher {

        static SettableFuture<Collection<Element>> unsubscribeRefParam = SettableFuture.create();
        static SettableFuture<Collection<Element>> getStatusRefParam = SettableFuture.create();
        static SettableFuture<Collection<Element>> renewRefParam = SettableFuture.create();
        private final SoapUtil soapUtil;

        @AssistedInject
        EventSourceInterceptorDispatcherMock(
                @Assisted Collection<EventSourceDialectHandler> eventSources,
                SoapUtil soapUtil,
                WsEventingFaultFactory faultFactory,
                JaxbUtil jaxbUtil,
                WsAddressingUtil wsaUtil,
                org.somda.sdc.dpws.soap.wseventing.model.ObjectFactory wseFactory,
                SubscriptionRegistryFactory subscriptionRegistryFactory,
                SubscriptionManagerFactory subscriptionManagerFactory,
                EventSourceUtil eventSourceUtil
        ) {
            super(eventSources, soapUtil, faultFactory, eventSourceUtil, jaxbUtil, wsaUtil, wseFactory, subscriptionRegistryFactory, subscriptionManagerFactory, "abcd");
            this.soapUtil = soapUtil;

            // reset the futures on every instantiation to avoid side effects
            unsubscribeRefParam = SettableFuture.create();
            getStatusRefParam = SettableFuture.create();
            renewRefParam = SettableFuture.create();
        }

        @Override
        @MessageInterceptor(value = WsEventingConstants.WSA_ACTION_SUBSCRIBE, direction = Direction.REQUEST)
        void processSubscribe(RequestResponseObject rrObj) throws SoapFaultException {
            super.processSubscribe(rrObj);

            ReferenceParametersType referenceParameters = new ReferenceParametersType();
            // create some random child element
            var fac = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder;
            try {
                builder = fac.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e);
            }
            var doc = builder.newDocument();

            var root = doc.createElementNS("ftp://namespace.example.com", "MyFunkyRoot");
            root.setTextContent(REFERENCE);

            referenceParameters.setAny(List.of(root));

            SubscribeResponse body = soapUtil.getBody(rrObj.getResponse(), SubscribeResponse.class)
                    .orElseThrow(() -> new RuntimeException("err"));

            body.getSubscriptionManager().setReferenceParameters(referenceParameters);

            soapUtil.setBody(body, rrObj.getResponse());
        }


        @Override
        @MessageInterceptor(value = WsEventingConstants.WSA_ACTION_UNSUBSCRIBE, direction = Direction.REQUEST)
        void processUnsubscribe(RequestResponseObject rrObj) throws SoapFaultException {
            super.processUnsubscribe(rrObj);

            // find any reference parameters
            var refParm = rrObj.getRequest().getWsAddressingHeader().getMappedReferenceParameters();
            refParm.ifPresent(refParmContent -> unsubscribeRefParam.set(refParmContent));
        }

        @Override
        @MessageInterceptor(value = WsEventingConstants.WSA_ACTION_GET_STATUS, direction = Direction.REQUEST)
        void processGetStatus(RequestResponseObject rrObj) throws SoapFaultException {
            super.processGetStatus(rrObj);

            // find any reference parameters
            var refParm = rrObj.getRequest().getWsAddressingHeader().getMappedReferenceParameters();
            refParm.ifPresent(refParmContent -> getStatusRefParam.set(refParmContent));
        }

        @Override
        @MessageInterceptor(value = WsEventingConstants.WSA_ACTION_RENEW, direction = Direction.REQUEST)
        void processRenew(RequestResponseObject rrObj) throws SoapFaultException {
            super.processRenew(rrObj);

            // find any reference parameters
            var refParm = rrObj.getRequest().getWsAddressingHeader().getMappedReferenceParameters();
            refParm.ifPresent(refParmContent -> renewRefParam.set(refParmContent));
        }
    }

    private static class DpwsModuleReplacements extends AbstractModule {
        @Override
        protected void configure() {
            TransportBindingFactoryMock.setHandlerRegistry(HttpServerRegistryMock.getRegistry());
            install(new FactoryModuleBuilder()
                    .implement(EventSourceInterceptorDispatcher.class, EventSourceInterceptorDispatcherMock.class)
                    .build(EventSourceInterceptorDispatcherFactory.class));
            bind(Duration.class)
                    .annotatedWith(Names.named(WsEventingConfig.SOURCE_MAX_EXPIRES))
                    .toInstance(Duration.ofHours(3));
            bind(HttpServerRegistry.class)
                    .to(HttpServerRegistryMock.class);
            bind(TransportBindingFactory.class)
                    .to(TransportBindingFactoryMock.class);
            bind(LocalAddressResolver.class).to(LocalAddressResolverMock.class);
        }
    }
}
