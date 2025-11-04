package org.somda.sdc.glue.provider.plugin;

import org.somda.sdc.glue.provider.SdcDeviceContext;

import java.util.Set;

/**
 * Decorator interface to allow extending WS-Discovery Scopes during runtime.
 * <p>
 * The decorator does only allow changes to scopes based on context and description modifications.
 * This interface can be used to extend the required types and scopes implementation {@link SdcRequiredTypesAndScopes}
 * by means of the decorator pattern.
 */
public interface ScopesDecorator {
    /**
     * Call this function to initialize the scopes decorator.
     * <p>
     * <em>Not doing so can cause unknown side-effects depending on the decorated implementation.</em>
     *  @param context the context data possibly needed by the decorator to work.
     * @param scopes  initial set of scopes visible to the decorator.
     */
    void init(SdcDeviceContext context, Set<String> scopes);

    /**
     * Gives the order to append the given set of scopes and send a Hello if changes ensued.
     * <p>
     * Call this function after a context or description modification message has been processed.
     *
     * @param scopes the scopes to update.
     */
    void appendScopesAndSendHello(Set<String> scopes);
}
