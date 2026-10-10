package org.bukkit.plugin;

/**
 * A registry provider for Bukkit services.
 */
public class RegisteredServiceProvider<T> implements Comparable<RegisteredServiceProvider<?>> {
    private final Class<T> service;
    private final Plugin plugin;
    private final T provider;
    private final ServicePriority priority;

    public RegisteredServiceProvider(Class<T> service, T provider, ServicePriority priority, Plugin plugin) {
        this.service = service;
        this.plugin = plugin;
        this.provider = provider;
        this.priority = priority;
    }

    public Class<T> getService() {
        return service;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public T getProvider() {
        return provider;
    }

    public ServicePriority getPriority() {
        return priority;
    }

    @Override
    public int compareTo(RegisteredServiceProvider<?> other) {
        return other.priority.ordinal() - this.priority.ordinal();
    }
}
