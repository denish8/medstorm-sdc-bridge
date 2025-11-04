package org.somda.sdc.biceps.common.access;

import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.common.model.ObjectStringifier;
import org.somda.sdc.common.model.Stringified;

import java.util.Collections;
import java.util.List;

/**
 * Read-only result set of a write description call.
 */
public class WriteDescriptionResult {
    @Stringified
    private final MdibVersion mdibVersion;
    private final List<MdibEntity> insertedEntities;
    private final List<MdibEntity> updatedEntities;
    private final List<MdibEntity> deletedEntities;

    /**
     * Constructor to initialize all values of the result set.
     *
     * @param mdibVersion      the MDIB version.
     * @param insertedEntities all inserted entities.
     * @param updatedEntities  all updated entities.
     * @param deletedEntities  all deleted entities.
     */
    public WriteDescriptionResult(MdibVersion mdibVersion,
                                  List<MdibEntity> insertedEntities,
                                  List<MdibEntity> updatedEntities,
                                  List<MdibEntity> deletedEntities) {
        this.mdibVersion = mdibVersion;
        this.insertedEntities = Collections.unmodifiableList(insertedEntities);
        this.updatedEntities = Collections.unmodifiableList(updatedEntities);
        this.deletedEntities = Collections.unmodifiableList(deletedEntities);
    }

    /**
     * Gets the MDIB version that ensued during the preceding write operation.
     *
     * @return the MDIB version.
     */
    public MdibVersion getMdibVersion() {
        return mdibVersion;
    }

    /**
     * Gets all inserted entities.
     *
     * @return the entities.
     */
    public List<MdibEntity> getInsertedEntities() {
        return insertedEntities;
    }

    /**
     * Gets all updated entities.
     *
     * @return the entities.
     */
    public List<MdibEntity> getUpdatedEntities() {
        return updatedEntities;
    }

    /**
     * Gets all deleted entities.
     *
     * @return the entities.
     */
    public List<MdibEntity> getDeletedEntities() {
        return deletedEntities;
    }

    @Override
    public String toString() {
        return ObjectStringifier.stringify(this);
    }
}
