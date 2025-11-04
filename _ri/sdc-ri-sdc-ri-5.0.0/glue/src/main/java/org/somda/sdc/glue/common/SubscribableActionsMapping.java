package org.somda.sdc.glue.common;

import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SubscribableActionsMapping {
    public static final Map<String, QName> TARGET_QNAMES;

    static {
        var qnames = new HashMap<String, QName>();
        qnames.put(ActionConstants.ACTION_OPERATION_INVOKED_REPORT, WsdlConstants.PORT_TYPE_SET_QNAME);

        qnames.put(ActionConstants.ACTION_DESCRIPTION_MODIFICATION_REPORT,
                WsdlConstants.PORT_TYPE_DESCRIPTION_EVENT_QNAME);

        qnames.put(ActionConstants.ACTION_EPISODIC_ALERT_REPORT, WsdlConstants.PORT_TYPE_STATE_EVENT_QNAME);
        qnames.put(ActionConstants.ACTION_PERIODIC_ALERT_REPORT, WsdlConstants.PORT_TYPE_STATE_EVENT_QNAME);
        qnames.put(ActionConstants.ACTION_EPISODIC_COMPONENT_REPORT, WsdlConstants.PORT_TYPE_STATE_EVENT_QNAME);
        qnames.put(ActionConstants.ACTION_PERIODIC_COMPONENT_REPORT, WsdlConstants.PORT_TYPE_STATE_EVENT_QNAME);
        qnames.put(ActionConstants.ACTION_EPISODIC_METRIC_REPORT, WsdlConstants.PORT_TYPE_STATE_EVENT_QNAME);
        qnames.put(ActionConstants.ACTION_PERIODIC_METRIC_REPORT, WsdlConstants.PORT_TYPE_STATE_EVENT_QNAME);
        qnames.put(ActionConstants.ACTION_EPISODIC_OPERATIONAL_STATE_REPORT, WsdlConstants.PORT_TYPE_STATE_EVENT_QNAME);
        qnames.put(ActionConstants.ACTION_PERIODIC_OPERATIONAL_STATE_REPORT, WsdlConstants.PORT_TYPE_STATE_EVENT_QNAME);
        qnames.put(ActionConstants.ACTION_SYSTEM_ERROR_REPORT, WsdlConstants.PORT_TYPE_STATE_EVENT_QNAME);

        qnames.put(ActionConstants.ACTION_PERIODIC_CONTEXT_REPORT, WsdlConstants.PORT_TYPE_CONTEXT_QNAME);
        qnames.put(ActionConstants.ACTION_EPISODIC_CONTEXT_REPORT, WsdlConstants.PORT_TYPE_CONTEXT_QNAME);

        qnames.put(ActionConstants.ACTION_WAVEFORM_STREAM, WsdlConstants.PORT_TYPE_WAVEFORM_QNAME);
        qnames.put(ActionConstants.ACTION_OBSERVED_VALUE_STREAM, WsdlConstants.PORT_TYPE_WAVEFORM_QNAME);

        TARGET_QNAMES = Collections.unmodifiableMap(qnames);
    }
}
