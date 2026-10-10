package org.bukkit.plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default thread-safe implementation of ServicesManager.
 */
public class SimpleServicesManager implements ServicesManager {

    private final Map<Class<?>, List<RegisteredServiceProvider<?>>> providers = new ConcurrentHashMap<>();

    @Override
    public synchronized <T> void register(Class<T> service, T provider, Plugin plugin, ServicePriority priority) {
        if (service == null || provider == null || priority == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }
        RegisteredServiceProvider<T> registered = new RegisteredServiceProvider<>(service, provider, priority, plugin);
        List<RegisteredServiceProvider<?>> list = providers.computeIfAbsent(service, k -> new ArrayList<>());
        list.add(registered);
        list.sort(null); // Sorts based on compareTo (highest priority first)
    }

    @Override
    public synchronized void unregister(Class<?> service, Object provider) {
        if (service == null || provider == null) return;
        List<RegisteredServiceProvider<?>> list = providers.get(service);
        if (list != null) {
            list.removeIf(rsp -> rsp.getProvider().equals(provider));
            if (list.isEmpty()) {
                providers.remove(service);
            }
        }
    }

    @Override
    public synchronized void unregister(Object provider) {
        if (provider == null) return;
        Iterator<Map.Entry<Class<?>, List<RegisteredServiceProvider<?>>>> it = providers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Class<?>, List<RegisteredServiceProvider<?>>> entry = it.next();
            entry.getValue().removeIf(rsp -> rsp.getProvider().equals(provider));
            if (entry.getValue().isEmpty()) {
                it.remove();
            }
        }
    }

    @Override
    public synchronized void unregisterAll(Plugin plugin) {
        if (plugin == null) return;
        Iterator<Map.Entry<Class<?>, List<RegisteredServiceProvider<?>>>> it = providers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Class<?>, List<RegisteredServiceProvider<?>>> entry = it.next();
            entry.getValue().removeIf(rsp -> rsp.getPlugin().equals(plugin));
            if (entry.getValue().isEmpty()) {
                it.remove();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized <T> T load(Class<T> service) {
        RegisteredServiceProvider<T> registration = getRegistration(service);
        return registration != null ? registration.getProvider() : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized <T> RegisteredServiceProvider<T> getRegistration(Class<T> service) {
        if (service == null) return null;
        List<RegisteredServiceProvider<?>> list = providers.get(service);
        if (list != null && !list.isEmpty()) {
            return (RegisteredServiceProvider<T>) list.get(0);
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized <T> Collection<RegisteredServiceProvider<T>> getRegistrations(Class<T> service) {
        if (service == null) return Collections.emptyList();
        List<RegisteredServiceProvider<?>> list = providers.get(service);
        if (list == null || list.isEmpty()) return Collections.emptyList();
        List<RegisteredServiceProvider<T>> result = new ArrayList<>(list.size());
        for (RegisteredServiceProvider<?> rsp : list) {
            result.add((RegisteredServiceProvider<T>) rsp);
        }
        return Collections.unmodifiableCollection(result);
    }

    @Override
    public synchronized List<RegisteredServiceProvider<?>> getRegistrations(Plugin plugin) {
        if (plugin == null) return Collections.emptyList();
        List<RegisteredServiceProvider<?>> result = new ArrayList<>();
        for (List<RegisteredServiceProvider<?>> list : providers.values()) {
            for (RegisteredServiceProvider<?> rsp : list) {
                if (rsp.getPlugin().equals(plugin)) {
                    result.add(rsp);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public synchronized Collection<Class<?>> getKnownServices() {
        return Collections.unmodifiableSet(new HashSet<>(providers.keySet()));
    }

    @Override
    public synchronized <T> boolean isProvidedFor(Class<T> service) {
        List<RegisteredServiceProvider<?>> list = providers.get(service);
        return list != null && !list.isEmpty();
    }
}
