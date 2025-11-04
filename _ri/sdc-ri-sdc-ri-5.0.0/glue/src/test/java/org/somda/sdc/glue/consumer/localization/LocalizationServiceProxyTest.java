package org.somda.sdc.glue.consumer.localization;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.model.message.GetLocalizedText;
import org.somda.sdc.biceps.model.message.GetLocalizedTextResponse;
import org.somda.sdc.biceps.model.message.GetSupportedLanguages;
import org.somda.sdc.biceps.model.message.GetSupportedLanguagesResponse;
import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.glue.UnitTestUtil;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.glue.consumer.localization.factory.LocalizationServiceProxyFactory;
import org.somda.sdc.glue.consumer.sco.InvocationException;
import test.org.somda.common.LoggingTestWatcher;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(LoggingTestWatcher.class)
class LocalizationServiceProxyTest {
    private static final UnitTestUtil UT = new UnitTestUtil();
    private final HostingServiceProxy hostingServiceProxy = mock(HostingServiceProxy.class);
    private final HostedServiceProxy hostedServiceProxy = mock(HostedServiceProxy.class);
    private final RequestResponseClient requestResponseClient = mock(RequestResponseClient.class);
    private LocalizationServiceProxy localizationServiceProxy;
    private SoapUtil soapUtil;

    @BeforeAll
    static void beforeAll() {
        // start required thread pool(s)
        UT.getInjector().getInstance(Key.get(
                new TypeLiteral<ExecutorWrapperService<ListeningExecutorService>>() {
                },
                org.somda.sdc.glue.guice.Consumer.class
        )).startAsync().awaitRunning();
    }

    @BeforeEach
    void beforeEach() throws MarshallingException, InterceptorException, TransportException, SoapFaultException {
        LocalizationServiceProxyFactory localizationServiceProxyFactory =
                UT.getInjector().getInstance(LocalizationServiceProxyFactory.class);
        localizationServiceProxy = localizationServiceProxyFactory
                .createLocalizationServiceProxy(hostingServiceProxy, hostedServiceProxy);

        soapUtil = UT.getInjector().getInstance(SoapUtil.class);

        when(requestResponseClient.sendRequestResponse(any())).thenAnswer(invocation ->
                mockSoapResponse(invocation.getArgument(0, SoapMessage.class)));
        when(hostedServiceProxy.getRequestResponseClient()).thenReturn(requestResponseClient);
    }

    @Test
    void getLocalizedText() throws ExecutionException, InterruptedException, MarshallingException,
            InterceptorException, TransportException, SoapFaultException, InvocationException {
        { // first request to the proxy will call cache prefetch for version & language
            var request = createRequest(List.of("en"), List.of("ref1"));
            var response = localizationServiceProxy.getLocalizedText(request);

            assertEquals(response.get().getText().size(), 1);
            assertEquals(response.get().getText().get(0).getRef(), "ref1");
            verify(requestResponseClient, times(1)).sendRequestResponse(any());
        }

        { // second request will same version and language will not send SOAP request anymore and use cache
            clearInvocations(requestResponseClient);
            var request = createRequest(List.of("en"), List.of("ref2"));
            var response = localizationServiceProxy.getLocalizedText(request);

            assertEquals(response.get().getText().size(), 1);
            assertEquals(response.get().getText().get(0).getRef(), "ref2");
            verify(requestResponseClient, never()).sendRequestResponse(any());
        }

        { // request with not cached language will trigger SOAP request
            clearInvocations(requestResponseClient);
            var request = createRequest(List.of("de"), List.of("ref1"));
            var response = localizationServiceProxy.getLocalizedText(request);

            assertEquals(response.get().getText().size(), 1);
            assertEquals(response.get().getText().get(0).getRef(), "ref1");
            assertEquals(response.get().getText().get(0).getLang(), "de");
            verify(requestResponseClient, times(1)).sendRequestResponse(any());
        }

        { // request with multiple languages which are cached will not trigger SOAP request and use cache only
            clearInvocations(requestResponseClient);
            var request = createRequest(List.of("en", "de"), List.of("ref1"));
            var response = localizationServiceProxy.getLocalizedText(request);

            assertEquals(response.get().getText().size(), 2);
            // not called since both versions were cached before
            verify(requestResponseClient, never()).sendRequestResponse(any());
        }
    }

    @Test
    void getLocalizedTextEmptyLang() throws ExecutionException, InterruptedException, MarshallingException,
            InterceptorException, TransportException, SoapFaultException, InvocationException {
        { // request with empty languages list will trigger cache if version cache was not fully pre-fetched
            localizationServiceProxy.cachePrefetch(BigInteger.ONE, List.of("en"));
            verify(requestResponseClient, times(1)).sendRequestResponse(any());

            clearInvocations(requestResponseClient);
            var request = createRequest(Collections.emptyList(), List.of("ref1"));
            var response = localizationServiceProxy.getLocalizedText(request);

            assertEquals(response.get().getText().size(), 2);
            verify(requestResponseClient, times(1)).sendRequestResponse(any());
        }

        { // request with empty languages list will not trigger cache anymore if version is fully cached
            clearInvocations(requestResponseClient);
            localizationServiceProxy.cachePrefetch(BigInteger.ONE);
            verify(requestResponseClient, times(1)).sendRequestResponse(any());

            clearInvocations(requestResponseClient);
            var request = createRequest(Collections.emptyList(), List.of("ref1"));
            var response = localizationServiceProxy.getLocalizedText(request);

            assertEquals(response.get().getText().size(), 2);
            // not called since version cache is fully pre-fetched
            verify(requestResponseClient, never()).sendRequestResponse(any());
        }
    }

    @Test
    void getSupportedLanguages() throws ExecutionException, InterruptedException {
        var request = new GetSupportedLanguages();
        var response = localizationServiceProxy.getSupportedLanguages(request);
        assertEquals(response.get().getLang().size(), 2);
    }

    private SoapMessage mockSoapResponse(SoapMessage argument) {
        var action = argument.getWsAddressingHeader().getAction().get().getValue();

        if (ActionConstants.ACTION_GET_LOCALIZED_TEXT.equals(action)) {
            var body = soapUtil.getBody(argument, GetLocalizedText.class).orElseThrow();
            return soapUtil.createMessage(
                    ActionConstants.ACTION_GET_LOCALIZED_TEXT,
                    createGetLocalizedTextResponse(body.getLang(), body.getRef()));
        }

        if (ActionConstants.ACTION_GET_SUPPORTED_LANGUAGES.equals(action)) {
            return soapUtil.createMessage(
                    ActionConstants.ACTION_GET_SUPPORTED_LANGUAGES,
                    createSupportedLanguagesResponse());
        }

        return null;
    }

    private GetLocalizedText createRequest(List<String> lang, @Nullable List<String> ref) {
        var getLocalizedText = new GetLocalizedText();
        getLocalizedText.setVersion(BigInteger.ONE);
        getLocalizedText.setLang(lang);
        getLocalizedText.setRef(ref);
        return getLocalizedText;
    }

    private GetLocalizedTextResponse createGetLocalizedTextResponse(List<String> languages, List<String> refs) {

        var text1 = new LocalizedText();
        text1.setVersion(BigInteger.ONE);
        text1.setLang("en");
        text1.setRef("ref1");

        var text2 = new LocalizedText();
        text2.setVersion(BigInteger.ONE);
        text2.setLang("en");
        text2.setRef("ref2");

        var text3 = new LocalizedText();
        text3.setVersion(BigInteger.ONE);
        text3.setLang("de");
        text3.setRef("ref1");

        var response = new GetLocalizedTextResponse();
        // filter mock texts based on provided languages and ref list
        var texts = Stream.of(text1, text2, text3)
                .filter(text -> languages.isEmpty() || languages.contains(text.getLang()))
                .filter(text -> refs.isEmpty() || refs.contains(text.getRef()))
                .collect(Collectors.toList());

        response.setText(texts);

        return response;
    }

    private GetSupportedLanguagesResponse createSupportedLanguagesResponse() {
        var response = new GetSupportedLanguagesResponse();
        response.setLang(List.of("en", "de"));
        return response;
    }
}