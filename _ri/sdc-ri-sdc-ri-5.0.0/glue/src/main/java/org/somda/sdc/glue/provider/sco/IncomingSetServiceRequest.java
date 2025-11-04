package org.somda.sdc.glue.provider.sco;

import com.google.inject.BindingAnnotation;
import org.somda.sdc.biceps.model.message.InvocationError;
import org.somda.sdc.biceps.model.message.InvocationState;
import org.somda.sdc.biceps.model.participant.MdibVersion;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation to define a callback for incoming set service operation calls.
 * <p>
 * The receiving method needs to be parameterized with the following arguments:
 * <ol>
 * <li>{@link Context} instance
 * <li>requested data type
 * </ol>
 * <p>
 * Requested data types can be:
 * <ul>
 * <li>{@link String} for a SetString request
 * <li>{@link java.math.BigDecimal} for a SetValue request
 * <li>{@link java.util.List} of {@link Object} depending on the argument types for an Activate request
 * <li>An instance derived from {@link org.somda.sdc.biceps.model.participant.AbstractMetricState}
 * for a SetMetricState request
 * <li>An instance derived from {@link org.somda.sdc.biceps.model.participant.AbstractAlertState}
 * for a SetAlertState request
 * <li>An instance derived from {@link org.somda.sdc.biceps.model.participant.AbstractDeviceComponentState}
 * for a SetComponentState request
 * <li>An instance derived from {@link org.somda.sdc.biceps.model.participant.AbstractContextState}
 * for a SetContextState request
 * </ul>
 * <p>
 * The result of the operation is required to be of the type {@link InvocationResponse}, which can be created by using
 * {@link Context#createSuccessfulResponse(MdibVersion, InvocationState)} or
 * {@link Context#createUnsuccessfulResponse(MdibVersion, InvocationState, InvocationError, List)}.
 */
@Target({METHOD})
@Retention(RUNTIME)
@BindingAnnotation
public @interface IncomingSetServiceRequest {
    /**
     * Defines the operation handle that triggers the method.
     * <p>
     * The annotated method is called only if the handle of the operation matches this value. Leave the value empty to
     * match every handle.
     *
     * @return the handle to match for this  incoming operation call annotation.
     */
    String operationHandle() default "";

    /**
     * Defines the data type of a list container that is required due to type erasure.
     * <p>
     * If the operation payload comes as a list of proposed data items, this type defines the list type that is used
     * to distinguish between different functions that accept lists (required as Java lacks real templates).
     *
     * @return the generic type of the list element. Defaults to {@link NoList}, which indicates no list type.
     */
    Class<?> listType() default NoList.class;

    /**
     * Indicates a no-list type.
     */
    class NoList {
    }
}
