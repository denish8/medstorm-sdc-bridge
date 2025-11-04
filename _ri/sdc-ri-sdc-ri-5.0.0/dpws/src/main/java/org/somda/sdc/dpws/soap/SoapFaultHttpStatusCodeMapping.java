package org.somda.sdc.dpws.soap;

import org.eclipse.jetty.http.HttpStatus;
import org.somda.sdc.dpws.soap.model.Fault;

/**
 * Mapping class to convert from SOAP faults to HTTP status codes.
 * <p>
 * The conversion is based on the mapping table SOAP Version 1.2 Part 2: Adjuncts specification.
 *
 * @see <a href="https://www.w3.org/TR/soap12-part2/#tabresstatereccodes"
 * >SOAP Version 1.2 Part 2: Adjuncts specification, Table 20: SOAP Fault to HTTP Status Mapping</a>
 */
public class SoapFaultHttpStatusCodeMapping {

    /**
     * Converts {@linkplain Fault} to an HTTP status code.
     * <p>
     * Sender fault causes the mapper to return 400 (bad request), all other fault QNames return 500
     * (internal server error).
     *
     * @param fault the fault to convert.
     * @return an HTTP status code number.
     */
    public static int get(Fault fault) {
        if (fault.getCode() == null || fault.getCode().getValue() == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR_500; // Missing code/value, convert to internal server error
        }

        if (fault.getCode().getValue().equals(SoapConstants.SENDER)) {
            return HttpStatus.BAD_REQUEST_400;
        }
        // Return 500 by default (Receiver, MustUnderstand, VersionMismatch, DataEncodingUnknown and others)
        return HttpStatus.INTERNAL_SERVER_ERROR_500;
    }
}
