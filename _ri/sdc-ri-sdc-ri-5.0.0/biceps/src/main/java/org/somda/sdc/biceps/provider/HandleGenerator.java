package org.somda.sdc.biceps.provider;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility to create handles.
 */
public class HandleGenerator {
    private static final AtomicInteger HANDLE_COUNTER = new AtomicInteger(0);
    private final String prefix;

    private HandleGenerator(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Creates instance that generates handles with a fixed prefix.
     *
     * @param prefix the prefix to set for all generated handles.
     * @return a handle generator instance.
     */
    public static HandleGenerator create(String prefix) {
        return new HandleGenerator(prefix);
    }

    /**
     * Static function to create one handle with a given prefix.
     *
     * @param prefix the prefix to set for the call.
     * @return handle name in the format: prefix + non-negative integer.
     */
    public static String createOne(String prefix) {
        return concatHandle(prefix, HANDLE_COUNTER.incrementAndGet());
    }

    /**
     * Creates a handle that is globally unique.
     *
     * @return a UUID in hex format.
     */
    public static String createGloballyUnique() {
        return UUID.randomUUID().toString();
    }

    /**
     * Creates the next handle with the prefix given during construction.
     *
     * @return handle name in the format: prefix + non-negative integer.
     */
    public String next() {
        return concatHandle(prefix, HANDLE_COUNTER.incrementAndGet());
    }

    private static String concatHandle(String prefix, int suffix) {
        return String.format("%s%s", prefix, suffix);
    }
}
