package org.somda.sdc.dpws;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.inject.Inject;
import org.apache.http.HttpHeaders;
import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.http.HttpException;
import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;
import org.somda.sdc.dpws.soap.SoapConstants;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.TransportInfo;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.factory.SoapMessageFactory;
import org.somda.sdc.dpws.soap.model.Envelope;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TransportBindingFactoryMock implements TransportBindingFactory {
    private static Map<String, HttpHandler> handlerRegistry;
    private final SoapMarshalling soapMarshalling;
    private final SoapMessageFactory soapMessageFactory;
    private final CommunicationContext mockCommunicationContext;

    @Inject
    TransportBindingFactoryMock(SoapMarshalling soapMarshalling,
                                SoapMessageFactory soapMessageFactory) {
        this.soapMarshalling = soapMarshalling;
        this.soapMessageFactory = soapMessageFactory;
        final ListMultimap<String, String> headers = ArrayListMultimap.create();
        headers.put(HttpHeaders.CONTENT_TYPE, SoapConstants.MEDIA_TYPE_SOAP);
        mockCommunicationContext = new CommunicationContext(
                new HttpApplicationInfo(headers, "mockTransactionId", null),
                new TransportInfo(
                        "mock.scheme",
                        "localhost",
                        123,
                        "remotehost",
                        456,
                        Collections.emptyList()
                ),
                new CommunicationLogContext("txetnoCgoLnoitacinummoC")
        );
    }

    public static void setHandlerRegistry(Map<String, HttpHandler> handlerRegistry) {
        TransportBindingFactoryMock.handlerRegistry = handlerRegistry;
    }

    @Override
    public TransportBinding createTransportBinding(String endpointUri, @Nullable CommunicationLogContext communicationLogContext) {
        if (handlerRegistry == null) {
            handlerRegistry = new HashMap<>();
        }
        Optional<HttpHandler> httpHandler = Optional.ofNullable(handlerRegistry.get(endpointUri));

        return new TransportBinding() {
            @Override
            public void close() {
            }

            @Override
            public void onNotification(SoapMessage notification) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try {
                    soapMarshalling.marshal(notification.getEnvelopeWithMappedHeaders(), bos);
                    if (httpHandler.isPresent()) {
                        httpHandler.get().handle(new ByteArrayInputStream(bos.toByteArray()),
                                new ByteArrayOutputStream(), mockCommunicationContext);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public SoapMessage onRequestResponse(SoapMessage request) throws MarshallingException, TransportException {
                ByteArrayOutputStream bosRequest = new ByteArrayOutputStream();

                try {
                    soapMarshalling.marshal(request.getEnvelopeWithMappedHeaders(), bosRequest);
                } catch (JAXBException e) {
                    throw new MarshallingException(e);
                }

                ByteArrayOutputStream bosResponse = new ByteArrayOutputStream();
                HttpHandler theHttpHandler = httpHandler.orElseThrow(() -> new TransportException("HTTP handler not set"));
                try {
                    theHttpHandler.handle(new ByteArrayInputStream(bosRequest.toByteArray()), bosResponse, mockCommunicationContext);
                } catch (HttpException e) {
                    throw new TransportException(e);
                }

                try {
                    Envelope env = soapMarshalling.unmarshal(new ByteArrayInputStream(bosResponse.toByteArray()));
                    return soapMessageFactory.createSoapMessage(env);
                } catch (JAXBException e) {
                    throw new MarshallingException(e);
                }
            }
        };
    }

    @Override
    public TransportBinding createHttpBinding(String endpointUri, @Nullable CommunicationLogContext communicationLogContext)
            throws UnsupportedOperationException {
        return createTransportBinding(endpointUri, communicationLogContext);
    }
}
