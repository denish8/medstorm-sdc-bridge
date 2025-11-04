package org.somda.sdc.glue.common;

import javax.xml.namespace.QName;

/**
 * Constants used throughout high priority and low priority WSDL files.
 * <p>
 * Final action URIs are excluded from this file.
 *
 * @see ActionConstants
 */
public class WsdlConstants {
    // CHECKSTYLE.OFF: DeclarationOrder
    private static final String SLASH = "/";

    public static final String TARGET_NAMESPACE = CommonConstants.NAMESPACE_SDC;

    public static final String ACTION_PREFIX = TARGET_NAMESPACE + SLASH;

    public static final String SERVICE_GET = "GetService";
    public static final String SERVICE_SET = "SetService";
    public static final String SERVICE_DESCRIPTION_EVENT = "DescriptionEventService";
    public static final String SERVICE_STATE_EVENT = "StateEventService";
    public static final String SERVICE_CONTEXT = "ContextService";
    public static final String SERVICE_WAVEFORM = "WaveformService";
    public static final String SERVICE_CONTAINMENT_TREE = "ContainmentTreeService";
    public static final String SERVICE_ARCHIVE = "ArchiveService";
    public static final String SERVICE_LOCALIZATION = "LocalizationService";

    public static final String ACTION_GET_PREFIX = ACTION_PREFIX + SERVICE_GET + SLASH;
    public static final String ACTION_SET_PREFIX = ACTION_PREFIX + SERVICE_SET + SLASH;
    public static final String ACTION_DESCRIPTION_EVENT_PREFIX = ACTION_PREFIX + SERVICE_DESCRIPTION_EVENT + SLASH;
    public static final String ACTION_STATE_EVENT_PREFIX = ACTION_PREFIX + SERVICE_STATE_EVENT + SLASH;
    public static final String ACTION_CONTEXT_PREFIX = ACTION_PREFIX + SERVICE_CONTEXT + SLASH;
    public static final String ACTION_WAVEFORM_PREFIX = ACTION_PREFIX + SERVICE_WAVEFORM + SLASH;
    public static final String ACTION_CONTAINMENT_TREE_PREFIX = ACTION_PREFIX + SERVICE_CONTAINMENT_TREE + SLASH;
    public static final String ACTION_ARCHIVE_PREFIX = ACTION_PREFIX + SERVICE_ARCHIVE + SLASH;
    public static final String ACTION_LOCALIZATION_PREFIX = ACTION_PREFIX + SERVICE_LOCALIZATION + SLASH;

    public static final String OPERATION_GET_MDIB = "GetMdib";
    public static final String OPERATION_GET_MD_DESCRIPTION = "GetMdDescription";
    public static final String OPERATION_GET_MD_STATE = "GetMdState";

    public static final String OPERATION_SET_VALUE = "SetValue";
    public static final String OPERATION_SET_STRING = "SetString";
    public static final String OPERATION_ACTIVATE = "Activate";
    public static final String OPERATION_SET_ALERT_STATE = "SetAlertState";
    public static final String OPERATION_SET_COMPONENT_STATE = "SetComponentState";
    public static final String OPERATION_SET_METRIC_STATE = "SetMetricState";
    public static final String OPERATION_OPERATION_INVOKED_REPORT = "OperationInvokedReport";

    public static final String OPERATION_DESCRIPTION_MODIFICATION_REPORT = "DescriptionModificationReport";

    public static final String OPERATION_EPISODIC_ALERT_REPORT = "EpisodicAlertReport";
    public static final String OPERATION_PERIODIC_ALERT_REPORT = "PeriodicAlertReport";
    public static final String OPERATION_EPISODIC_COMPONENT_REPORT = "EpisodicComponentReport";
    public static final String OPERATION_PERIODIC_COMPONENT_REPORT = "PeriodicComponentReport";
    public static final String OPERATION_EPISODIC_METRIC_REPORT = "EpisodicMetricReport";
    public static final String OPERATION_PERIODIC_METRIC_REPORT = "PeriodicMetricReport";
    public static final String OPERATION_EPISODIC_OPERATIONAL_STATE_REPORT = "EpisodicOperationalStateReport";
    public static final String OPERATION_PERIODIC_OPERATIONAL_STATE_REPORT = "PeriodicOperationalStateReport";
    public static final String OPERATION_SYSTEM_ERROR_REPORT = "SystemErrorReport";

    public static final String OPERATION_GET_CONTEXT_STATES = "GetContextStates";
    public static final String OPERATION_SET_CONTEXT_STATE = "SetContextState";
    public static final String OPERATION_GET_CONTEXT_STATES_BY_IDENTIFICATION = "GetContextStatesByIdentification";
    public static final String OPERATION_GET_CONTEXT_STATES_BY_FILTER = "GetContextStatesByFilter";
    public static final String OPERATION_EPISODIC_CONTEXT_REPORT = "EpisodicContextReport";
    public static final String OPERATION_PERIODIC_CONTEXT_REPORT = "PeriodicContextReport";

    public static final String OPERATION_WAVEFORM_STREAM = "WaveformStream";
    public static final String OPERATION_OBSERVED_VALUE_STREAM = "ObservedValueStream";

    public static final String OPERATION_GET_CONTAINMENT_TREE = "GetContainmentTree";
    public static final String OPERATION_GET_DESCRIPTOR = "GetDescriptor";

    public static final String OPERATION_GET_DESCRIPTORS_FROM_ARCHIVE = "GetDescriptorsFromArchive";
    public static final String OPERATION_GET_STATES_FROM_ARCHIVE = "GetStatesFromArchive";

    public static final String OPERATION_GET_LOCALIZED_TEXT = "GetLocalizedText";
    public static final String OPERATION_GET_SUPPORTED_LANGUAGES = "GetSupportedLanguages";

    public static final QName PORT_TYPE_GET_QNAME = new QName(TARGET_NAMESPACE, SERVICE_GET);
    public static final QName PORT_TYPE_SET_QNAME = new QName(TARGET_NAMESPACE, SERVICE_SET);
    public static final QName PORT_TYPE_DESCRIPTION_EVENT_QNAME =
            new QName(TARGET_NAMESPACE, SERVICE_DESCRIPTION_EVENT);
    public static final QName PORT_TYPE_STATE_EVENT_QNAME = new QName(TARGET_NAMESPACE, SERVICE_STATE_EVENT);
    public static final QName PORT_TYPE_CONTEXT_QNAME = new QName(TARGET_NAMESPACE, SERVICE_CONTEXT);
    public static final QName PORT_TYPE_WAVEFORM_QNAME = new QName(TARGET_NAMESPACE, SERVICE_WAVEFORM);
    public static final QName PORT_TYPE_CONTAINMENT_TREE_QNAME = new QName(TARGET_NAMESPACE, SERVICE_CONTAINMENT_TREE);
    public static final QName PORT_TYPE_ARCHIVE_QNAME = new QName(TARGET_NAMESPACE, SERVICE_ARCHIVE);
    public static final QName PORT_TYPE_LOCALIZATION_QNAME = new QName(TARGET_NAMESPACE, SERVICE_LOCALIZATION);
    // CHECKSTYLE.ON: DeclarationOrder

}
