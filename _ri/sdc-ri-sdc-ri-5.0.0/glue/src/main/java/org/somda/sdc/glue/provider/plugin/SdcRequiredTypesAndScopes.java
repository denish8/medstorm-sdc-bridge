package org.somda.sdc.glue.provider.plugin;

import com.google.common.base.Joiner;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.common.access.MdibAccessObserver;
import org.somda.sdc.biceps.common.event.ContextStateModificationMessage;
import org.somda.sdc.biceps.common.event.DescriptionModificationMessage;
import org.somda.sdc.biceps.model.participant.ContextAssociation;
import org.somda.sdc.biceps.model.participant.LocationContextState;
import org.somda.sdc.biceps.model.participant.MdsDescriptor;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.device.Device;
import org.somda.sdc.glue.GlueConstants;
import org.somda.sdc.glue.common.CommonConstants;
import org.somda.sdc.glue.common.uri.ComplexDeviceComponentMapper;
import org.somda.sdc.glue.common.uri.LocationDetailQueryMapper;
import org.somda.sdc.glue.common.uri.UriMapperGenerationArgumentException;
import org.somda.sdc.glue.provider.SdcDeviceContext;
import org.somda.sdc.glue.provider.SdcDevicePlugin;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maps all WS-Discovery Types and Scopes required by SDC and sends Hellos respectively.
 * <p>
 * In order to append custom scopes please consider using the {@link ScopesDecorator}.
 */
public class SdcRequiredTypesAndScopes implements SdcDevicePlugin, MdibAccessObserver, ScopesDecorator {
    private static final Logger LOG = LogManager.getLogger(SdcRequiredTypesAndScopes.class);
    private final Logger instanceLogger;

    private Device device;
    private MdibAccess mdibAccess;

    private Set<String> allScopes;
    private Set<String> locationContexts;
    private Set<String> mdsTypes;

    private boolean initializing;

    @Inject
    SdcRequiredTypesAndScopes(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.allScopes = new HashSet<>();
        this.locationContexts = new HashSet<>();
        this.mdsTypes = new HashSet<>();
        this.initializing = true;
    }

    @Override
    public void beforeStartUp(SdcDeviceContext context) {
        instanceLogger.info("Startup of automatic required types and scopes updating for device with EPR address {}",
                context.getDevice().getEprAddress());

        init(context, Collections.emptySet());
        context.getLocalMdibAccess().registerObserver(this);
    }

    @Override
    public void afterShutDown(SdcDeviceContext context) {
        instanceLogger.info("Automatic required types and scopes updating for device with EPR address {} stopped",
                context.getDevice().getEprAddress());
    }

    @Override
    public void init(SdcDeviceContext context, Set<String> scopes) {
        device = context.getDevice();
        device.getDiscoveryAccess().setTypes(Collections.singletonList(CommonConstants.MEDICAL_DEVICE_TYPE));
        device.getDiscoveryAccess().setScopes(Collections.singletonList(GlueConstants.SCOPE_SDC_PROVIDER));

        mdibAccess = context.getLocalMdibAccess();

        updateScopes();

        // The initializing flag prevents this call from sending a Hello during initialization
        // which would be bad as Hello is sent automatically on startup by the DPWS device implementation
        appendScopesAndSendHello(Collections.emptySet());
    }

    @Override
    public void appendScopesAndSendHello(Set<String> scopes) {
        updateScopes();

        // Setup new scopes based from scopes from outside plus recognized scopes from this class
        var newScopes = new HashSet<String>(locationContexts.size() + mdsTypes.size() + scopes.size());
        newScopes.add(GlueConstants.SCOPE_SDC_PROVIDER);
        newScopes.addAll(locationContexts);
        newScopes.addAll(mdsTypes);

        instanceLogger.debug("Append scopes [{}] to internal scope set [{}]",
                             () -> Joiner.on(",").join(scopes),
                             () -> Joiner.on(",").join(newScopes));

        newScopes.addAll(scopes);

        // If latest scopes and new scopes sizes are equal
        if (allScopes.size() == newScopes.size()) {
            // Remove latest scopes from new scopes
            var newScopesCopy = new HashSet<>(newScopes);
            newScopesCopy.removeAll(allScopes);

            // If there are no scopes left from the new scopes set, then there was no change at all
            // => return without sending Hello
            if (newScopesCopy.isEmpty()) {
                instanceLogger.debug("No scope changes detected");
                return;
            }
        }

        allScopes = newScopes;
        device.getDiscoveryAccess().setScopes(newScopes);

        if (initializing) {
            initializing = false;
        } else {
            device.getDiscoveryAccess().sendHello();
        }
    }

    @Subscribe
    private void onContextChange(ContextStateModificationMessage message) {
        instanceLogger.info("Context modification received");
        appendScopesAndSendHello(Collections.emptySet());
    }

    @Subscribe
    private void onDescriptionChange(DescriptionModificationMessage message) {
        instanceLogger.info("Description modification received");
        appendScopesAndSendHello(Collections.emptySet());
    }

    private void updateScopes() {
        var locationContextsBefore = locationContexts;
        locationContexts = extractAssociatedLocationContextStateIdentifiers(
                mdibAccess.findContextStatesByType(LocationContextState.class));

        instanceLogger.info("Location context scopes updated from [{}] to [{}]",
                            () -> Joiner.on(",").join(locationContextsBefore),
                            () -> Joiner.on(",").join(locationContexts));

        var mdsTypesBefore = mdsTypes;
        mdsTypes = extractMdsTypes(mdibAccess.findEntitiesByType(MdsDescriptor.class));
        instanceLogger.info("MDS type scopes updated from [{}] to [{}]",
                            () -> Joiner.on(",").join(mdsTypesBefore),
                            () -> Joiner.on(",").join(mdsTypes));
    }

    private Set<String> extractMdsTypes(Collection<MdibEntity> entities) {
        var mdsDescriptors = entities.stream()
                .filter(mdibEntity -> mdibEntity.getDescriptor(MdsDescriptor.class).isPresent())
                .map(mdibEntity -> mdibEntity.getDescriptor(MdsDescriptor.class).get())
                .collect(Collectors.toList());

        HashSet<String> uris = new HashSet<>(mdsDescriptors.size());
        for (MdsDescriptor mdsDescriptor : mdsDescriptors) {
            try {
                uris.add(ComplexDeviceComponentMapper.fromComplexDeviceComponent(mdsDescriptor));
            } catch (UriMapperGenerationArgumentException e) {
                instanceLogger.warn("The URI generation based on the given MdsDescriptor with the handle {} failed",
                        mdsDescriptor.getHandle(), e);
            }
        }

        return uris;
    }

    private Set<String> extractAssociatedLocationContextStateIdentifiers(List<LocationContextState> contextStates) {
        var locationContextState = contextStates.stream()
                .filter(contextState -> ContextAssociation.ASSOC.equals(contextState.getContextAssociation()))
                .findFirst();

        if (locationContextState.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> uris = new HashSet<>(locationContextState.get().getIdentification().size());
        for (var instanceIdentifier : locationContextState.get().getIdentification()) {
            try {
                uris.add(LocationDetailQueryMapper.createWithLocationDetailQuery(
                        instanceIdentifier, locationContextState.get().getLocationDetail()));
            } catch (UriMapperGenerationArgumentException e) {
                instanceLogger.warn("Unable to encode to an URI", e);
            }
        }
        return uris;
    }
}
