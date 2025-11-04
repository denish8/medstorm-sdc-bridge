package org.somda.sdc.glue.common.helper;

import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.glue.common.DefaultStateValues;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Processes a {@link DefaultStateValues} object and forwards any states to matching methods.
 */
public class DefaultStateValuesDispatcher {
    private Map<Class<?>, Method> methods;
    private final DefaultStateValues defaultStateValues;

    public DefaultStateValuesDispatcher(DefaultStateValues defaultStateValues) {
        this.methods = new HashMap<>(defaultStateValues.getClass().getDeclaredMethods().length);
        this.defaultStateValues = defaultStateValues;
        for (var method : defaultStateValues.getClass().getDeclaredMethods()) {
            if (method.getParameterCount() != 1) {
                continue;
            }

            var paramType = method.getParameterTypes()[0];
            if (AbstractState.class.isAssignableFrom(paramType)) {
                method.setAccessible(true);
                methods.put(paramType, method);
            }
        }
    }

    public void dispatchDefaultStateValues(AbstractState state)
            throws InvocationTargetException, IllegalAccessException {
        Class<?> stateClass = state.getClass();
        do {
            Method method = methods.get(stateClass);
            if (method != null) {
                method.invoke(defaultStateValues, state);
                return;
            }

            stateClass = stateClass.getSuperclass();
        } while (!stateClass.equals(Object.class));
    }
}
