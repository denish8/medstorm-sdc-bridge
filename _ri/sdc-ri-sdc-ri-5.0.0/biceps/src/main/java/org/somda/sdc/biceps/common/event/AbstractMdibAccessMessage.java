package org.somda.sdc.biceps.common.event;

import org.somda.sdc.biceps.common.access.MdibAccess;

/**
 * Abstract message definition that provides MDIB access.
 * <p>
 * Getting the MDIB access during an event call allows to access the MDIB without storing a reference to it in the
 * processing class.
 * <em>Attention: the MDIB access is supposed to be used only during the event callback as the data is subject to be
 * changed after the callback has ended.</em>
 */
public abstract class AbstractMdibAccessMessage {
    private final MdibAccess mdibAccess;

    /**
     * Constructor that accepts the MDIB access to be provided by {@link #getMdibAccess()}.
     *
     * @param mdibAccess the MDIB access to provide.
     */
    protected AbstractMdibAccessMessage(MdibAccess mdibAccess) {
        this.mdibAccess = mdibAccess;
    }

    public MdibAccess getMdibAccess() {
        return mdibAccess;
    }
}
