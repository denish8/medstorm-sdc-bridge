package org.somda.sdc.biceps.common.access;

/**
 * Definition of an auto-closeable interface that allows to invoke read functions without
 * getting distracted by write calls.
 */
public interface ReadTransaction extends MdibAccess, AutoCloseable {
    /**
     * Creates an empty default to override {@linkplain AutoCloseable#close()}.
     * <p>
     * {@link AutoCloseable#close()} throws an exception whereas {@linkplain ReadTransaction} is
     * supposed to be no-throw.
     * In order to avoid a boiler plate catch branch that is never visited when using {@code try(...)} with
     * {@linkplain ReadTransaction}, this function overrides the {@link AutoCloseable#close()} function without
     * throwing a checked exception.
     *
     * @see AutoCloseable#close()
     */
    @Override
    default void close() {
    }
}
