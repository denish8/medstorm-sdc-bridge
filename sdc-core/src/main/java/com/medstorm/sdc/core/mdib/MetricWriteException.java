package com.medstorm.sdc.core.mdib;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Thrown when a batch of metric values could not be committed to the MDIB.
 */
public class MetricWriteException extends Exception {

    public MetricWriteException(String message, Throwable cause) {
        super(message, cause);
    }

    public MetricWriteException(String message) {
        super(message);
    }

    /**
     * Thrown when a write targets a handle that does not exist in the MDIB.
     *
     * <p>This is reported rather than ignored on purpose. A typo in a handle, or an
     * MDIB that does not describe the metric being published, otherwise produces a
     * provider that runs happily and emits nothing &mdash; a failure that only shows
     * up on the consumer side, late, and looks like a network problem.
     */
    public static final class UnknownHandleException extends MetricWriteException {
        private final Set<String> handles;

        public UnknownHandleException(Set<String> handles) {
            super("No metric state in the MDIB for handle(s): " + handles
                    + ". Check that these handles exist in the loaded MDIB and that their"
                    + " descriptor type matches the write (numeric vs. string).");
            this.handles = Collections.unmodifiableSet(new LinkedHashSet<>(handles));
        }

        /** The handles that could not be resolved, in the order they were written. */
        public Set<String> getHandles() {
            return handles;
        }
    }
}
