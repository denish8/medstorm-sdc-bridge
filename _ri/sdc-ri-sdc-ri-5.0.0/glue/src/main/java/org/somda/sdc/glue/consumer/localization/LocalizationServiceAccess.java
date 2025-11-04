package org.somda.sdc.glue.consumer.localization;

import com.google.common.util.concurrent.ListenableFuture;
import org.somda.sdc.biceps.model.message.GetLocalizedText;
import org.somda.sdc.biceps.model.message.GetLocalizedTextResponse;
import org.somda.sdc.biceps.model.message.GetSupportedLanguages;
import org.somda.sdc.biceps.model.message.GetSupportedLanguagesResponse;
import org.somda.sdc.glue.consumer.sco.InvocationException;

import java.math.BigInteger;
import java.util.List;

/**
 * API to invoke localization service operations.
 */
public interface LocalizationServiceAccess {
    /**
     * Invokes a localization service to get localized texts.
     * <p>
     * If there is no localization service available, this function returns with a cancelled future.
     *
     * @param getLocalizedTextRequest the request to send to the localization service.
     * @return a future object that contains a list of localized texts.
     */
    ListenableFuture<GetLocalizedTextResponse> getLocalizedText(GetLocalizedText getLocalizedTextRequest);

    /**
     * Invokes a localization service to get supported languages.
     * <p>
     * If there is no localization service available, this function returns with a cancelled future.
     *
     * @param getSupportedLanguagesRequest the request to get supported languages.
     * @return a future object that contains a list of supported languages.
     */
    ListenableFuture<GetSupportedLanguagesResponse> getSupportedLanguages(
            GetSupportedLanguages getSupportedLanguagesRequest);

    /**
     * Calls a synchronous cache prefetch of localized texts by provided version.
     * @param version - a version of localized texts to be included into cache.
     * @throws InvocationException if localization service is not available or something goes wrong during data fetch.
     */
    default void cachePrefetch(BigInteger version) throws InvocationException {}

    /**
     * Calls a synchronous cache prefetch of localized texts by provided version and languages.
     * @param version a version of localized texts to be included into cache.
     * @param lang optional list of languages to be included into cache.
     *             If languages list is not provided or is empty - all languages are fetched.
     * @throws InvocationException if localization service is not available or something goes wrong during data fetch.
     */
    default void cachePrefetch(BigInteger version, List<String> lang) throws InvocationException {}
}
