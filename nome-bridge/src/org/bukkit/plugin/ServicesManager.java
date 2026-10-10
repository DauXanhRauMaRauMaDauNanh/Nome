package org.bukkit.plugin;

import java.util.Collection;
import java.util.List;

/**
 * Manages services and service providers.
 */
public interface ServicesManager {

    <T> void register(Class<T> service, T provider, Plugin plugin, ServicePriority priority);

    void unregister(Class<?> service, Object provider);

    void unregister(Object provider);

    void unregisterAll(Plugin plugin);

    <T> T load(Class<T> service);

    <T> RegisteredServiceProvider<T> getRegistration(Class<T> service);

    <T> Collection<RegisteredServiceProvider<T>> getRegistrations(Class<T> service);

    List<RegisteredServiceProvider<?>> getRegistrations(Plugin plugin);

    Collection<Class<?>> getKnownServices();

    <T> boolean isProvidedFor(Class<T> service);
}
