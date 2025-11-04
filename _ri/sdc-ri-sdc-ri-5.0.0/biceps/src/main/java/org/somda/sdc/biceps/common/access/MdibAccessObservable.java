package org.somda.sdc.biceps.common.access;

/**
 * Interface to allow registration and unregistration of {@linkplain MdibAccessObserver} instances.
 */
public interface MdibAccessObservable {
    /**
     * Register for MDIB modification reports.
     *
     * @param observer the observer to subscribe.
     */
    void registerObserver(MdibAccessObserver observer);

    /**
     * Unregister from MDIB modification reports.
     *
     * @param observer the observer to unsubscribe.
     */
    void unregisterObserver(MdibAccessObserver observer);

    /**
     * Unregisters all observers at once.
     */
    void unregisterAllObservers();
}
