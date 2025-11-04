package org.somda.sdc.glue.common;

import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.biceps.model.participant.LocationDetail;
import org.somda.sdc.glue.common.helper.UrlUtf8;

import java.util.Optional;

/**
 * Utility class to create fallback instance identifiers from location detail.
 * <p>
 * The instance identifier generated with this class meet the specification of IEEE 11073-20701-2018 section 9.4.1.1.
 */
public class FallbackInstanceIdentifier {
    private static final String LOCATION_ROOT_SEGMENT = "sdc.ctxt.loc.detail";
    private static final String DELIMITER = "/";

    /**
     * Creates an instance identifier based on the fallback algorithm defined in IEEE 11073-20701-2018 section 9.4.1.1.
     *
     * @param locationDetail the location detail used to derive the instance identifier.
     * @return a converted instance identifier or {@link Optional#empty()} if no location information
     * is set.
     */
    public static Optional<InstanceIdentifier> create(LocationDetail locationDetail) {
        final String extension = UrlUtf8.encodePChars(locationDetail.getFacility())
                + DELIMITER + UrlUtf8.encodePChars(locationDetail.getBuilding())
                + DELIMITER + UrlUtf8.encodePChars(locationDetail.getFloor())
                + DELIMITER + UrlUtf8.encodePChars(locationDetail.getPoC())
                + DELIMITER + UrlUtf8.encodePChars(locationDetail.getRoom())
                + DELIMITER + UrlUtf8.encodePChars(locationDetail.getBed());
        if (!"/////".equals(extension)) {
            InstanceIdentifier instanceIdentifier = new InstanceIdentifier();
            instanceIdentifier.setRootName(LOCATION_ROOT_SEGMENT);
            instanceIdentifier.setExtensionName(extension);
            return Optional.of(instanceIdentifier);
        }

        return Optional.empty();
    }
}
