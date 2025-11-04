package org.somda.sdc.biceps.common.access;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.kscs.util.jaxb.Copyable;
import org.somda.sdc.biceps.common.CommonConfig;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibStateModifications;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A utility class to deep copy any input and output if configured.
 * <p>
 * <b>Problem description:</b>
 * <p>
 * Any JAXB converted classes come with getters and setters, hence it is not possible to create read only entities as
 * one can always access a descriptor or state of an entity and change it if desired.
 * By copying incoming and outgoing data the MDIB entities are protected against inadvertent or deliberate
 * modification at the cost of performance.
 * If performance plays a role and it can be guaranteed that MDIB data will not be modified at all, then
 * {@link CommonConfig#COPY_MDIB_INPUT} and {@link CommonConfig#COPY_MDIB_OUTPUT} can be configured such that no copies
 * of descriptors and states are made throughout the MDIB.
 * <p>
 * Default configuration is <em>yes, copy input and output data</em>.
 */
public class CopyManager {
    private final Boolean copyInput;
    private final Boolean copyOutput;

    @Inject
    CopyManager(@Named(CommonConfig.COPY_MDIB_INPUT) Boolean copyInput,
                @Named(CommonConfig.COPY_MDIB_OUTPUT) Boolean copyOutput) {
        this.copyInput = copyInput;
        this.copyOutput = copyOutput;
    }

    /**
     * Copies input data if configured.
     * <p>
     * Data is copied if {@link CommonConfig#COPY_MDIB_INPUT} is configured true.
     * Data is forwarded otherwise.
     *
     * @param input data to be copied.
     * @param <T>   any type that is supposed to be deep-copied.
     * @return a copy of {@code input} if configured, otherwise a forwarded reference.
     */
    public <T extends Copyable<T>> T processInput(T input) {
        if (!copyInput) {
            return input;
        }

        if (input instanceof MdibDescriptionModifications) {
            return input.createCopy();
        }

        if (input instanceof MdibStateModifications) {
            return input.createCopy();
        }

        return doDeepCopyIfConfigured(true, input);
    }

    /**
     * Copies output data if configured.
     * <p>
     * Data is copied if {@link CommonConfig#COPY_MDIB_OUTPUT} is configured true.
     * Data is forwarded otherwise.
     *
     * @param output data to be copied.
     * @param <T>    any type that is supposed to be deep-copied.
     * @return a copy of {@code output} if configured, otherwise a forwarded reference.
     */
    public <T extends Copyable<T>> T processOutput(T output) {
        return doDeepCopyIfConfigured(copyOutput, output);
    }

    /**
     * Copies output data if configured.
     * <p>
     * Data is copied if {@link CommonConfig#COPY_MDIB_OUTPUT} is configured true.
     * Data is forwarded otherwise.
     *
     * @param output data to be copied.
     * @param <T>    any type that is supposed to be deep-copied.
     * @return a copy of {@code output} if configured, otherwise a forwarded reference.
     */
    public <T extends Copyable<T>> List<T> processOutput(List<T> output) {
        return doDeepCopyIfConfigured(copyOutput, output);
    }

    private <T extends Copyable<T>> T doDeepCopyIfConfigured(boolean doCopy, T data) {
        if (doCopy) {
            return data.createCopy();
        } else {
            return data;
        }
    }

    private <T extends Copyable<T>> List<T> doDeepCopyIfConfigured(boolean doCopy, List<T> data) {
        if (doCopy) {
            return data.stream().map(Copyable::createCopy).collect(Collectors.toList());
        } else {
            return data;
        }
    }
}
