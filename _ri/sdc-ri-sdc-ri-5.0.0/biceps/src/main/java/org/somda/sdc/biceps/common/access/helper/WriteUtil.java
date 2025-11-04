package org.somda.sdc.biceps.common.access.helper;

import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.common.access.WriteDescriptionResult;
import org.somda.sdc.biceps.common.access.WriteStateResult;
import org.somda.sdc.biceps.common.event.Distributor;
import org.somda.sdc.biceps.common.storage.MdibStoragePreprocessingChain;
import org.somda.sdc.biceps.common.storage.PreprocessingException;

import java.util.Collections;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * Common code for write description and states that can be used by BICEPS providers and consumers.
 * <p>
 * <em>Remark: The operations do not copy descriptors or states.</em>
 */
public class WriteUtil {
    private final Logger log;

    private final Distributor eventDistributor;
    private final MdibStoragePreprocessingChain mdibAccessPreprocessing;
    private final ReentrantReadWriteLock readWriteLock;
    private final MdibAccess mdibAccess;

    /**
     * Constructor that accepts the dependencies in order to properly process write operations.
     *
     * @param logger                  the utility owner's logger to be used for logging.
     * @param eventDistributor        the event distributor to send event messages after write operation is done.
     * @param mdibAccessPreprocessing the preprocessing chain that is invoked before writing to the MDIB storage.
     * @param readWriteLock           a read write lock to protect against concurrent access.
     * @param mdibAccess              the MDIB access that is passed on event distribution.
     */
    public WriteUtil(Logger logger,
                     Distributor eventDistributor,
                     MdibStoragePreprocessingChain mdibAccessPreprocessing,
                     ReentrantReadWriteLock readWriteLock,
                     MdibAccess mdibAccess) {
        this.log = logger;
        this.eventDistributor = eventDistributor;
        this.mdibAccessPreprocessing = mdibAccessPreprocessing;
        this.readWriteLock = readWriteLock;
        this.mdibAccess = mdibAccess;
    }

    /**
     * Performs preprocessing, write operation and event distribution of description modifications.
     * <p>
     * The write operation gains a write lock, downgrades it to a read lock during event distribution and releases the
     * read lock by the end of the function.
     *
     * @param lockedWriteDescription   locked callback to finally write description.
     * @param descriptionModifications the description modifications to write.
     * @return a write description result that contains inserted, updated and deleted entities.
     * @throws PreprocessingException in case a consistency check or modifier fails.
     */
    public WriteDescriptionResult writeDescription(
            Function<MdibDescriptionModifications, WriteDescriptionResult> lockedWriteDescription,
            MdibDescriptionModifications descriptionModifications
    ) throws PreprocessingException {
        acquireWriteLock();

        long startTime = 0;
        if (log.isDebugEnabled()) {
            startTime = System.currentTimeMillis();
            log.debug("Start writing description");
        }

        WriteDescriptionResult modificationResult;
        try {
            mdibAccessPreprocessing.processDescriptionModifications(descriptionModifications);
            modificationResult = lockedWriteDescription.apply(descriptionModifications);

            // Return just here in order to apply MDIB version on remote MDIB writes
            if (descriptionModifications.getModifications().isEmpty()) {
                return new WriteDescriptionResult(mdibAccess.getMdibVersion(),
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
            }

            readWriteLock.readLock().lock();
        } catch (PreprocessingException e) {
            log.warn("Error while processing description modifications in chain segment {} on handle {}: {}",
                    e.getSegment(), e.getHandle(), e.getMessage());
            throw e;
        } finally {
            readWriteLock.writeLock().unlock();
        }

        long endTime = System.currentTimeMillis();
        if (log.isDebugEnabled()) {

            log.debug("MDIB version {} written in {} ms",
                    modificationResult.getMdibVersion(),
                    endTime - startTime);
        }

        try {
            eventDistributor.sendDescriptionModificationEvent(
                    mdibAccess,
                    modificationResult.getInsertedEntities(),
                    modificationResult.getUpdatedEntities(),
                    modificationResult.getDeletedEntities());
        } finally {
            readWriteLock.readLock().unlock();
        }

        if (log.isDebugEnabled()) {
            log.debug("Distributing changes with {} took {} ms",
                    modificationResult.getMdibVersion(),
                    System.currentTimeMillis() - endTime);
        }

        return modificationResult;
    }

    /**
     * Performs preprocessing, write operation and event distribution of state modifications.
     * <p>
     * The write operation gains a write lock, downgrades it to a read lock during event distribution and releases the
     * read lock by the end of the function.
     *
     * @param lockedWriteStates  locked callback to finally write states.
     * @param stateModifications the state modifications to write.
     * @return a write state result that contains updated states.
     * @throws PreprocessingException in case a consistency check or modifier fails.
     */
    public WriteStateResult writeStates(Function<MdibStateModifications, WriteStateResult> lockedWriteStates,
                                        MdibStateModifications stateModifications) throws PreprocessingException {
        acquireWriteLock();

        long startTime = 0;
        if (log.isDebugEnabled()) {
            startTime = System.currentTimeMillis();
            log.debug("Start writing states");
        }

        WriteStateResult modificationResult;
        try {
            mdibAccessPreprocessing.processStateModifications(stateModifications);
            modificationResult = lockedWriteStates.apply(stateModifications);

            // Return just here in order to apply MDIB version on remote MDIB writes
            if (stateModifications.getStates().isEmpty()) {
                return new WriteStateResult(mdibAccess.getMdibVersion(), Collections.emptyMap());
            }

            readWriteLock.readLock().lock();
        } catch (PreprocessingException e) {
            log.warn("Error while processing state modifications in chain segment {} on handle {}: {}",
                    e.getSegment(), e.getHandle(), e.getMessage());
            throw e;
        } finally {
            readWriteLock.writeLock().unlock();
        }

        long endTime = System.currentTimeMillis();
        if (log.isDebugEnabled()) {

            log.debug("MDIB version {} written in {} ms",
                    modificationResult.getMdibVersion(),
                    endTime - startTime);
        }

        try {
            eventDistributor.sendStateModificationEvent(
                    mdibAccess,
                    stateModifications.getChangeType(),
                    modificationResult.getStates());
        } finally {
            readWriteLock.readLock().unlock();
        }

        if (log.isDebugEnabled()) {
            log.debug("Distributing changes {} took {} ms",
                    modificationResult.getMdibVersion(),
                    System.currentTimeMillis() - endTime);
        }

        return modificationResult;
    }

    private void acquireWriteLock() {
        log.debug("Trying to acquire write lock");
        if (readWriteLock.getReadHoldCount() > 0) {
            throw new IllegalThreadStateException(
                    "Tried to invoke write operation with read lock held by the current thread present."
                            + " Check if a write description or state function has been executed within"
                            + " a read transaction context."
            );
        }

        if (!readWriteLock.isWriteLockedByCurrentThread()) {
            readWriteLock.writeLock().lock();
        }
    }
}
