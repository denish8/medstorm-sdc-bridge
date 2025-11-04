package org.somda.sdc.glue.consumer;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.dpws.client.DiscoveryFilter;
import test.org.somda.common.LoggingTestWatcher;

import javax.annotation.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(LoggingTestWatcher.class)
class SdcDiscoveryFilterBuilderTest {
    private static final String EXPECTED_PKP_SCOPE = "sdc.mds.pkp:1.2.840.10004.20701.1.1";

    @Test
    void addContext() {
        final InstanceIdentifier identification = new InstanceIdentifier();
        identification.setExtensionName("an-extension");
        identification.setRootName("http://a-root");

        {
            final LocationContextState locationContextState = new LocationContextState();
            locationContextState.getIdentification().add(identification);
            locationContextState.setContextAssociation(ContextAssociation.ASSOC);
            final DiscoveryFilter discoveryFilter = SdcDiscoveryFilterBuilder.create().addContext(locationContextState).get();
            assertEquals(2, discoveryFilter.getScopes().size());
            assertEquals(EXPECTED_PKP_SCOPE, Iterables.get(discoveryFilter.getScopes(), 0));
            assertEquals("sdc.ctxt.loc:/http:%2F%2Fa-root/an-extension", Iterables.get(discoveryFilter.getScopes(), 1));
        }

        {
            final LocationContextState locationContextState = new LocationContextState();
            locationContextState.getIdentification().add(identification);
            locationContextState.setContextAssociation(ContextAssociation.NO);
            final DiscoveryFilter discoveryFilter = SdcDiscoveryFilterBuilder.create().addContext(locationContextState).get();
            assertEquals(1, discoveryFilter.getScopes().size());
            assertEquals(EXPECTED_PKP_SCOPE, Iterables.get(discoveryFilter.getScopes(), 0));
        }
    }

    @Test
    void addDeviceComponent() {
        {
            final DiscoveryFilter discoveryFilter = SdcDiscoveryFilterBuilder.create()
                    .addDeviceComponent(createVmd("http://a-codingsystem", "a-version", "a-code"))
                    .addDeviceComponent(createVmd(null, "", "a-code"))
                    .get();

            assertEquals(3, discoveryFilter.getScopes().size()); // +1 bc of SDC Provider PKP
            assertTrue(discoveryFilter.getScopes().contains(EXPECTED_PKP_SCOPE));
            assertTrue(discoveryFilter.getScopes().contains("sdc.cdc.type:/http:%2F%2Fa-codingsystem/a-version/a-code"));
            assertTrue(discoveryFilter.getScopes().contains("sdc.cdc.type:///a-code"));
        }
        {
            final DiscoveryFilter discoveryFilter = SdcDiscoveryFilterBuilder.create()
                    .addDeviceComponent(createVmd(null, "", "a-code"))
                    .addDeviceComponent(createVmd(null, "", "a-code"))
                    .addDeviceComponent(createVmd("urn:oid:1.2.840.10004.1.1.1.0.0.1", null, "a-code"))
                    .get();
            assertEquals(2, discoveryFilter.getScopes().size()); // discovery filter builder removes duplicates
            assertTrue(discoveryFilter.getScopes().contains(EXPECTED_PKP_SCOPE));
            assertEquals("sdc.cdc.type:///a-code", Iterables.get(discoveryFilter.getScopes(), 0));
        }
    }

    private VmdDescriptor createVmd(@Nullable String codingSystem,
                                    @Nullable String codingSystemVersion,
                                    @Nullable String code) {
        final VmdDescriptor vmdDescriptor = new VmdDescriptor();
        vmdDescriptor.setType(createCodedValue(codingSystem, codingSystemVersion, code));
        return vmdDescriptor;
    }

    private CodedValue createCodedValue(@Nullable String codingSystem,
                                        @Nullable String codingSystemVersion,
                                        @Nullable String code) {
        final CodedValue codedValue = new CodedValue();
        codedValue.setCodingSystem(codingSystem);
        codedValue.setCodingSystemVersion(codingSystemVersion);
        codedValue.setCode(code);
        return codedValue;
    }
}