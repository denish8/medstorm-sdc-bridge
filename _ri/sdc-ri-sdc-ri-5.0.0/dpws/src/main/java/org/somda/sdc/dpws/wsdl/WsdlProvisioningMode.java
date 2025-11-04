package org.somda.sdc.dpws.wsdl;

/**
 * Definition of the WSDL provisioning mode.
 * <p>
 * The WSDL provisioning mode defines how a device allows a client to request a hosted service's WSDL definition.
 *
 * todo DGr ReferenceParameter provisioning mode is missing.
 *
 * @see <a href="https://www.w3.org/Submission/2008/SUBM-WS-MetadataExchange-20080813/#web-services-metadata">
 *     4. Web Services Metadata</a>
 */
public enum WsdlProvisioningMode {
    /**
     * The WSDL definition is included in the GetMetadata response's MetadataSection as DialectSpecificElement.
     */
    INLINE,

    /**
     * The WSDL definition is included in the GetMetadata response's MetadataSection as URL.
     */
    RESOURCE
}