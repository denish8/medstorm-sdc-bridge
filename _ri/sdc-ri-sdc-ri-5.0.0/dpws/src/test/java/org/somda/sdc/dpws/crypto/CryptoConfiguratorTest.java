package org.somda.sdc.dpws.crypto;

import it.org.somda.sdc.dpws.soap.Ssl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.net.ssl.SSLContext;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CryptoConfiguratorTest {

    @Test
    @DisplayName("Verifies that in case a CachingCryptoSettings is provided, it is used to cache context instances.")
    void testCache() throws Exception {
        var configurator = new CryptoConfigurator();
        var mockSettings = mock(CachingCryptoSettings.class);
        var innerData = Ssl.setupServer();

        when(mockSettings.getKeyStoreStream()).thenReturn(innerData.getKeyStoreStream());
        when(mockSettings.getKeyStorePassword()).thenReturn(innerData.getKeyStorePassword());
        when(mockSettings.getTrustStoreStream()).thenReturn(innerData.getTrustStoreStream());
        when(mockSettings.getTrustStorePassword()).thenReturn(innerData.getTrustStorePassword());

        // when no context is in cache, it is created
        when(mockSettings.getSslContext()).thenReturn(Optional.empty());
        var context = configurator.createSslContextFromCryptoConfig(mockSettings);
        // context is identical in cache
        var contextCaptor = ArgumentCaptor.forClass(SSLContext.class);
        verify(mockSettings, times(1)).setSslContext(contextCaptor.capture());
        verify(mockSettings, atLeast(1)).getKeyStoreStream();
        verify(mockSettings, atLeast(1)).getKeyStorePassword();
        verify(mockSettings, atLeast(1)).getTrustStoreStream();
        verify(mockSettings, atLeast(1)).getTrustStorePassword();
        assertEquals(context, contextCaptor.getValue());

        // when context is in cache, it is retrieved from cache and not set or created again
        clearInvocations(mockSettings);
        when(mockSettings.getSslContext()).thenReturn(Optional.of(context));
        var context2 = configurator.createSslContextFromCryptoConfig(mockSettings);
        verify(mockSettings, never()).setSslContext(contextCaptor.capture());
        verify(mockSettings, never()).getKeyStoreStream();
        verify(mockSettings, never()).getKeyStorePassword();
        verify(mockSettings, never()).getTrustStoreStream();
        verify(mockSettings, never()).getTrustStorePassword();
        assertEquals(context, context2);
    }

}
