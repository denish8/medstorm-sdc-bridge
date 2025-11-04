package org.somda.sdc.glue.consumer;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.glue.common.SubscribableActionsMapping;
import org.somda.sdc.glue.common.WsdlConstants;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Container to provide connection options for remote SDC device connections.
 *
 * @see SdcRemoteDevicesConnector#connect(HostingServiceProxy, ConnectConfiguration)
 */
public class ConnectConfiguration {


    /**
     * List of all port types shipped with SDC.
     */
    public static final Collection<QName> PORT_TYPES = List.of(
            WsdlConstants.PORT_TYPE_GET_QNAME,
            WsdlConstants.PORT_TYPE_SET_QNAME,
            WsdlConstants.PORT_TYPE_DESCRIPTION_EVENT_QNAME,
            WsdlConstants.PORT_TYPE_STATE_EVENT_QNAME,
            WsdlConstants.PORT_TYPE_CONTEXT_QNAME,
            WsdlConstants.PORT_TYPE_WAVEFORM_QNAME,
            WsdlConstants.PORT_TYPE_CONTAINMENT_TREE_QNAME,
            WsdlConstants.PORT_TYPE_ARCHIVE_QNAME,
            WsdlConstants.PORT_TYPE_LOCALIZATION_QNAME);

    /**
     * List of all episodic report actions.
     */
    public static final Collection<String> EPISODIC_REPORTS = List.of(
            ActionConstants.ACTION_EPISODIC_ALERT_REPORT,
            ActionConstants.ACTION_EPISODIC_COMPONENT_REPORT,
            ActionConstants.ACTION_EPISODIC_CONTEXT_REPORT,
            ActionConstants.ACTION_EPISODIC_METRIC_REPORT,
            ActionConstants.ACTION_EPISODIC_OPERATIONAL_STATE_REPORT,
            ActionConstants.ACTION_DESCRIPTION_MODIFICATION_REPORT,
            ActionConstants.ACTION_OPERATION_INVOKED_REPORT,
            ActionConstants.ACTION_SYSTEM_ERROR_REPORT);

    /**
     * List of all periodic report actions.
     */
    public static final Collection<String> PERIODIC_REPORTS = List.of(
            ActionConstants.ACTION_PERIODIC_ALERT_REPORT,
            ActionConstants.ACTION_PERIODIC_COMPONENT_REPORT,
            ActionConstants.ACTION_PERIODIC_CONTEXT_REPORT,
            ActionConstants.ACTION_PERIODIC_METRIC_REPORT,
            ActionConstants.ACTION_PERIODIC_OPERATIONAL_STATE_REPORT);

    /**
     * List of all streaming actions.
     */
    public static final Collection<String> STREAMING_REPORTS = List.of(
            ActionConstants.ACTION_OBSERVED_VALUE_STREAM,
            ActionConstants.ACTION_WAVEFORM_STREAM);

    /**
     * Commonly used episodic actions for remote SDC device synchronization.
     * <p>
     * Comprises all episodic reports plus waveforms.
     *
     * @see #EPISODIC_REPORTS
     */
    public static final Collection<String> ALL_EPISODIC_AND_WAVEFORM_REPORTS;

    /**
     * Commonly used actions if only updates on description and contexts are desired.
     * <p>
     * This can be used in order to watch remote devices without receiving all of their data.
     * Note that this setup includes operation invoked reports by default.
     */
    public static final Collection<String> DESCRIPTION_AND_CONTEXTS = Collections.unmodifiableCollection(Arrays.asList(
            ActionConstants.ACTION_EPISODIC_CONTEXT_REPORT,
            ActionConstants.ACTION_DESCRIPTION_MODIFICATION_REPORT,
            ActionConstants.ACTION_OPERATION_INVOKED_REPORT));

    /**
     * Commonly used periodic actions for remote SDC device synchronization.
     * <p>
     * Comprises all periodic reports plus waveforms.
     *
     * @see #PERIODIC_REPORTS
     */
    public static final Collection<String> ALL_PERIODIC_AND_WAVEFORM_REPORTS;

    private static final Logger LOG = LogManager.getLogger(ConnectConfiguration.class);

    static {
        var allEpisodicAndWaveformReports = new ArrayList<>(EPISODIC_REPORTS);
        allEpisodicAndWaveformReports.add(ActionConstants.ACTION_WAVEFORM_STREAM);
        ALL_EPISODIC_AND_WAVEFORM_REPORTS = Collections.unmodifiableCollection(allEpisodicAndWaveformReports);
        var allPeriodicAndWaveformReports = new ArrayList<>(PERIODIC_REPORTS);
        allPeriodicAndWaveformReports.add(ActionConstants.ACTION_WAVEFORM_STREAM);
        ALL_PERIODIC_AND_WAVEFORM_REPORTS = Collections.unmodifiableCollection(allPeriodicAndWaveformReports);
    }


    private Collection<String> actions;
    private Collection<QName> requiredPortTypes;

    private ConnectConfiguration(Collection<String> actions,
                                 Collection<QName> requiredPortTypes) {
        this.actions = new ArrayList<>(actions);
        this.requiredPortTypes = new ArrayList<>(requiredPortTypes);
        this.requiredPortTypes.add(WsdlConstants.PORT_TYPE_GET_QNAME);
        this.requiredPortTypes.addAll(findRequiredQNamesBasedOnActions(this.actions));
    }

    /**
     * Creates a configuration that subscribes nothing.
     * <p>
     * The configuration automatically requires the get service to be existing.
     *
     * @return the new connect configuration.
     */
    public static ConnectConfiguration create() {
        return new ConnectConfiguration(Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Creates a configuration with predefined actions.
     * <p>
     * The configuration automatically requires all port types required by the given actions plus the get service.
     *
     * @param actions the action URIs to be subscribed.
     * @return the new connect configuration.
     */
    public static ConnectConfiguration create(Collection<String> actions) {
        return new ConnectConfiguration(actions, Collections.emptyList());
    }

    /**
     * Creates a configuration with predefined actions.
     *
     * @param actions           the action URIs to be subscribed.
     * @param requiredPortTypes the required service interfaces used to establish a connection.
     *                          The get service QName will be added to the required port types collection as minimum
     *                          requirement.
     *                          The port type collection will be appended automatically by the port types required
     *                          to subscribe the given actions.
     * @return the new connect configuration.
     */
    public static ConnectConfiguration create(Collection<String> actions,
                                              Collection<QName> requiredPortTypes) {
        return new ConnectConfiguration(actions, requiredPortTypes);
    }


    public Collection<String> getActions() {
        return actions;
    }

    public Collection<QName> getRequiredPortTypes() {
        return requiredPortTypes;
    }

    private Collection<QName> findRequiredQNamesBasedOnActions(Collection<String> actions) {
        final Set<QName> qNames = new HashSet<>();
        actions.forEach(action -> {
            final QName qName = SubscribableActionsMapping.TARGET_QNAMES.get(action);
            if (qName != null) {
                qNames.add(qName);
            }
        });
        if (qNames.isEmpty()) {
            LOG.warn("No matching QNames found for actions {}", () -> Arrays.toString(actions.toArray()));
        }
        return qNames;
    }
}
