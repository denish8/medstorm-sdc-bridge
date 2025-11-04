package org.somda.sdc.glue.consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.AbstractComplexDeviceComponentDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.ContextAssociation;
import org.somda.sdc.dpws.client.DiscoveryFilter;
import org.somda.sdc.dpws.client.DiscoveryFilterBuilder;
import org.somda.sdc.glue.GlueConstants;
import org.somda.sdc.glue.common.CommonConstants;
import org.somda.sdc.glue.common.uri.ComplexDeviceComponentMapper;
import org.somda.sdc.glue.common.uri.ContextIdentificationMapper;
import org.somda.sdc.glue.common.uri.UriMapperGenerationArgumentException;

import javax.xml.namespace.QName;
import java.util.Optional;

/**
 * A variant of the {@linkplain DiscoveryFilterBuilder} that adds scopes and types required by SDC.
 * <p>
 * The following type is assigned: {@code {http://standards.ieee.org/downloads/11073/11073-20702-2016}MedicalDevice}
 * The following scope is assigned: {@code sdc.mds.pkp:1.2.840.10004.20701.1.1}
 *
 * @see GlueConstants#OID_KEY_PURPOSE_SDC_SERVICE_PROVIDER
 */
public class SdcDiscoveryFilterBuilder {
    private static final Logger LOG = LogManager.getLogger(SdcDiscoveryFilterBuilder.class);

    private final DiscoveryFilterBuilder discoveryFilterBuilder;

    /**
     * Constructs a new object with empty types and scopes.
     */
    private SdcDiscoveryFilterBuilder() {
        this.discoveryFilterBuilder = new DiscoveryFilterBuilder();
        this.discoveryFilterBuilder.addType(CommonConstants.MEDICAL_DEVICE_TYPE);
        this.discoveryFilterBuilder.addScope(GlueConstants.SCOPE_SDC_PROVIDER);
    }

    public static SdcDiscoveryFilterBuilder create() {
        return new SdcDiscoveryFilterBuilder();
    }

    /**
     * Adds a type.
     *
     * @param type the type as QName according to WS-Discovery.
     * @return this object.
     */
    public SdcDiscoveryFilterBuilder addType(QName type) {
        discoveryFilterBuilder.addType(type);
        return this;
    }

    /**
     * Adds a scope.
     *
     * @param scope the scope URI as string.
     * @return this object.
     */
    public SdcDiscoveryFilterBuilder addScope(String scope) {
        discoveryFilterBuilder.addScope(scope);
        return this;
    }

    /**
     * Adds a primary context state instance identifier as scope.
     *
     * @param state the location context state.
     * @param <T>   a context state type.
     * @return this object.
     */
    public <T extends AbstractContextState> SdcDiscoveryFilterBuilder addContext(T state) {
        try {
            createScopeFromContext(state).ifPresent(this::addScope);
        } catch (UriMapperGenerationArgumentException e) {
            LOG.warn("Context state could not be encoded as an URI", e);
        }
        return this;
    }

    /**
     * Adds a device component type.
     *
     * @param component the location context state.
     * @param <T>       a complex device component descriptor type.
     * @return this object.
     */
    public <T extends AbstractComplexDeviceComponentDescriptor> SdcDiscoveryFilterBuilder addDeviceComponent(
            T component) {

        try {
            addScope(ComplexDeviceComponentMapper.fromComplexDeviceComponent(component));
        } catch (UriMapperGenerationArgumentException e) {
            LOG.warn("The URI generation based on the given component failed", e);
        }
        return this;
    }

    /**
     * Gets a discovery filter with all types and scopes added via {@link #addType(QName)} and
     * {@link #addScope(String)}.
     *
     * @return a {@linkplain DiscoveryFilter} instance.
     */
    public DiscoveryFilter get() {
        return discoveryFilterBuilder.get();
    }

    // Creates a scope for a context based on the grammar in IEEE 11073-20701 section 9.4
    // optional means: not associated or no identification found
    private static Optional<String> createScopeFromContext(AbstractContextState contextState)
            throws UriMapperGenerationArgumentException {
        if (!contextState.getContextAssociation().equals(ContextAssociation.ASSOC)) {
            return Optional.empty();
        }

        if (contextState.getIdentification().isEmpty()) {
            return Optional.empty();
        }

        ContextIdentificationMapper.ContextSource contextSource = mapToContextSource(contextState);
        return Optional.of(ContextIdentificationMapper.fromInstanceIdentifier(contextState.getIdentification().get(0),
                                                                              contextSource));
    }

    private static ContextIdentificationMapper.ContextSource mapToContextSource(AbstractContextState contextState) {
        for (ContextIdentificationMapper.ContextSource value : ContextIdentificationMapper.ContextSource.values()) {
            if (value.getSourceClass().isAssignableFrom(contextState.getClass())) {
                return value;
            }
        }
        throw new RuntimeException(String.format("Reached unknown context: %s", contextState.getClass().toString()));
    }

}
