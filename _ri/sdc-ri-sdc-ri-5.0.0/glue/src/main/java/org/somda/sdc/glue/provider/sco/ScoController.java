package org.somda.sdc.glue.provider.sco;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.message.InvocationError;
import org.somda.sdc.biceps.model.message.InvocationState;
import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.biceps.model.participant.ObjectFactory;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.device.EventSourceAccess;
import org.somda.sdc.glue.provider.sco.factory.ContextFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Manages callbacks for incoming set service requests.
 */
public class ScoController {
    private static final Logger LOG = LogManager.getLogger(ScoController.class);

    private final Map<String, ReflectionInfo> invocationReceivers;
    private final List<ReflectionInfo> defaultInvocationReceivers;
    private final EventSourceAccess eventSourceAccess;
    private final LocalMdibAccess mdibAccess;
    private final ContextFactory contextFactory;
    private final ObjectFactory participantModelFactory;
    private final Logger instanceLogger;

    private long transactionCounter;

    @AssistedInject
    ScoController(@Assisted EventSourceAccess eventSourceAccess,
                  @Assisted LocalMdibAccess mdibAccess,
                  ContextFactory contextFactory,
                  ObjectFactory participantModelFactory,
                  @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.eventSourceAccess = eventSourceAccess;
        this.mdibAccess = mdibAccess;
        this.contextFactory = contextFactory;
        this.participantModelFactory = participantModelFactory;
        this.invocationReceivers = new HashMap<>();
        this.defaultInvocationReceivers = new ArrayList<>();

        this.transactionCounter = 0;
    }

    /**
     * Invokes processing of an incoming network set service call.
     *
     * @param handle  the handle of the operation that was called.
     * @param source  the instance identifier that represents the calling client.
     * @param payload the request data.
     * @param <T>     type of the request data.
     * @param callerId string that uniquely identifies the certificate of the calling consumer (e.g. Distinguished Name)
     * @throws RuntimeException if the handle of the operation is not known and no source mds can be determined
     * @return the initial invocation info required for the response message. The initial invocation info is
     * requested by the callback that is going to be invoked. In case that no callback can be found, a fail state is
     * returned.
     */
    public <T> InvocationResponse processIncomingSetOperation(String handle, InstanceIdentifier source, T payload, @Nullable String callerId) {
        // can throw RuntimeException if handle is unknown
        final Context context = contextFactory.createContext(transactionCounter++, handle, source, eventSourceAccess, mdibAccess, callerId);
        try {
            // in order to also seek a specific handle-based invocation receiver
            // prepend default receivers with handle-based receiver
            final var thisCallReceivers = new ArrayList<ReflectionInfo>(defaultInvocationReceivers.size() + 1);
            thisCallReceivers.add(invocationReceivers.get(handle));
            thisCallReceivers.addAll(defaultInvocationReceivers);

            // iterate receivers until a suitable one is found
            for (var receiver : thisCallReceivers) {
                if (receiver == null) {
                    continue;
                }
                if (receiver.getCallbackMethod().getParameters()[1].getType().isAssignableFrom(payload.getClass())) {
                    return (InvocationResponse) receiver.getCallbackMethod()
                            .invoke(receiver.getReceiver(), context, payload);
                }
            }
        } catch (Exception e) {
            final var errorMessage =
                    String.format("Invocation of operation with handle '%s' failed: %s", handle, e.getMessage());
            instanceLogger.warn(errorMessage);
            instanceLogger.trace(errorMessage, e);

            return additionallySendResponseAsReport(context, context.createUnsuccessfulResponse(
                    mdibAccess.getMdibVersion(),
                    InvocationState.FAIL,
                    InvocationError.UNSPEC,
                    Collections.singletonList(createLocalizedText(errorMessage))));
        }

        // if no suitable receiver was found, send error report
        return additionallySendResponseAsReport(context, context.createUnsuccessfulResponse(
                mdibAccess.getMdibVersion(),
                InvocationState.FAIL,
                InvocationError.UNSPEC,
                Collections.singletonList(createLocalizedText(
                        String.format("A handler for the operation with handle '%s' could not be found",
                                handle)))));
    }

    private LocalizedText createLocalizedText(String text) {
        var localizedText = participantModelFactory.createLocalizedText();
        localizedText.setLang("en");
        localizedText.setValue(text);
        return localizedText;
    }

    private InvocationResponse additionallySendResponseAsReport(Context context, InvocationResponse response) {
        context.sendReport(
                response.getMdibVersion(),
                response.getInvocationState(),
                response.getInvocationError(),
                response.getInvocationErrorMessage(),
                null
        );
        return response;
    }

    /**
     * Registers an object that possesses callback functions for incoming set service requests.
     * <p>
     * This class is designed to handle exactly one receiver per handle or data type.
     * <em>Adding multiple receivers for one handle or data type causes undefined behavior!</em>
     *
     * @param receiver the object that includes methods annotated with {@link IncomingSetServiceRequest}.
     */
    public void addOperationInvocationReceiver(OperationInvocationReceiver receiver) {
        boolean annotationFound = false;
        for (Method method : receiver.getClass().getDeclaredMethods()) {
            final IncomingSetServiceRequest annotation = method.getDeclaredAnnotation(IncomingSetServiceRequest.class);
            if (annotation == null) {
                continue;
            }

            if (method.getReturnType() == null || !method.getReturnType().equals(InvocationResponse.class)) {
                continue;
            }

            if (method.getParameterCount() != 2) {
                continue;
            }

            final Parameter[] parameters = method.getParameters();
            if (!parameters[0].getType().equals(Context.class)) {
                continue;
            }

            String key = annotation.operationHandle();
            if (!key.isEmpty() && invocationReceivers.containsKey(key)) {
                instanceLogger.warn("Ignore callback registration for key {} as there is a receiver already", key);
                continue;
            }

            method.setAccessible(true);
            final ReflectionInfo reflectionInfo = new ReflectionInfo(receiver, method, annotation);
            if (key.isEmpty()) {
                defaultInvocationReceivers.add(reflectionInfo);
            } else {
                invocationReceivers.put(key, reflectionInfo);
            }

            annotationFound = true;
        }

        if (!annotationFound) {
            instanceLogger.warn("No callback function found in object {} of type {}",
                    receiver, receiver.getClass().getName());
        }
    }

    private static class ReflectionInfo {
        private final OperationInvocationReceiver receiver;
        private final Method callbackMethod;
        private final IncomingSetServiceRequest annotation;

        public ReflectionInfo(OperationInvocationReceiver receiver, Method callbackMethod,
                              IncomingSetServiceRequest annotation) {
            this.receiver = receiver;
            this.callbackMethod = callbackMethod;
            this.annotation = annotation;
        }

        public OperationInvocationReceiver getReceiver() {
            return receiver;
        }

        public Method getCallbackMethod() {
            return callbackMethod;
        }

        public IncomingSetServiceRequest getAnnotation() {
            return annotation;
        }
    }
}
