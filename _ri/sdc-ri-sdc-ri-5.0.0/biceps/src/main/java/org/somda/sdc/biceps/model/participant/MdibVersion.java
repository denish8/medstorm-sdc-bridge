package org.somda.sdc.biceps.model.participant;

import org.somda.sdc.common.model.ObjectStringifier;
import org.somda.sdc.common.util.UriUtil;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Container for MDIB version attributes.
 * <p>
 * This class models the MDIB version attributes that lack a separate container in the XML Schema.
 * It models sequence id, instance id and version number, enclosed in one class.
 * {@linkplain MdibVersion} is an immutable class and provides means to
 * <ul>
 * <li>create versions with random UUID by {@link #create()}
 * <li>increment versions by {@link #increment(MdibVersion)}
 * <li>set the version counter of an {@linkplain MdibVersion} by {@link #setVersionCounter(MdibVersion, BigInteger)}
 * <li>compare versions (see {@link #equals(Object)})
 * </ul>
 */
public class MdibVersion {
    private final String sequenceId;
    private final BigInteger instanceId;
    private final BigInteger version;

    /**
     * Constructor that sets a given sequence id.
     * <p>
     * Instance id and version counter are initialized with 0.
     *
     * @param sequenceId the sequence id to set.
     */
    public MdibVersion(String sequenceId) {
        this.sequenceId = sequenceId;
        this.version = BigInteger.ZERO;
        this.instanceId = BigInteger.ZERO;
    }

    /**
     * Constructor that sets a given sequence id and version counter.
     * <p>
     * Instance id is initialized with 0.
     *
     * @param sequenceId the sequence id to set.
     * @param version the version counter to set.
     */
    public MdibVersion(String sequenceId, BigInteger version) {
        this.sequenceId = sequenceId;
        this.version = version;
        this.instanceId = BigInteger.ZERO;
    }

    /**
     * Constructor that sets all version attributes.
     *
     * @param sequenceId the sequence id to set.
     * @param version the version counter to set.
     * @param instanceId the instance id to set.
     */
    public MdibVersion(String sequenceId, BigInteger version, BigInteger instanceId) {
        this.sequenceId = sequenceId;
        this.version = version;
        this.instanceId = instanceId;
    }

    /**
     * Creates a new instance with a random sequence id.
     *
     * @return a new instance.
     */
    public static MdibVersion create() {
        return new MdibVersion(UriUtil.createUuid(UUID.randomUUID()));
    }

    /**
     * Accepts an existing instance and increments the version counter.
     *
     * @param mdibVersion the version base.
     * @return a new instance with same sequence and instance id as in {@code mdibVersion} plus a version counter
     * incremented by one.
     */
    public static MdibVersion increment(MdibVersion mdibVersion) {
        return new MdibVersion(mdibVersion.getSequenceId(), mdibVersion.getVersion().add(BigInteger.ONE),
                mdibVersion.getInstanceId());
    }

    /**
     * Accepts an existing instance and resets the version counter to the given number.
     * @param mdibVersion the version base.
     * @param versionCounter the version counter number to replace.
     * @return a new instance with same sequence and instance id as in {@code mdibVersion} plus a version counter
     * incremented by {@code versionCounter}.
     */
    public static MdibVersion setVersionCounter(MdibVersion mdibVersion, BigInteger versionCounter) {
        return new MdibVersion(mdibVersion.getSequenceId(), versionCounter, mdibVersion.getInstanceId());
    }

    public String getSequenceId() {
        return sequenceId;
    }

    public BigInteger getVersion() {
        return version;
    }

    public BigInteger getInstanceId() {
        return instanceId;
    }

    /**
     * Compares two {@linkplain MdibVersion} objects on equality.
     *
     * @param rhsObject the right hand side to compare against with this object.
     * @return true if all version attributes equal true, otherwise false.
     */
    @Override
    public boolean equals(Object rhsObject) {
        if (rhsObject == this) {
            return true;
        }
        if (!(rhsObject instanceof MdibVersion)) {
            return false;
        }

        MdibVersion rhs = (MdibVersion) rhsObject;
        return URI.create(this.sequenceId).equals(URI.create(rhs.sequenceId))
                && this.version.equals(rhs.version)
                && this.instanceId.equals(rhs.instanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sequenceId, instanceId, version);
    }

    /**
     * Compares two MDIB versions.
     * <p>
     * As the MDIB version is a triple consisting of at least one non-trivially comparable data type, it cannot be
     * compared the classical way.
     * Hence there is a set of rules that define what to expect depending on the data of that triple.
     * For that reason this compare function introduces a fourth result state, which is {@linkplain Optional#empty()},
     * in case that the non-trivially comparable sequence ids are not equal.
     *
     * @param rhs right hand side to compare.
     * @return depending on the fields of the MDIB version triple:
     * <ul>
     * <li>{@linkplain Optional#empty()} if sequence ids differ from each other
     * <li>-1 if {@code lhs.instanceId < rhs.instanceId
     *      || lhs.instanceId == rhs.instanceId && lhs.version < rhs.version}
     * <li>the result of {@code lhs.version.compareTo(rhs.version)} if {@code lhs.instanceId == rhs.instanceId}
     * <li>1 if {@code lhs.instanceId > rhs.instanceId || lhs.instanceId == rhs.instanceId && lhs.version > rhs.version}
     * </ul>
     */
    public Optional<Integer> compareToMdibVersion(@Nullable MdibVersion rhs) {
        if (rhs == null || !URI.create(this.getSequenceId()).equals(URI.create(rhs.getSequenceId()))) {
            return Optional.empty();
        }

        if (this.getInstanceId().compareTo(rhs.getInstanceId()) < 0) {
            return Optional.of(-1);
        }

        if (this.getInstanceId().equals(rhs.getInstanceId())) {
            return Optional.of(this.getVersion().compareTo(rhs.getVersion()));
        }

        if (this.getInstanceId().compareTo(rhs.getInstanceId()) > 0) {
            return Optional.of(1);
        }

        return Optional.empty();
    }

    @Override
    public String toString() {
        return ObjectStringifier.stringifyAll(this);
    }
}
