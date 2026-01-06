package gg.miracle.api.events;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {
    private final Map<Class<?>, List<EventSubscriber>> subscribers = new ConcurrentHashMap<>();

    public void register(Object listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Subscribe.class)) {
                if (method.getParameterCount() != 1) {
                    throw new IllegalArgumentException(
                            "Event handler method must have exactly one parameter: " + method
                    );
                }

                Class<?> eventType = method.getParameterTypes()[0];
                Subscribe annotation = method.getAnnotation(Subscribe.class);

                method.setAccessible(true);

                subscribers
                        .computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                        .add(new EventSubscriber(listener, method, annotation.priority()));
            }
        }

        // Sort subscribers by priority
        subscribers.values().forEach(list ->
                list.sort(Comparator.comparingInt(EventSubscriber::priority).reversed())
        );
    }

    public void unregister(Object listener) {
        subscribers.values().forEach(list ->
                list.removeIf(sub -> sub.instance() == listener)
        );
    }

    public <T extends Event> T post(T event) {
        List<EventSubscriber> subs = subscribers.get(event.getClass());
        if (subs != null) {
            for (EventSubscriber sub : subs) {
                if (event instanceof Cancellable cancellable && cancellable.isCancelled()) {
                    break;
                }

                try {
                    sub.method().invoke(sub.instance(), event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return event;
    }

    private record EventSubscriber(Object instance, Method method, int priority) {}
}
