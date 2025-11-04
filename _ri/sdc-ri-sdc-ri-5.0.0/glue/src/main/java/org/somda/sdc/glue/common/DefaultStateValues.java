package org.somda.sdc.glue.common;

import org.somda.sdc.biceps.model.participant.Mdib;

/**
 * Implementing classes of this interface can be used to define default values for states.
 * <p>
 * This interface enables {@link ModificationsBuilder} instances to transform an
 * {@link Mdib} into a modifications set in which every generated state
 * visits a callback function of the implementing class.
 * <p>
 * For each state type that is supposed to have a default value, a class derived from {@linkplain DefaultStateValues}
 * is recognized by a {@link ModificationsBuilder} if it contains functions of the following pattern:
 * <pre>
 * void onFoo(AbstractMetricState state) {
 *     // modify any abstract metric state type here
 *     // NumericMetricState is not affected as there is another callback defined
 * }
 * void onBar(NumericMetricState state) {
 *     // Every NumericMetricState goes to this callback
 * }
 * </pre>
 * The first method is called for each passed {@link org.somda.sdc.biceps.model.participant.AbstractMetricState}.
 * Please note that this function is not called in case there is a method declaration with a type that derives from
 * {@link org.somda.sdc.biceps.model.participant.AbstractMetricState}.
 * In that case the method needs to call the more generated callback itself.
 * The second method is called for each {@link org.somda.sdc.biceps.model.participant.NumericMetricState}.
 *
 * @see org.somda.sdc.glue.common.factory.ModificationsBuilderFactory#createModificationsBuilder(
 *Mdib, Boolean, DefaultStateValues)
 */
public interface DefaultStateValues {
}
