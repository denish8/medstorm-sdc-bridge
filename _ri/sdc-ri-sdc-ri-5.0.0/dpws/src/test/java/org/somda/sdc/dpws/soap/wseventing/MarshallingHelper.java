package org.somda.sdc.dpws.soap.wseventing;

import com.google.inject.Injector;
import org.eclipse.jetty.http.HttpStatus;
import org.somda.sdc.dpws.http.HttpException;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.MarshallingService;
import org.somda.sdc.dpws.soap.RequestResponseServer;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;

import java.io.InputStream;
import java.io.OutputStream;

class MarshallingHelper {
    static void handleRequestResponse(Injector injector,
                                      RequestResponseServer srv,
                                      InputStream is,
                                      OutputStream os,
                                      CommunicationContext communicationContext) throws HttpException {
        try {
            var responseMessage = injector.getInstance(SoapUtil.class).createMessage();
            var marshallingService = injector.getInstance(MarshallingService.class);
            try {
                srv.receiveRequestResponse(marshallingService.unmarshal(is), responseMessage, communicationContext);
            } catch (SoapFaultException e) {
                marshallingService.marshal(e.getFaultMessage(), os);
                return;
            }
            marshallingService.marshal(responseMessage, os);
        } catch (MarshallingException e) {
            throw new HttpException(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage());
        }
    }
}
