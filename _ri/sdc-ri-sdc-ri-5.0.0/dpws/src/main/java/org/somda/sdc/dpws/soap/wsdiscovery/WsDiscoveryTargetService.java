package org.somda.sdc.dpws.soap.wsdiscovery;

import com.google.common.primitives.UnsignedInteger;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * WS-Discovery Target Service interface.
 *
 * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231815"
 * >Conceptual Message Content</a>
 */
public interface WsDiscoveryTargetService extends Interceptor {
    EndpointReferenceType getEndpointReference();

    /**
     * Thread-safe function to set the list of QNames.
     *
     * @param qNames the list of QNames to set.
     */
    void setTypes(List<QName> qNames);

    /**
     * Thread-safe function to get the list of QNames.
     *
     * @return the list of QNames.
     */
    List<QName> getTypes();

    /**
     * Thread-safe function to set the list of scope URIs.
     *
     * @param uris the list of scope URIs to set.
     */
    void setScopes(List<String> uris);

    /**
     * Thread-safe function to get the list of scope URIs.
     *
     * @return the list of scope URIs.
     */
    List<String> getScopes();

    /**
     * Thread-safe function to set the list of XAddr URIs.
     *
     * @param xAddrs the list of XAddr URIs.
     */
    void setXAddrs(List<String> xAddrs);

    /**
     * Thread-safe function to get the list of XAddr URIs.
     *
     * @return the list of XAddr URIs.
     */
    List<String> getXAddrs();

    /**
     * Thread-safe function to explicitly set the target service metadata to be modified (thread-safe).
     * <p>
     * This is useful if the metadata change does not belong to, e.g., Types and Scopes.
     */
    void setMetadataModified();

    /**
     * Thread-safe function to get the metadata version.
     *
     * @return the metadata version.
     */
    UnsignedInteger getMetadataVersion();

    /**
     * Blocking function to send out a Hello message.
     * <p>
     * This is a shorthand function to {@link #sendHello(boolean)}.
     *
     * @return the metadata version that has been attached to the Hello message.
     * @throws MarshallingException if marshalling the Hello message fails.
     * @throws TransportException   if there is any problem on the transport layer.
     * @throws InterceptorException if one of the interceptors pops up with an error.
     */
    UnsignedInteger sendHello() throws MarshallingException, TransportException, InterceptorException;

    /**
     * Blocking function to send out a Hello message.
     *
     * @param forceNewMetadataVersion set to true to force incrementing the metadata version, or false to not do so.
     * @return the metadata version that has been attached to the Hello message.
     * @throws MarshallingException if marshalling the Hello message fails.
     * @throws TransportException   if there is any problem on the transport layer.
     * @throws InterceptorException if one of the interceptors pops up with an error.
     */
    UnsignedInteger sendHello(boolean forceNewMetadataVersion)
            throws MarshallingException, TransportException, InterceptorException;

    /**
     * Blocking function to send out a Bye message.
     *
     * @throws MarshallingException if marshalling the Bye message fails.
     * @throws TransportException   if there is any problem on the transport layer.
     * @throws InterceptorException if one of the interceptors pops up with an error.
     */
    void sendBye() throws MarshallingException, TransportException, InterceptorException;
}
