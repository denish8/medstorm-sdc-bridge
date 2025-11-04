package org.somda.sdc.glue.common;

import javax.xml.namespace.QName;

public class CommonConstants {
    /**
     * Definition of the SDC target namespace.
     */
    public static final String NAMESPACE_SDC = "http://standards.ieee.org/downloads/11073/11073-20701-2018";

    /**
     * Prefix used for the SDC namespace.
     */
    public static final String NAMESPACE_SDC_PREFIX = "sdc";


    /**
     * Namespace mappings of the MDPWS model.
     * <p>
     * todo DGr populate MDPWS namespace mappings constant
     */
    public static final String NAMESPACE_PREFIX_MAPPINGS_MDPWS = "";

    /**
     * Namespace mappings of the BICEPS model.
     */
    public static final String NAMESPACE_PREFIX_MAPPINGS_BICEPS =
            "{" + org.somda.sdc.biceps.common.CommonConstants.NAMESPACE_EXTENSION_PREFIX + ":"
                    + org.somda.sdc.biceps.common.CommonConstants.NAMESPACE_EXTENSION + "}" +
                    "{" + org.somda.sdc.biceps.common.CommonConstants.NAMESPACE_PARTICIPANT_PREFIX + ":"
                    + org.somda.sdc.biceps.common.CommonConstants.NAMESPACE_PARTICIPANT + "}" +
                    "{" + org.somda.sdc.biceps.common.CommonConstants.NAMESPACE_MESSAGE_PREFIX + ":"
                    + org.somda.sdc.biceps.common.CommonConstants.NAMESPACE_MESSAGE + "}";

    /**
     * Namespace mappings of the SDC Glue model.
     */
    public static final String NAMESPACE_PREFIX_MAPPINGS_GLUE = "{" + NAMESPACE_SDC_PREFIX + ":" + NAMESPACE_SDC + "}";

    /**
     * Definition of the SDC MDPWS target namespace.
     */
    public static final String NAMESPACE_MDPWS = "http://standards.ieee.org/downloads/11073/11073-20702-2016";

    /**
     * Defines the MDPWS device type that is required to identify an MDPWS compliant device during discovery.
     */
    public static final QName MEDICAL_DEVICE_TYPE = new QName(NAMESPACE_MDPWS, "MedicalDevice");
}
