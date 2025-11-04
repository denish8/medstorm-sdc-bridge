package org.somda.sdc.glue.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.biceps.model.participant.LocationDetail;
import test.org.somda.common.LoggingTestWatcher;

import javax.annotation.Nullable;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(LoggingTestWatcher.class)
class FallbackInstanceIdentifierTest {
    private static final String EXPECTED_ROOT = "sdc.ctxt.loc.detail";

    @Test
    void fullyPopulatedLocationDetail() {
        Optional<InstanceIdentifier> ii = create("facility", "building", "floor", "poc", "room", "bed");
        final String expectedExtension = "facility/building/floor/poc/room/bed";
        assertTrue(ii.isPresent());
        assertEquals(EXPECTED_ROOT, ii.get().getRootName());
        assertEquals(expectedExtension, ii.get().getExtensionName());
    }

    @Test
    void emptySegmentsInLocationDetail() {
        {
            Optional<InstanceIdentifier> ii = create("", "building", "", "poc", "room", "");
            final String expectedExtension = "/building//poc/room/";
            assertTrue(ii.isPresent());
            assertEquals(EXPECTED_ROOT, ii.get().getRootName());
            assertEquals(expectedExtension, ii.get().getExtensionName());
        }
        {
            Optional<InstanceIdentifier> ii = create(null, "building", "", "poc", "room", null);
            final String expectedExtension = "/building//poc/room/";
            assertTrue(ii.isPresent());
            assertEquals(EXPECTED_ROOT, ii.get().getRootName());
            assertEquals(expectedExtension, ii.get().getExtensionName());
        }
    }

    @Test
    void emptyLocationDetail() {
        {
            Optional<InstanceIdentifier> ii = create("", "", "", "", "", "");
            assertTrue(ii.isEmpty());
        }
        {
            Optional<InstanceIdentifier> ii = create(null, null, null, null, null, null);
            assertTrue(ii.isEmpty());
        }
    }

    @Test
    void specialCharacters() {
        {
            Optional<InstanceIdentifier> ii = create("Gebäude", "?build ing", "Flo%r+", "p/o/c", "room", "Bed%");
            final String expectedExtension = "Geb%C3%A4ude/%3Fbuild%20ing/Flo%25r+/p%2Fo%2Fc/room/Bed%25";
            assertTrue(ii.isPresent());
            assertEquals(EXPECTED_ROOT, ii.get().getRootName());
            assertEquals(expectedExtension, ii.get().getExtensionName());
        }
    }

    private Optional<InstanceIdentifier> create(@Nullable String facility,
                                                @Nullable String building,
                                                @Nullable String floor,
                                                @Nullable String poc,
                                                @Nullable String room,
                                                @Nullable String bed) {
        LocationDetail locationDetail = new LocationDetail();
        locationDetail.setFacility(facility);
        locationDetail.setBuilding(building);
        locationDetail.setFloor(floor);
        locationDetail.setPoC(poc);
        locationDetail.setRoom(room);
        locationDetail.setBed(bed);
        return FallbackInstanceIdentifier.create(locationDetail);
    }
}