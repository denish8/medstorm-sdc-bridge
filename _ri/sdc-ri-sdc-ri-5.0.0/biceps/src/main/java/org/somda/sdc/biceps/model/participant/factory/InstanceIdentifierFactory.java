package org.somda.sdc.biceps.model.participant.factory;

import org.somda.sdc.biceps.model.participant.InstanceIdentifier;

import javax.annotation.Nullable;

/**
 * Convenience factory to create instance identifiers.
 */
public class InstanceIdentifierFactory {
    /**
     * Creates an instance identifier solely based on the root.
     *
     * @param root the root to set up.
     * @return a new instance.
     */
    public static InstanceIdentifier createInstanceIdentifier(String root) {
        return createInstanceIdentifier(root, null);
    }

    /**
     * Creates an instance identifier.
     *
     * @param root      the root to set up or null if unknown.
     * @param extension the extension to set up or null if unknown.
     * @return a new instance.
     */
    public static InstanceIdentifier createInstanceIdentifier(@Nullable String root, @Nullable String extension) {
        var instanceIdentifier = new InstanceIdentifier();
        instanceIdentifier.setRootName(root);
        instanceIdentifier.setExtensionName(extension);
        return instanceIdentifier;
    }

    /**
     * Creates an instance identifier solely based on the extension.
     *
     * @param extension the extension to set up.
     * @return a new instance.
     */
    public static InstanceIdentifier createInstanceIdentifierWithoutRoot(String extension) {
        return createInstanceIdentifier(null, extension);
    }
}
