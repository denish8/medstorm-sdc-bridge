package org.somda.sdc.glue.common;

/**
 * Constants for every SDC service operation comprising input-output and input-only message exchanges.
 */
public class ActionConstants {
    public static final String ACTION_GET_MDIB = WsdlConstants.ACTION_GET_PREFIX + WsdlConstants.OPERATION_GET_MDIB;
    public static final String ACTION_GET_MD_DESCRIPTION =
            WsdlConstants.ACTION_GET_PREFIX + WsdlConstants.OPERATION_GET_MD_DESCRIPTION;
    public static final String ACTION_GET_MD_STATE =
            WsdlConstants.ACTION_GET_PREFIX + WsdlConstants.OPERATION_GET_MD_STATE;

    public static final String ACTION_SET_VALUE = WsdlConstants.ACTION_SET_PREFIX + WsdlConstants.OPERATION_SET_VALUE;
    public static final String ACTION_SET_STRING = WsdlConstants.ACTION_SET_PREFIX + WsdlConstants.OPERATION_SET_STRING;
    public static final String ACTION_ACTIVATE = WsdlConstants.ACTION_SET_PREFIX + WsdlConstants.OPERATION_ACTIVATE;
    public static final String ACTION_SET_ALERT_STATE =
            WsdlConstants.ACTION_SET_PREFIX + WsdlConstants.OPERATION_SET_ALERT_STATE;
    public static final String ACTION_SET_COMPONENT_STATE =
            WsdlConstants.ACTION_SET_PREFIX + WsdlConstants.OPERATION_SET_COMPONENT_STATE;
    public static final String ACTION_SET_METRIC_STATE =
            WsdlConstants.ACTION_SET_PREFIX + WsdlConstants.OPERATION_SET_METRIC_STATE;
    public static final String ACTION_OPERATION_INVOKED_REPORT =
            WsdlConstants.ACTION_SET_PREFIX + WsdlConstants.OPERATION_OPERATION_INVOKED_REPORT;

    public static final String ACTION_DESCRIPTION_MODIFICATION_REPORT =
            WsdlConstants.ACTION_DESCRIPTION_EVENT_PREFIX + WsdlConstants.OPERATION_DESCRIPTION_MODIFICATION_REPORT;

    public static final String ACTION_EPISODIC_ALERT_REPORT =
            WsdlConstants.ACTION_STATE_EVENT_PREFIX + WsdlConstants.OPERATION_EPISODIC_ALERT_REPORT;
    public static final String ACTION_PERIODIC_ALERT_REPORT =
            WsdlConstants.ACTION_STATE_EVENT_PREFIX + WsdlConstants.OPERATION_PERIODIC_ALERT_REPORT;
    public static final String ACTION_EPISODIC_COMPONENT_REPORT =
            WsdlConstants.ACTION_STATE_EVENT_PREFIX + WsdlConstants.OPERATION_EPISODIC_COMPONENT_REPORT;
    public static final String ACTION_PERIODIC_COMPONENT_REPORT =
            WsdlConstants.ACTION_STATE_EVENT_PREFIX + WsdlConstants.OPERATION_PERIODIC_COMPONENT_REPORT;
    public static final String ACTION_EPISODIC_METRIC_REPORT =
            WsdlConstants.ACTION_STATE_EVENT_PREFIX + WsdlConstants.OPERATION_EPISODIC_METRIC_REPORT;
    public static final String ACTION_PERIODIC_METRIC_REPORT =
            WsdlConstants.ACTION_STATE_EVENT_PREFIX + WsdlConstants.OPERATION_PERIODIC_METRIC_REPORT;
    public static final String ACTION_EPISODIC_OPERATIONAL_STATE_REPORT =
            WsdlConstants.ACTION_STATE_EVENT_PREFIX + WsdlConstants.OPERATION_EPISODIC_OPERATIONAL_STATE_REPORT;
    public static final String ACTION_PERIODIC_OPERATIONAL_STATE_REPORT =
            WsdlConstants.ACTION_STATE_EVENT_PREFIX + WsdlConstants.OPERATION_PERIODIC_OPERATIONAL_STATE_REPORT;
    public static final String ACTION_SYSTEM_ERROR_REPORT =
            WsdlConstants.ACTION_STATE_EVENT_PREFIX + WsdlConstants.OPERATION_SYSTEM_ERROR_REPORT;

    public static final String ACTION_GET_CONTEXT_STATES =
            WsdlConstants.ACTION_CONTEXT_PREFIX + WsdlConstants.OPERATION_GET_CONTEXT_STATES;
    public static final String ACTION_SET_CONTEXT_STATE =
            WsdlConstants.ACTION_CONTEXT_PREFIX + WsdlConstants.OPERATION_SET_CONTEXT_STATE;
    public static final String ACTION_GET_CONTEXT_STATES_BY_IDENTIFICATION =
            WsdlConstants.ACTION_CONTEXT_PREFIX + WsdlConstants.OPERATION_GET_CONTEXT_STATES_BY_IDENTIFICATION;
    public static final String ACTION_GET_CONTEXT_STATES_BY_FILTER =
            WsdlConstants.ACTION_CONTEXT_PREFIX + WsdlConstants.OPERATION_GET_CONTEXT_STATES_BY_FILTER;
    public static final String ACTION_PERIODIC_CONTEXT_REPORT =
            WsdlConstants.ACTION_CONTEXT_PREFIX + WsdlConstants.OPERATION_PERIODIC_CONTEXT_REPORT;
    public static final String ACTION_EPISODIC_CONTEXT_REPORT =
            WsdlConstants.ACTION_CONTEXT_PREFIX + WsdlConstants.OPERATION_EPISODIC_CONTEXT_REPORT;

    public static final String ACTION_WAVEFORM_STREAM =
            WsdlConstants.ACTION_WAVEFORM_PREFIX + WsdlConstants.OPERATION_WAVEFORM_STREAM;
    public static final String ACTION_OBSERVED_VALUE_STREAM =
            WsdlConstants.ACTION_WAVEFORM_PREFIX + WsdlConstants.OPERATION_OBSERVED_VALUE_STREAM;

    public static final String ACTION_GET_CONTAINMENT_TREE =
            WsdlConstants.ACTION_CONTAINMENT_TREE_PREFIX + WsdlConstants.OPERATION_GET_CONTAINMENT_TREE;
    public static final String ACTION_GET_DESCRIPTOR =
            WsdlConstants.ACTION_CONTAINMENT_TREE_PREFIX + WsdlConstants.OPERATION_GET_DESCRIPTOR;

    public static final String ACTION_GET_DESCRIPTORS_FROM_ARCHIVE =
            WsdlConstants.ACTION_ARCHIVE_PREFIX + WsdlConstants.OPERATION_GET_DESCRIPTORS_FROM_ARCHIVE;
    public static final String ACTION_GET_STATES_FROM_ARCHIVE =
            WsdlConstants.ACTION_ARCHIVE_PREFIX + WsdlConstants.OPERATION_GET_STATES_FROM_ARCHIVE;

    public static final String ACTION_GET_LOCALIZED_TEXT =
            WsdlConstants.ACTION_LOCALIZATION_PREFIX + WsdlConstants.OPERATION_GET_LOCALIZED_TEXT;
    public static final String ACTION_GET_SUPPORTED_LANGUAGES =
            WsdlConstants.ACTION_LOCALIZATION_PREFIX + WsdlConstants.OPERATION_GET_SUPPORTED_LANGUAGES;

    /**
     * Generates a response action URI from an input action URI by appending 'Response'.
     *
     * @param requestActionUri the request action URI.
     * @return the response action that is the requestAction plus 'Response'.
     */
    public static String getResponseAction(String requestActionUri) {
        return requestActionUri + "Response";
    }
}
