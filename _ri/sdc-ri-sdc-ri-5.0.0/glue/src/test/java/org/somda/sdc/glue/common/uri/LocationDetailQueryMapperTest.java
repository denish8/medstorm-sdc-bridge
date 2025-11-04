package org.somda.sdc.glue.common.uri;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.biceps.model.participant.LocationDetail;
import test.org.somda.common.LoggingTestWatcher;

import javax.annotation.Nullable;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(LoggingTestWatcher.class)
class LocationDetailQueryMapperTest {
    @Test
    void createWithLocationDetailQuery() throws UriMapperGenerationArgumentException {

        InstanceIdentifier instanceIdentifier = new InstanceIdentifier();
        instanceIdentifier.setRootName("http://root");

        {
            var locationDetail = createLocationDetail("facility1&", "building1", "floor1", "poc1", "room1", "bed1");
            final String actualUri = LocationDetailQueryMapper.createWithLocationDetailQuery(instanceIdentifier, locationDetail);

            var expectedUri = "sdc.ctxt.loc:/http:%2F%2Froot/?fac=facility1%26&bldng=building1&poc=poc1&flr=floor1&rm=room1&bed=bed1";
            assertEquals(expectedUri, actualUri);
        }
        {
            var locationDetail = createLocationDetail("facility1", null, "floor1", "poc1", "room1", null);
            final String actualUri = LocationDetailQueryMapper.createWithLocationDetailQuery(instanceIdentifier, locationDetail);

            var expectedUri = "sdc.ctxt.loc:/http:%2F%2Froot/?fac=facility1&poc=poc1&flr=floor1&rm=room1";
            assertEquals(expectedUri, actualUri);
        }
        {
            var locationDetail = createLocationDetail(null, null, null, null, null, null);
            final String actualUri = LocationDetailQueryMapper.createWithLocationDetailQuery(instanceIdentifier, locationDetail);

            var expectedUri = "sdc.ctxt.loc:/http:%2F%2Froot/";
            assertEquals(expectedUri, actualUri);
        }
        {
            instanceIdentifier.setRootName("sdc.ctxt.loc.detail");
            instanceIdentifier.setExtensionName("//lol");

            var locationDetail = createLocationDetail(null, null, null, null, null, null);
            final String actualUri = LocationDetailQueryMapper.createWithLocationDetailQuery(instanceIdentifier, locationDetail);

            var expectedUri = "sdc.ctxt.loc:/sdc.ctxt.loc.detail/%2F%2Flol";
            assertEquals(expectedUri, actualUri);
        }
        {
            instanceIdentifier.setRootName("sdc.ctxt.loc.detail");
            instanceIdentifier.setExtensionName("%2F%2Flol");

            var locationDetail = createLocationDetail(null, null, null, null, null, null);
            final String actualUri = LocationDetailQueryMapper.createWithLocationDetailQuery(instanceIdentifier, locationDetail);

            var expectedUri = "sdc.ctxt.loc:/sdc.ctxt.loc.detail/%252F%252Flol";
            assertEquals(expectedUri, actualUri);
        }
    }

    @Test
    void readLocationDetailQuery() throws UriMapperParsingException {

        {
            assertThrows(UriMapperParsingException.class,
                    () -> LocationDetailQueryMapper.readLocationDetailQuery("sdc.ctxt.loc:?fac=1?"));
        }
        {
            assertThrows(UriMapperParsingException.class,
                    () -> LocationDetailQueryMapper.readLocationDetailQuery("sdc.ctxt.loc://@@host:NoPort?fac=1"));
        }
        {
            LocationDetailQueryMapper.readLocationDetailQuery("sdc.ctxt.loc:?fac=123");
        }
        {
            assertThrows(UriMapperParsingException.class,
                    () -> LocationDetailQueryMapper.readLocationDetailQuery("sdc.ctxt.loc:?fac=1&"));
        }
        {
            assertThrows(UriMapperParsingException.class,
                    () -> LocationDetailQueryMapper.readLocationDetailQuery("sdc.ctxt.loc:?fac=1&fac=1"));
        }
        {
            final String uri = "sdc.ctxt.loc:/http:%2F%2Froot/?fac=facility1&bldng=building1&poc=poc1&flr=floor1&rm=room1&bed=bed1";
            final LocationDetail locationDetail = LocationDetailQueryMapper.readLocationDetailQuery(uri);
            assertEquals("facility1", locationDetail.getFacility());
            assertEquals("building1", locationDetail.getBuilding());
            assertEquals("poc1", locationDetail.getPoC());
            assertEquals("floor1", locationDetail.getFloor());
            assertEquals("room1", locationDetail.getRoom());
            assertEquals("bed1", locationDetail.getBed());
        }
        {
            final String uri = "sdc.ctxt.loc:/http:%2F%2Froot/?bldng=building1&poc=poc1&flr=floor1&bed=bed1";
            final LocationDetail locationDetail = LocationDetailQueryMapper.readLocationDetailQuery(uri);
            assertNull(locationDetail.getFacility());
            assertEquals("building1", locationDetail.getBuilding());
            assertEquals("poc1", locationDetail.getPoC());
            assertEquals("floor1", locationDetail.getFloor());
            assertNull(locationDetail.getRoom());
            assertEquals("bed1", locationDetail.getBed());
        }
        {
            final String uri = "sdc.ctxt.loc:/http:%2F%2Froot/?";
            final LocationDetail locationDetail = LocationDetailQueryMapper.readLocationDetailQuery(uri);
            assertNull(locationDetail.getFacility());
            assertNull(locationDetail.getBuilding());
            assertNull(locationDetail.getPoC());
            assertNull(locationDetail.getFloor());
            assertNull(locationDetail.getRoom());
            assertNull(locationDetail.getBed());
        }
        {
            final String uri = "sdc.ctxt.loc:/http:%2F%2Froot/?rm=%C3%BC";
            final LocationDetail locationDetail = LocationDetailQueryMapper.readLocationDetailQuery(uri);
            assertNull(locationDetail.getFacility());
            assertNull(locationDetail.getBuilding());
            assertNull(locationDetail.getPoC());
            assertNull(locationDetail.getFloor());
            assertEquals("ü", locationDetail.getRoom());
            assertNull(locationDetail.getBed());
        }
    }

    private LocationDetail createLocationDetail(@Nullable String facility,
                                                @Nullable String building,
                                                @Nullable String floor,
                                                @Nullable String poc,
                                                @Nullable String room,
                                                @Nullable String bed) {
        var locationDetail = new LocationDetail();
        locationDetail.setFacility(facility);
        locationDetail.setBuilding(building);
        locationDetail.setFloor(floor);
        locationDetail.setPoC(poc);
        locationDetail.setRoom(room);
        locationDetail.setBed(bed);
        return locationDetail;
    }
}