package org.somda.sdc.glue.provider.services;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.biceps.common.access.ReadTransaction;
import org.somda.sdc.biceps.model.message.GetLocalizedText;
import org.somda.sdc.biceps.model.message.GetSupportedLanguages;
import org.somda.sdc.biceps.model.message.ObjectFactory;
import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.dpws.device.WebService;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.factory.SoapFaultFactory;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.glue.common.MdibVersionUtil;
import org.somda.sdc.glue.provider.localization.LocalizationService;
import org.somda.sdc.glue.provider.localization.LocalizationStorage;
import org.somda.sdc.glue.provider.localization.factory.LocalizationServiceFactory;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Implementation of the low-priority services.
 * <p>
 * Low-priority services are all services that are effective without time-constraints:
 * <ul>
 * <li>Archive service
 * <li>Localization service
 * </ul>
 */
public class LowPriorityServices extends WebService {
    private final LocalMdibAccess mdibAccess;
    private final SoapUtil soapUtil;
    private final SoapFaultFactory faultFactory;
    private final ObjectFactory messageModelFactory;
    private final MdibVersionUtil mdibVersionUtil;
    private final WsAddressingUtil wsaUtil;
    private LocalizationService localizationService;

    @AssistedInject
    LowPriorityServices(@Assisted LocalMdibAccess mdibAccess,
                        @Assisted @Nullable LocalizationStorage localizationStorage,
                        SoapUtil soapUtil,
                        SoapFaultFactory faultFactory,
                        ObjectFactory messageModelFactory,
                        MdibVersionUtil mdibVersionUtil,
                        WsAddressingUtil wsaUtil,
                        LocalizationServiceFactory localizationServiceFactory) {
        this.mdibAccess = mdibAccess;
        this.soapUtil = soapUtil;
        this.faultFactory = faultFactory;
        this.messageModelFactory = messageModelFactory;
        this.mdibVersionUtil = mdibVersionUtil;
        this.wsaUtil = wsaUtil;
        if (localizationStorage != null) {
            localizationService = localizationServiceFactory.createLocalizationService(localizationStorage);
            localizationService.startAsync().awaitRunning();
        }
    }

    @MessageInterceptor(ActionConstants.ACTION_GET_LOCALIZED_TEXT)
    void getLocalizedText(RequestResponseObject requestResponseObject) throws SoapFaultException {
        var getLocalizedText = getRequest(requestResponseObject, GetLocalizedText.class);
        var getLocalizedTextResponse = messageModelFactory.createGetLocalizedTextResponse();
        getLocalizedTextResponse.setText(fetchTexts(getLocalizedText));

        setResponse(requestResponseObject, getLocalizedTextResponse, mdibAccess.getMdibVersion(),
                ActionConstants.getResponseAction(ActionConstants.ACTION_GET_LOCALIZED_TEXT));

    }

    @MessageInterceptor(ActionConstants.ACTION_GET_SUPPORTED_LANGUAGES)
    void getSupportedLanguages(RequestResponseObject requestResponseObject) throws SoapFaultException {
        var getSupportedLanguagesResponse =
                messageModelFactory.createGetSupportedLanguagesResponse();
        getSupportedLanguagesResponse.setLang(localizationService.getSupportedLanguages());

        setResponse(requestResponseObject, getSupportedLanguagesResponse, mdibAccess.getMdibVersion(),
                ActionConstants.getResponseAction(ActionConstants.ACTION_GET_SUPPORTED_LANGUAGES));

    }

    private <T> T getRequest(RequestResponseObject requestResponseObject, Class<T> bodyType) throws SoapFaultException {
        return soapUtil.getBody(requestResponseObject.getRequest(), bodyType).orElseThrow(() ->
                new SoapFaultException(faultFactory.createSenderFault(String.format("%s SOAP request body is malformed",
                        bodyType.getSimpleName())),
                        requestResponseObject.getRequest().getWsAddressingHeader().getMessageId().orElse(null)));
    }

    private <T> void setResponse(RequestResponseObject requestResponseObject,
                                 T response,
                                 MdibVersion mdibVersion,
                                 String responseAction) throws SoapFaultException {
        try {
            mdibVersionUtil.setMdibVersion(mdibVersion, response);
        } catch (Exception e) {
            throw new SoapFaultException(faultFactory.createReceiverFault("Could not create MDIB version."),
                    requestResponseObject.getRequest().getWsAddressingHeader().getMessageId().orElse(null));
        }
        requestResponseObject.getResponse().getWsAddressingHeader().setAction(wsaUtil.createAttributedURIType(
                responseAction));
        soapUtil.setBody(response, requestResponseObject.getResponse());
    }

    private List<LocalizedText> fetchTexts(GetLocalizedText getLocalizedText) {
        return localizationService.getLocalizedText(
                getLocalizedText.getRef(),
                getLocalizedText.getVersion(),
                getLocalizedText.getLang(),
                getLocalizedText.getTextWidth(),
                getLocalizedText.getNumberOfLines());
    }
}
