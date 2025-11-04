package org.somda.sdc.dpws.http.apache;

import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.CommunicationLogContext;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.TransportBinding;
import org.somda.sdc.dpws.TransportBindingException;
import org.somda.sdc.dpws.http.ContentType;
import org.somda.sdc.dpws.http.HttpException;
import org.somda.sdc.dpws.soap.SoapConstants;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.model.Envelope;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

/**
 * Binding providing request-response and notification capabilities using an {@linkplain HttpClient}.
 */
public class ClientTransportBinding implements TransportBinding {

    public static final String USER_AGENT_KEY = "X-User-Agent";
    public static final String USER_AGENT_VALUE = "SDCri";

    public static final String COMMUNICATION_LOG_CONTEXT_KEY = "SDCri_Communication_Log_Context";

    private static final Logger LOG = LogManager.getLogger(ClientTransportBinding.class);

    private final SoapMarshalling marshalling;
    private final SoapUtil soapUtil;
    private final Logger instanceLogger;
    private final HttpClient client;
    private final String clientUri;
    private final boolean chunkedTransfer;
    private final String contentTypeString;
    @Nullable
    private final CommunicationLogContext communicationLogContext;

    @Inject
    ClientTransportBinding(@Assisted HttpClient client,
                           @Assisted String clientUri,
                           @Assisted SoapMarshalling marshalling,
                           @Assisted SoapUtil soapUtil,
                           @Assisted @Nullable CommunicationLogContext communicationLogContext,
                           @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                           @Named(DpwsConfig.ENFORCE_HTTP_CHUNKED_TRANSFER) boolean chunkedTransfer,
                           @Named(DpwsConfig.HTTP_CONTENT_TYPE_CHARSET) String charset) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        instanceLogger.debug("Creating ClientTransportBinding for {}", clientUri);
        this.client = client;
        this.clientUri = clientUri;
        this.marshalling = marshalling;
        this.soapUtil = soapUtil;
        this.chunkedTransfer = chunkedTransfer;
        this.contentTypeString = org.apache.http.entity.ContentType.create(SoapConstants.MEDIA_TYPE_SOAP,
                charset).toString();
        this.communicationLogContext = communicationLogContext;
    }

    @Override
    public void onNotification(SoapMessage notification) throws TransportBindingException {
        // Ignore the result even if there is one
        try {
            onRequestResponse(notification);
        } catch (SoapFaultException e) {
            // Swallow exception, rationale:
            // we assume that notifications have no response and therefore no soap exception
            // that could be thrown
        }
    }

    @Override
    public SoapMessage onRequestResponse(SoapMessage request) throws TransportBindingException, SoapFaultException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // create post request and set content type to SOAP
        HttpPost post = new HttpPost(this.clientUri);
        post.setHeader(HttpHeaders.ACCEPT, SoapConstants.MEDIA_TYPE_SOAP);
        post.setHeader(HttpHeaders.CONTENT_TYPE, contentTypeString);
        post.setHeader(USER_AGENT_KEY, USER_AGENT_VALUE);

        try {
            marshalling.marshal(request.getEnvelopeWithMappedHeaders(), byteArrayOutputStream);
        } catch (JAXBException e) {
            instanceLogger.warn("Marshalling of a message failed: {}", e.getMessage());
            instanceLogger.trace("Marshalling of a message failed", e);
            throw new TransportBindingException(
                    String.format("Sending of a request failed due to marshalling problem: %s", e.getMessage()),
                    new MarshallingException(e));
        }

        // attach payload
        var requestEntity = new ByteArrayEntity(byteArrayOutputStream.toByteArray());

        if (this.chunkedTransfer) {
            requestEntity.setChunked(true);
        }

        post.setEntity(requestEntity);

        final var httpContext = new BasicHttpContext();
        httpContext.setAttribute(COMMUNICATION_LOG_CONTEXT_KEY, communicationLogContext);

        instanceLogger.debug("Sending POST request to {}", this.clientUri);
        HttpResponse response;

        try {
            // no retry handling is required as apache httpclient already does
            response = this.client.execute(post, httpContext);
        } catch (SocketException e) {
            instanceLogger.error("Unexpected SocketException on request to {}", this.clientUri, e);
            throw new TransportBindingException(e);
        } catch (IOException e) {
            instanceLogger.error("Unexpected IO exception on request to {}. {}", this.clientUri, e.getMessage());
            instanceLogger.trace("Unexpected IO exception on request to {}", this.clientUri, e);
            throw new TransportBindingException("No response received", e);
        }

        HttpEntity entity = response.getEntity();
        byte[] bytes;

        var contentTypeElements = entity.getContentType();
        var contentType = ContentType.fromApache(contentTypeElements).orElseThrow(() -> {
            instanceLogger.error("Could not parse content type from element {}", contentTypeElements);
            return new TransportBindingException("Could not parse content type from element " + contentTypeElements);
        });

        try (InputStream contentStream = entity.getContent()) {
            bytes = ByteStreams.toByteArray(contentStream);
        } catch (IOException e) {
            instanceLogger.error("Couldn't read response", e);
            bytes = new byte[0];
        }

        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            if (inputStream.available() > 0) {
                Envelope envelope;
                // wrap up content with a known or forced charset into a reader
                if (contentType.getCharset() != null) {
                    Reader reader = new InputStreamReader(inputStream, contentType.getCharset());
                    envelope = marshalling.unmarshal(reader);
                } else {
                    // let jaxb figure it out otherwise
                    envelope = marshalling.unmarshal(inputStream);
                }

                SoapMessage msg = soapUtil.createMessage(envelope);
                if (msg.isFault()) {
                    throw new SoapFaultException(msg, new HttpException(response.getStatusLine().getStatusCode()),
                            request.getWsAddressingHeader().getMessageId().orElse(null));
                }

                return msg;
            } else {
                if (response.getStatusLine().getStatusCode() >= 300) {
                    throw new TransportBindingException(String.format(
                            "Endpoint was not able to process request. HTTP status code: %s", response.getStatusLine()),
                            new TransportException(new HttpException(response.getStatusLine().getStatusCode())));
                }
            }
        } catch (JAXBException e) {
            instanceLogger.debug("Unmarshalling of a message failed: {}. Response payload:\n{}", e.getMessage(),
                    new String(bytes, StandardCharsets.UTF_8));
            instanceLogger.trace("Unmarshalling of a message failed. ", e);
            throw new TransportBindingException(String
                    .format("Receiving of a response failed due to unmarshalling problem: %s", e.getCause()),
                    new MarshallingException(e));
        } catch (IOException e) {
            instanceLogger.debug("Error occurred while processing response: {}", e.getMessage());
            instanceLogger.trace("Error occurred while processing response", e);
        } finally {
            try {
                // ensure the entire response was consumed, just in case
                EntityUtils.consume(response.getEntity());
            } catch (IOException e) {
                // if this fails, we will either all die or it doesn't matter at all...
            }
        }

        return soapUtil.createMessage();
    }

    @Override
    public void close() {
        // no action on HTTP
    }
}
