package org.somda.sdc.dpws.soap.interception;

import com.google.inject.Inject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry to store and access a set of interceptor objects.
 */
public class InterceptorRegistry {
    private final Map<Direction, Map<String, List<InterceptorInfo>>> interceptorChains;

    @Inject
    InterceptorRegistry() {
        interceptorChains = new EnumMap<>(Direction.class);
        Arrays.stream(Direction.values()).forEach(direction ->
                // concurrent hash map as computeIfAbsent is a mutating function that could potentially be called by
                // getInterceptorInfoList in parallel
                interceptorChains.put(direction, new ConcurrentHashMap<>()));
    }

    /**
     * Takes an object and seeks interceptor methods.
     * <p>
     * Interceptor methods are stored in a registry for fast access.
     * They are retrievable by using
     *
     * <ul>
     * <li>{@link #getDefaultInterceptors()}
     * <li>{@link #getDefaultInterceptors(Direction)}
     * <li>{@link #getInterceptors(String)}
     * <li>{@link #getInterceptors(Direction, String)}
     * </ul>
     *
     * @param interceptor the object where to search for interceptor methods.
     */
    public void addInterceptor(Interceptor interceptor) {
        List<Method> actionCallbackMethods = getCallbackMethods(interceptor);
        for (Method method : actionCallbackMethods) {
            MessageInterceptor annotation = method.getAnnotation(MessageInterceptor.class);
            InterceptorInfo interceptorInfo = new InterceptorInfo(interceptor, method, annotation.sequenceNumber());
            Direction direction = annotation.direction();
            String action = annotation.value();

            if (direction != Direction.ANY) {
                List<InterceptorInfo> interceptorInfoList = getInterceptorInfoList(Direction.ANY, action);
                interceptorInfoList.add(interceptorInfo);
                Collections.sort(interceptorInfoList);
            }

            List<InterceptorInfo> interceptorInfoList = getInterceptorInfoList(direction, action);
            interceptorInfoList.add(interceptorInfo);
            Collections.sort(interceptorInfoList);
        }
    }

    /**
     * Gets all default interceptors.
     *
     * @return all interceptors of any direction and action.
     */
    public List<InterceptorInfo> getDefaultInterceptors() {
        return Collections.unmodifiableList(getInterceptors(""));
    }

    /**
     * Gets default interceptors of a specific direction.
     *
     * @param direction the direction to filter for.
     * @return all interceptors with given direction.
     */
    public List<InterceptorInfo> getDefaultInterceptors(Direction direction) {
        return Collections.unmodifiableList(getInterceptors(direction, ""));
    }

    /**
     * Gets default interceptor of a specific direction and action.
     *
     * @param direction the direction to filter for.
     * @param action    the action to filter for.
     * @return all interceptors with given direction and action.
     */
    public List<InterceptorInfo> getInterceptors(Direction direction, String action) {
        return Collections.unmodifiableList(getInterceptorInfoList(direction, action));
    }

    /**
     * Gets interceptors of a specific action.
     *
     * @param action the action to filter for.
     * @return all interceptors of any direction with given action.
     */
    public List<InterceptorInfo> getInterceptors(String action) {
        return Collections.unmodifiableList(getInterceptorInfoList(Direction.ANY, action));
    }

    private List<Method> getCallbackMethods(Object obj) {
        final Method[] declaredMethods = obj.getClass().getDeclaredMethods();
        return Arrays.stream(declaredMethods)
                .filter(m -> Arrays.stream(m.getAnnotations())
                        .anyMatch(a -> a.annotationType().equals(MessageInterceptor.class)))
                .filter(m -> m.getParameterCount() == 1 &&
                        InterceptorCallbackType.class.isAssignableFrom(m.getParameterTypes()[0]))
                .filter(m -> m.getReturnType().equals(Void.TYPE))
                .peek(method -> method.setAccessible(true))
                .collect(Collectors.toList());
    }

    private List<InterceptorInfo> getInterceptorInfoList(Direction direction, String action) {
        var actionMap = interceptorChains.get(direction);
        return actionMap.computeIfAbsent(action, k -> new ArrayList<>());
    }
}
