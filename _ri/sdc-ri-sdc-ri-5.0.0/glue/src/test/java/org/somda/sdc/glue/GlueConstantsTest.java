package org.somda.sdc.glue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import test.org.somda.common.LoggingTestWatcher;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(LoggingTestWatcher.class)
class GlueConstantsTest {
    @Test
    void staticInitialization() {
        assertNotNull(GlueConstants.SCOPE_SDC_PROVIDER);
    }
}