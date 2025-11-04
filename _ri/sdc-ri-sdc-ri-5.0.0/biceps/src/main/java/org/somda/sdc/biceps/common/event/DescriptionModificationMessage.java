package org.somda.sdc.biceps.common.event;

import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.common.MdibEntity;

import java.util.Collections;
import java.util.List;

/**
 * Subscribe to this message in order to receive description changes.
 * <p>
 * Message is also being used to communicate the initial set of changes from GetMdibResponse after device connection.
 */
public class DescriptionModificationMessage extends AbstractMdibAccessMessage {
    private final List<MdibEntity> insertedEntities;
    private final List<MdibEntity> updatedEntities;
    private final List<MdibEntity> deletedEntities;

    /**
     * Constructor.
     *
     * @param mdibAccess the MDIB access for {@link AbstractMdibAccessMessage}.
     * @param insertedEntities all inserted entities.
     * @param updatedEntities all updated entities.
     * @param deletedEntities all deleted entities.
     */
    public DescriptionModificationMessage(MdibAccess mdibAccess,
                                          List<MdibEntity> insertedEntities,
                                          List<MdibEntity> updatedEntities,
                                          List<MdibEntity> deletedEntities) {
        super(mdibAccess);
        this.insertedEntities = Collections.unmodifiableList(insertedEntities);
        this.updatedEntities = Collections.unmodifiableList(updatedEntities);
        this.deletedEntities = Collections.unmodifiableList(deletedEntities);
    }

    public List<MdibEntity> getInsertedEntities() {
        return insertedEntities;
    }

    public List<MdibEntity> getUpdatedEntities() {
        return updatedEntities;
    }

    public List<MdibEntity> getDeletedEntities() {
        return deletedEntities;
    }
}
