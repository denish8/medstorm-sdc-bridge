package org.somda.sdc.biceps.common;

/**
 * Common BICEPS constants.
 */
public class CommonConstants {
    /**
     * Namespace string of the BICEPS Extension Model.
     */
    public static final String NAMESPACE_EXTENSION =
            "http://standards.ieee.org/downloads/11073/11073-10207-2017/extension";

    /**
     * Prefix used for the namespace of the BICEPS Extension Model.
     */
    public static final String NAMESPACE_EXTENSION_PREFIX = "ext";

    /**
     * Namespace string of the BICEPS Participant Model.
     */
    public static final String NAMESPACE_PARTICIPANT =
            "http://standards.ieee.org/downloads/11073/11073-10207-2017/participant";

    /**
     * Prefix used for the namespace of the BICEPS Participant Model.
     */
    public static final String NAMESPACE_PARTICIPANT_PREFIX = "pm";

    /**
     * Namespace string of the BICEPS Message Model.
     */
    public static final String NAMESPACE_MESSAGE = "http://standards.ieee.org/downloads/11073/11073-10207-2017/message";

    /**
     * Prefix used for the namespace of the BICEPS Message Model.
     */
    public static final String NAMESPACE_MESSAGE_PREFIX = "msg";

    /**
     * JAXB context paths used to let JAXB recognize the BICEPS model.
     */
    public static final String BICEPS_JAXB_CONTEXT_PATH = "org.somda.sdc.biceps.model.extension:" +
            "org.somda.sdc.biceps.model.participant:" +
            "org.somda.sdc.biceps.model.message";
}
