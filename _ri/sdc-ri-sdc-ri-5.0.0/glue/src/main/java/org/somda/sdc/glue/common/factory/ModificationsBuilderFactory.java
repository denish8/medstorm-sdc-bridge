package org.somda.sdc.glue.common.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.biceps.model.participant.Mdib;
import org.somda.sdc.glue.common.DefaultStateValues;
import org.somda.sdc.glue.common.ModificationsBuilder;

import javax.annotation.Nullable;

/**
 * Factory to create {@linkplain ModificationsBuilder} instances.
 */
public interface ModificationsBuilderFactory {
    /**
     * Creates a {@linkplain ModificationsBuilder} instance.
     * <p>
     * <em>Important note: the MDIB passed to the {@linkplain ModificationsBuilder} will be modified.
     * Make sure to pass a copy if necessary.</em>
     *
     * @param mdib the MDIB used to create the modifications from.
     * @return a new {@linkplain ModificationsBuilder}.
     * @throws RuntimeException if a single state descriptor does not have a corresponding state.
     */
    ModificationsBuilder createModificationsBuilder(@Assisted Mdib mdib);

    /**
     * Creates a {@linkplain ModificationsBuilder} instance.
     * <p>
     * <em>Important note: the MDIB passed to the {@linkplain ModificationsBuilder} will be modified.
     * Make sure to pass a copy if necessary.</em>
     *
     * @param mdib                       the MDIB used to create the modifications from.
     * @param createSingleStateIfMissing if true then the builder tries to create a missing single state; if false then
     *                                   a runtime exception is thrown is a single state is missing. If single states
     *                                   are added automatically, then
     *                                   {@link org.somda.sdc.glue.common.RequiredDefaultStateValues} will be used to
     *                                   populate default values.
     * @return a new {@linkplain ModificationsBuilder} instance.
     */
    ModificationsBuilder createModificationsBuilder(@Assisted Mdib mdib,
                                                    @Assisted Boolean createSingleStateIfMissing);

    /**
     * Creates a {@linkplain ModificationsBuilder} instance.
     * <p>
     * <em>Important note: the MDIB passed to the {@linkplain ModificationsBuilder} will be modified.
     * Make sure to pass a copy if necessary.</em>
     *
     * @param mdib                       the MDIB used to create the modifications from.
     * @param createSingleStateIfMissing if true then the builder tries to create a missing single state; if false then
     *                                   a runtime exception is thrown is a single state is missing.
     * @param defaultStateValues         defines callbacks for the builder to apply default state values.
     *                                   If null, then {@link org.somda.sdc.glue.common.RequiredDefaultStateValues} will
     *                                   be used to populate default values.
     *                                   This parameter does only take effect if createSingleStateIfMissing is true.
     * @return a new {@linkplain ModificationsBuilder} instance.
     */
    ModificationsBuilder createModificationsBuilder(@Assisted Mdib mdib,
                                                    @Assisted Boolean createSingleStateIfMissing,
                                                    @Assisted @Nullable DefaultStateValues defaultStateValues);
}
