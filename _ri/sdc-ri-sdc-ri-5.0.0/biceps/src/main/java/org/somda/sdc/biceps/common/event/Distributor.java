package org.somda.sdc.biceps.common.event;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.event.EventBus;
import org.somda.sdc.common.logging.InstanceLogger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

/**
 * Utility class to distribute any BICEPS MDIB events.
 */
public class Distributor {
    private static final Logger LOG = LogManager.getLogger(Distributor.class);

    private final EventBus eventBus;
    private final Logger instanceLogger;

    @Inject
    Distributor(EventBus eventBus,
                @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.eventBus = eventBus;
    }

    /**
     * Registers an observer to MDIB modification events.
     *
     * @param observer to unregister
     */
    public void registerObserver(Object observer) {
        eventBus.register(observer);
    }

    /**
     * Unregisters an observer from MDIB modification events.
     *
     * @param observer to unregister
     */
    public void unregisterObserver(Object observer) {
        eventBus.unregister(observer);
    }

    /**
     * Unregisters all observers at once.
     */
    public void unregisterAllObservers() {
        eventBus.unregisterAll();
    }

    /**
     * Creates a {@linkplain DescriptionModificationMessage} and sends it to all subscribers.
     *
     * @param mdibAccess       the MDIB access for {@link AbstractMdibAccessMessage}.
     * @param insertedEntities all inserted entities.
     * @param updatedEntities  all updated entities.
     * @param deletedEntities  all deleted entities.
     */
    public void sendDescriptionModificationEvent(MdibAccess mdibAccess,
                                                 List<MdibEntity> insertedEntities,
                                                 List<MdibEntity> updatedEntities,
                                                 List<MdibEntity> deletedEntities) {
        eventBus.post(
                new DescriptionModificationMessage(mdibAccess, insertedEntities, updatedEntities, deletedEntities)
        );
    }

    /**
     * Creates a specific {@linkplain StateModificationMessage} based on the change type and
     * sends it to all subscribers.
     *
     * @param mdibAccess the MDIB access for {@link AbstractMdibAccessMessage}.
     * @param changeType the change type where to derive the message type from.
     * @param states     all updates states.
     */
    public void sendStateModificationEvent(
            MdibAccess mdibAccess,
            MdibStateModifications.Type changeType, Map<String, ? extends List<? extends AbstractState>> states
    ) {
        Constructor<?> ctor = null;
        for (Constructor<?> constructor : changeType.getEventMessageClass().getConstructors()) {
            if (constructor.getParameterCount() == 2) {
                ctor = constructor;
                break;
            }
        }

        if (ctor == null) {
            instanceLogger.error(
                    "Expected constructor to create state modification message not found. Distribution failed."
            );
            return;
        }

        try {
            eventBus.post(ctor.newInstance(mdibAccess, states));
        } catch (IllegalAccessException | IllegalArgumentException
                | InstantiationException | InvocationTargetException e) {
            instanceLogger.error("Failed to call state event message constructor", e);
        }
    }
}
