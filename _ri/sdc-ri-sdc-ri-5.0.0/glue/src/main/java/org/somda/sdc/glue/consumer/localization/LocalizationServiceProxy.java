package org.somda.sdc.glue.consumer.localization;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.message.AbstractGetResponse;
import org.somda.sdc.biceps.model.message.GetLocalizedText;
import org.somda.sdc.biceps.model.message.GetLocalizedTextResponse;
import org.somda.sdc.biceps.model.message.GetSupportedLanguages;
import org.somda.sdc.biceps.model.message.GetSupportedLanguagesResponse;
import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.glue.common.LocalizationServiceFilterUtil;
import org.somda.sdc.glue.consumer.helper.HostingServiceLogger;
import org.somda.sdc.glue.consumer.sco.InvocationException;
import org.somda.sdc.glue.guice.Consumer;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controller class that is responsible for invoking localization requests.
 */
public class LocalizationServiceProxy implements LocalizationServiceAccess {
    private static final Logger LOG = LogManager.getLogger(LocalizationServiceProxy.class);
    private final HostedServiceProxy hostedServiceProxy;
    private final ExecutorWrapperService<ListeningExecutorService> executorService;
    private final SoapUtil soapUtil;
    private final Logger instanceLogger;
    /**
     * Representation of Map<Version, Table<Row, Column, Value>>,
     * where row = ref, column = lang, value = LocalizedText
     */
    private final Map<BigInteger, Table<String, String, LocalizedText>> localizationCache = new HashMap<>();
    private final Set<BigInteger> fullyCachedVersions = new HashSet<>();
    private final Map<BigInteger, Set<String>> cachedVersionsToLanguageMap = new HashMap<>();

    @AssistedInject
    LocalizationServiceProxy(@Assisted HostingServiceProxy hostingServiceProxy,
                             @Assisted("localizationServiceProxy") @Nullable HostedServiceProxy hostedServiceProxy,
                             @Consumer ExecutorWrapperService<ListeningExecutorService> executorService,
                             SoapUtil soapUtil,
                             @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = HostingServiceLogger.getLogger(LOG, hostingServiceProxy, frameworkIdentifier);
        this.hostedServiceProxy = hostedServiceProxy;
        this.executorService = executorService;
        this.soapUtil = soapUtil;
    }

    @Override
    public ListenableFuture<GetLocalizedTextResponse> getLocalizedText(GetLocalizedText getLocalizedText) {
        return executorService.get().submit(() -> {
            instanceLogger.debug("Invoke GetLocalizedText with payload: {}",
                    getLocalizedText.toString());

            var texts = getLocalizedTextFromCache(getLocalizedText);
            if (texts != null && !texts.isEmpty()) {
                GetLocalizedTextResponse response = new GetLocalizedTextResponse();
                response.setText(texts);
                return response;
            }

            // no texts were found, try to fetch texts directly from Localization service via SOAP
            var request = soapUtil.createMessage(
                    ActionConstants.ACTION_GET_LOCALIZED_TEXT, getLocalizedText);
            return sendMessage(request, GetLocalizedTextResponse.class);
        });
    }

    @Override
    public ListenableFuture<GetSupportedLanguagesResponse> getSupportedLanguages(
            GetSupportedLanguages getSupportedLanguages) {
        return executorService.get().submit(() -> {
            instanceLogger.debug("Invoke GetSupportedLanguages");
            var request = soapUtil.createMessage(
                    ActionConstants.ACTION_GET_SUPPORTED_LANGUAGES, getSupportedLanguages);
            return sendMessage(request, GetSupportedLanguagesResponse.class);
        });
    }

    @Override
    public void cachePrefetch(BigInteger version) throws InvocationException {
        fullyCachedVersions.add(version);
        cachePrefetch(version, Collections.emptyList());
    }

    @Override
    public void cachePrefetch(BigInteger version, List<String> lang) throws InvocationException {
        var localizedTextTable = fetchLocalizedTextCache(version, lang);
        updateCache(version, localizedTextTable);
    }

    private void updateCache(BigInteger version, Table<String, String, LocalizedText> localizedTextTable) {
        if (!localizationCache.containsKey(version)) {
            localizationCache.put(version, localizedTextTable);
        } else {
            // in case this version was already cached for some languages, but we want to cache additional ones
            localizationCache.get(version).putAll(localizedTextTable);
        }
        updateCachedVersionMap(version, localizedTextTable);
    }

    private void updateCachedVersionMap(BigInteger version, Table<String, String, LocalizedText> localizedTextTable) {
        if (!cachedVersionsToLanguageMap.containsKey(version)) {
            cachedVersionsToLanguageMap.put(version, new HashSet<>());
        }

        cachedVersionsToLanguageMap.get(version).addAll(localizedTextTable.columnKeySet());
    }

    private Table<String, String, LocalizedText> fetchLocalizedTextCache(BigInteger version,
                                                                         List<String> lang) throws InvocationException {
        var request = new GetLocalizedText();
        request.setVersion(version);
        request.setLang(lang);
        var requestMsg = soapUtil.createMessage(ActionConstants.ACTION_GET_LOCALIZED_TEXT, request);
        var response = sendMessage(requestMsg, GetLocalizedTextResponse.class);
        Table<String, String, LocalizedText> localizedTextTable = HashBasedTable.create();
        if (response != null && response.getText() != null && !response.getText().isEmpty()) {
            response.getText().forEach(localizedText -> {
                localizedTextTable.put(localizedText.getRef(), localizedText.getLang(), localizedText);
            });
        }

        return localizedTextTable;
    }

    /**
     * Gets localized text from cache if it exists.
     * <p>
     * First tries to fetch localized text from cache. In case it doesn't exist and version with only one language
     * provided in the request then method pre-fetch cache based on version and language and tries to find localized
     * texts again. In case multiple languages provided in the request, cache prefetch is not executed.
     *
     * @param getLocalizedText request with localized text filter parameters
     * @return a list of localized text which matches filter criteria
     * @throws InvocationException if localization service is not available or something goes wrong during data fetch.
     */
    private List<LocalizedText> getLocalizedTextFromCache(GetLocalizedText getLocalizedText) throws InvocationException {
        List<LocalizedText> texts = queryLocalizedTextFromCache(getLocalizedText);
        // text was not found in cache
        if (texts == null || texts.isEmpty()) {
            // try to prefetch cache in case only one language provided
            List<String> requestedLanguages = getLocalizedText.getLang() != null ?
                    getLocalizedText.getLang() : Collections.emptyList();

            if (requestedLanguages.size() == 1) {
                cachePrefetch(getLocalizedText.getVersion(), requestedLanguages);
                return queryLocalizedTextFromCache(getLocalizedText);
            }
        }

        return texts;
    }

    private List<LocalizedText> queryLocalizedTextFromCache(GetLocalizedText getLocalizedText) {
        if (!cacheExist(getLocalizedText.getVersion(), getLocalizedText.getLang())) {
            return Collections.emptyList();
        }

        Multimap<String, LocalizedText> refToValueMap = LocalizationServiceFilterUtil.filterByLanguage(
                localizationCache.get(getLocalizedText.getVersion()), getLocalizedText.getLang());

        // if references not provided, return all records, otherwise filter by reference
        var references = getLocalizedText.getRef();
        return references.isEmpty() ? new ArrayList<>(refToValueMap.values()) :
                LocalizationServiceFilterUtil.filterByReferences(refToValueMap, references);
    }

    private boolean cacheExist(@Nullable BigInteger version, List<String> languages) {
        // version is mandatory for cached records
        if (version == null) {
            return false;
        }

        // we cannot trust cache records if languages not provided and version is not fully cached
        if (CollectionUtils.isEmpty(languages) && !fullyCachedVersions.contains(version)) {
            return false;
        }
        // finally, check version and languages was already cached.
        return localizationCache.containsKey(version) &&
                cachedVersionsToLanguageMap.containsKey(version) &&
                cachedVersionsToLanguageMap.get(version).containsAll(languages);
    }

    private <T extends AbstractGetResponse> T sendMessage(SoapMessage request,
                                                          Class<T> expectedResponseClass) throws InvocationException {
        try {
            if (hostedServiceProxy == null) {
                throw new InvocationException("Request could not be sent: no localization service available");
            }

            var response = hostedServiceProxy.getRequestResponseClient().sendRequestResponse(request);
            return soapUtil.getBody(response, expectedResponseClass).orElseThrow(() ->
                    new InvocationException("Received unexpected response"));
        } catch (InterceptorException | SoapFaultException | MarshallingException | TransportException e) {
            throw new InvocationException(String.format("Request to %s failed: %s",
                    hostedServiceProxy.getActiveEprAddress(), e.getMessage()), e);
        }
    }
}
