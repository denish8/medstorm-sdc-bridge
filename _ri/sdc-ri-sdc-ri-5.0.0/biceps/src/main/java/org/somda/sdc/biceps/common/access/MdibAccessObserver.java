package org.somda.sdc.biceps.common.access;

/**
 * Marks a class to be an MDIB changes observer.
 * <p>
 * Annotate any method with {@link com.google.common.eventbus.Subscribe} to get callbacks on
 * <ul>
 * <li>{@link org.somda.sdc.biceps.common.event.AlertStateModificationMessage}
 * <li>{@link org.somda.sdc.biceps.common.event.ComponentStateModificationMessage}
 * <li>{@link org.somda.sdc.biceps.common.event.ContextStateModificationMessage}
 * <li>{@link org.somda.sdc.biceps.common.event.DescriptionModificationMessage}
 * <li>{@link org.somda.sdc.biceps.common.event.MetricStateModificationMessage}
 * <li>{@link org.somda.sdc.biceps.common.event.OperationStateModificationMessage}
 * <li>{@link org.somda.sdc.biceps.common.event.WaveformStateModificationMessage}
 * </ul>
 */
public interface MdibAccessObserver {
}
