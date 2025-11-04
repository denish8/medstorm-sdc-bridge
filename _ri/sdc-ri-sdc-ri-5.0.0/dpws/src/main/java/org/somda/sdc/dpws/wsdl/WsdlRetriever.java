package org.somda.sdc.dpws.wsdl;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.http.HttpResponse;
import org.somda.sdc.dpws.http.ContentType;
import org.somda.sdc.dpws.http.factory.HttpClientFactory;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.factory.RequestResponseClientFactory;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wsmetadataexchange.GetMetadataClient;
import org.somda.sdc.dpws.soap.wsmetadataexchange.WsMetadataExchangeConstants;
import org.somda.sdc.dpws.soap.wsmetadataexchange.model.Metadata;
import org.somda.sdc.dpws.soap.wstransfer.TransferGetClient;
import org.somda.sdc.dpws.wsdl.model.TDefinitions;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Utility to retrieve and handle WSDLs.
 * <p>
 * Three methods of retrieval are supported:
 * <ol>
 * <li> wsx:Location entries can retrieved via {@linkplain org.somda.sdc.dpws.http.HttpClient}
 * <li> WSDLs embedded in wsx:MetadataSection elements can be extracted
 * <li> WSDLs to be requested via wsx:MetadataReference can be requested via transfer get
 * </ol>
 * <em>WSDLs embedded in wsx:MetadataReference entries are currently unsupported.</em>
 */
public class WsdlRetriever {
    private static final Logger LOG = LogManager.getLogger();

    private final Logger instanceLogger;
    private final TransportBindingFactory transportBindingFactory;
    private final RequestResponseClientFactory requestResponseClientFactory;
    private final GetMetadataClient getMetadataClient;
    private final TransferGetClient transferGetClient;
    private final HttpClientFactory httpClientFactory;
    private final Duration maxWait;
    private final WsdlMarshalling wsdlMarshalling;

    @Inject
    WsdlRetriever(TransportBindingFactory transportBindingFactory,
                  RequestResponseClientFactory requestResponseClientFactory,
                  GetMetadataClient getMetadataClient,
                  TransferGetClient transferGetClient,
                  HttpClientFactory httpClientFactory,
                  WsdlMarshalling wsdlMarshalling,
                  @Named(DpwsConfig.MAX_WAIT_FOR_FUTURES) Duration maxWait,
                  @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.transportBindingFactory = transportBindingFactory;
        this.requestResponseClientFactory = requestResponseClientFactory;
        this.getMetadataClient = getMetadataClient;
        this.transferGetClient = transferGetClient;
        this.httpClientFactory = httpClientFactory;
        this.wsdlMarshalling = wsdlMarshalling;
        this.maxWait = maxWait;
    }

    /**
     * Retrieves all WSDLs of the Provider the client is connected to.
     *
     * @param hostingServiceProxy to retrieve WSDLs from
     * @return map with hosted service name as key and all associated WSDLs as values
     * @throws IOException        in case WSDLs could not be retrieved
     * @throws TransportException in case a transport related error occurs (connection refused, ...)
     */
    public Map<String, List<String>> retrieveWsdls(HostingServiceProxy hostingServiceProxy)
            throws IOException, TransportException {
        instanceLogger.debug("Retrieving WSDLs for {}", hostingServiceProxy.getActiveXAddr());
        var wsdlMap = new HashMap<String, List<String>>();

        var hostedServices = hostingServiceProxy.getHostedServices();
        for (Map.Entry<String, HostedServiceProxy> entry : hostedServices.entrySet()) {

            String serviceName = entry.getKey();
            HostedServiceProxy hostedServiceProxy = entry.getValue();
            SoapMessage metadataResponse;
            ListenableFuture<SoapMessage> metadataResponseFuture = null;
            LOG.debug("Retrieving WSDL for service {}", serviceName);
            try {
                metadataResponseFuture = getMetadataClient.sendGetMetadata(
                    hostedServiceProxy.getRequestResponseClient()
                );
                metadataResponse = metadataResponseFuture.get(maxWait.toSeconds(), TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException e) {
                throw new IOException("Could not retrieve Metadata from device for service " + serviceName);
            } catch (TimeoutException e) {
                metadataResponseFuture.cancel(true);
                throw new IOException(String.format(
                        "Could not retrieve Metadata from device for service %s after %ss",
                        serviceName, maxWait.toSeconds()
                    ));
            }

            var bodyElements = metadataResponse.getOriginalEnvelope().getBody().getAny();
            var getMetadataResponse = (Metadata) bodyElements.get(0);

            try {
                var wsdl = retrieveWsdlFromMetadata(getMetadataResponse);
                wsdlMap.put(serviceName, wsdl);
            } catch (IOException | TransportException e) {
                LOG.error("Could not retrieve WSDL for service {}", serviceName);
                LOG.trace("Could not retrieve WSDL for service {}", serviceName, e);
                throw e;
            }
        }

        return wsdlMap;
    }

    /**
     * Retrieves the WSDLs referenced or embedded in the provided metadata.
     *
     * @param metadata to extract WSDLs from
     * @return all WSDLs found within the given metadata
     * @throws IOException        in case WSDLs could not be retrieved
     * @throws TransportException in case a transport related error occurs (connection refused, ...)
     */
    public List<String> retrieveWsdlFromMetadata(Metadata metadata) throws TransportException, IOException {
        // find metadata section with wsdl
        var wsdlSections = metadata.getMetadataSection()
                .stream()
                .filter(section -> WsMetadataExchangeConstants.DIALECT_WSDL.equals(section.getDialect()))
                .collect(Collectors.toList());
        if (wsdlSections.isEmpty()) {
            throw new IOException(
                    "Metadata for service did not contain any wsdl references"
            );
        }

        var collectedWsdls = new ArrayList<String>();

        for (var wsdlSection : wsdlSections) {
            // only one of these is allowed to exist, so verify that is true
            var location = wsdlSection.getLocation();
            var reference = wsdlSection.getMetadataReference();
            var any = wsdlSection.getAny();
            boolean hasLocation = location != null;
            boolean hasReference = reference != null;
            boolean hasAny = any != null;

            if (!BooleanUtils.xor(new boolean[]{hasLocation, hasReference, hasAny})) {
                throw new IOException(
                        "Metadata for service has too many possible wsdl locations. It had"
                                + " DialectSpecificElement: " + hasAny
                                + " MetadataReference: " + hasReference
                                + " Location: " + hasLocation
                                + ". Only one is allowed."
                );
            }

            if (hasLocation) {
                LOG.debug("WSDL is stored at location {}", location);
                collectedWsdls.add(retrieveWsdlFromLocation(location));
            } else if (hasReference) {
                LOG.debug("WSDL stored as MetadataReference {}", reference);
                collectedWsdls.addAll(retrieveWsdlFromMetadataReference(reference));
            } else if (hasAny) {
                LOG.debug("WSDL is stored in metadata response");
                collectedWsdls.add(retrieveWsdlFromAny(any));
            } else {
                throw new IOException("Unexpected error while trying to retrieve WSDL: No match for any method.");
            }
        }

        return collectedWsdls;
    }

    @SuppressWarnings("unchecked")
    private String retrieveWsdlFromAny(Object any) throws IOException {
        try {
            var definition = (JAXBElement<TDefinitions>) any;
            return retrieveEmbeddedWsdlFromMetadata(definition.getValue());
        } catch (ClassCastException e) {
            LOG.error(
                    "Any node in metadata was not of instance JAXBElement<TDefinitions>,"
                            + " was {} instead, cannot handle it",
                    any.getClass().getSimpleName()
            );
            throw new IOException("Any node in metadata was not of instance JAXBElement<TDefinitions>, " +
                    "cannot handle it");
        }
    }

    private String retrieveWsdlFromLocation(String location) throws TransportException {
        var client = httpClientFactory.createHttpClient();
        HttpResponse response = client.sendGet(location);
        if (response.getStatusCode() >= 300) {
            throw new TransportException("Unexpected HTTP status code. Was " + response.getStatusCode());
        }

        return convertResponseToString(response);
    }

    String convertResponseToString(HttpResponse response) {
        Charset charset = null;
        var contentTypeOpt = ContentType.fromListMultimap(response.getHeader());
        if (contentTypeOpt.isPresent() && contentTypeOpt.get().getCharset() != null) {
            charset = contentTypeOpt.get().getCharset();
        }
        // find xml declaration if there is one and content-type didn't specify the encoding
        if (charset == null) {
            try {
                var factory = XMLInputFactory.newInstance();
                // #218 prevent XXE attacks
                factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
                XMLStreamReader xmlStreamReader = factory.createXMLStreamReader(
                        new ByteArrayInputStream(response.getBody())
                );
                var encoding = xmlStreamReader.getCharacterEncodingScheme();
                if (encoding != null) {
                    charset = Charset.forName(encoding);
                }
            } catch (XMLStreamException | UnsupportedCharsetException e) {
                LOG.warn("Could not determine XML encoding scheme");
                LOG.trace("Could not determine XML encoding scheme", e);
            }
        }
        if (charset == null) {
            // this seems like a sensible default in a world which apparently does not make any sense
            charset = StandardCharsets.UTF_8;
        }

        return new String(response.getBody(), charset);
    }

    private List<String> retrieveWsdlFromMetadataReference(EndpointReferenceType reference)
            throws TransportException, IOException {
        var address = reference.getAddress().getValue();
        var referenceParameters = reference.getReferenceParameters();

        var transportBinding = transportBindingFactory.createTransportBinding(address, null);
        var rrClient = requestResponseClientFactory.createRequestResponseClient(transportBinding);
        ListenableFuture<SoapMessage> getResponseFuture = null;
        SoapMessage getResponse;
        try {
            if (referenceParameters != null) {
                getResponseFuture = transferGetClient.sendTransferGet(rrClient, address, referenceParameters);
            } else {
                getResponseFuture = transferGetClient.sendTransferGet(rrClient, address);
            }
            getResponse = getResponseFuture.get(maxWait.toSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException e) {
            instanceLogger.error("Could not retrieve Metadata from device. Message: {}", e.getMessage());
            instanceLogger.trace("Could not retrieve Metadata from device", e);
            throw new TransportException("Could not retrieve Metadata from device", e);
        } catch (TimeoutException e) {
            getResponseFuture.cancel(true);
            instanceLogger.error(
                "Could not retrieve Metadata from device after {}s. Message: {}",
                maxWait.toSeconds(), e.getMessage()
            );
            instanceLogger.trace("Could not retrieve Metadata from device after {}s", maxWait.toSeconds(), e);
            throw new TransportException(String.format(
                "Could not retrieve Metadata from device after %ss", maxWait.toSeconds()
            ), e);
        }

        var bodyElements = getResponse.getOriginalEnvelope().getBody().getAny();
        var getMetadataResponse = (Metadata) bodyElements.get(0);

        return retrieveWsdlFromMetadata(getMetadataResponse);
    }

    private String retrieveEmbeddedWsdlFromMetadata(TDefinitions wsdlNode) throws IOException {
        var output = new ByteArrayOutputStream();
        try {
            wsdlMarshalling.marshal(wsdlNode, output);
        } catch (JAXBException e) {
            instanceLogger.error("Error while marshalling embedded wsdl. Message: {}", e.getMessage());
            instanceLogger.error("Error while marshalling embedded wsdl", e);
            throw new IOException("Error while marshalling embedded wsdl", e);
        }

        return output.toString(StandardCharsets.UTF_8);
    }
}
