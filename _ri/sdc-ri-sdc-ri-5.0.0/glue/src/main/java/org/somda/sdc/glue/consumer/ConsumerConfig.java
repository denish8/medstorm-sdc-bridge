package org.somda.sdc.glue.consumer;

/**
 * Configuration for the SDC consumer side.
 *
 * @see org.somda.sdc.glue.guice.DefaultGlueConfigModule
 */
public class ConsumerConfig {
    /**
     * Configures the period for {@linkplain SdcRemoteDevice} watchdog jobs.
     *
     * <ul>
     * <li>Data type: {@linkplain java.time.Duration}
     * <li>Use: optional
     * </ul>
     */
    public static final String WATCHDOG_PERIOD = "SdcGlue.Consumer.WatchdogPeriod";

    /**
     * Configures the default expiration time requested for subscribe requests.
     *
     * <ul>
     * <li>Data type: {@linkplain java.time.Duration}
     * <li>Use: optional
     * </ul>
     */
    public static final String REQUESTED_EXPIRES = "SdcGlue.Consumer.RequestedExpires";

    /**
     * Default timeout for awaiting of transaction objects.
     * <p>
     * This duration is used to sort out stale reports.
     * Its value defines the threshold from which on a report counts as stale.
     *
     * <ul>
     * <li>Data type: {@linkplain java.time.Duration}
     * <li>Use: optional
     * </ul>
     */
    public static final String AWAITING_TRANSACTION_TIMEOUT = "SdcGlue.Consumer.AwaitingTransactionTimeout";


    /**
     * Enable applying reports which have the same MDIB version as the current
     * {@linkplain org.somda.sdc.biceps.common.storage.MdibStorage}.
     * <p>
     * This useful for testing purposes, as there are requirements where this behavior is of interest.
     *
     * <ul>
     * <li>Data type: {@linkplain Boolean}
     * <li>Use: optional
     * </ul>
     */
    public static final String APPLY_REPORTS_SAME_MDIB_VERSION = "SdcGlue.Consumer.ApplyReportsWithSameMdibVersion";
}
