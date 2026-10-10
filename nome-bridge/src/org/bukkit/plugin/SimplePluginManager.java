package org.bukkit.plugin;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.bukkit.Server;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class SimplePluginManager implements PluginManager {

    private static class RegisteredHandler {
        final Listener listener;
        final Plugin plugin;
        final Method method;
        final EventPriority priority;
        final boolean ignoreCancelled;

        RegisteredHandler(Listener listener, Plugin plugin, Method method, EventPriority priority, boolean ignoreCancelled) {
            this.listener = listener;
            this.plugin = plugin;
            this.method = method;
            this.priority = priority;
            this.ignoreCancelled = ignoreCancelled;
            method.setAccessible(true);
        }
    }

    private final Server server;
    private final File pluginFolder;
    private final Map<String, Plugin> plugins = new ConcurrentHashMap<>();
    private final Map<Class<? extends Event>, List<RegisteredHandler>> handlers = new ConcurrentHashMap<>();

    public SimplePluginManager(Server server, File pluginFolder) {
        this.server = server;
        this.pluginFolder = pluginFolder;
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs();
        }
    }

    @Override
    public void registerEvents(Listener listener, Plugin plugin) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            EventHandler annotation = method.getAnnotation(EventHandler.class);
            if (annotation != null && method.getParameterCount() == 1) {
                Class<?> param = method.getParameterTypes()[0];
                if (Event.class.isAssignableFrom(param)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends Event> eventClass = (Class<? extends Event>) param;
                    handlers.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>())
                            .add(new RegisteredHandler(listener, plugin, method, annotation.priority(), annotation.ignoreCancelled()));

                    handlers.get(eventClass).sort(Comparator.comparingInt(h -> h.priority.getSlot()));
                }
            }
        }
    }

    @Override
    public void callEvent(Event event) {
        List<RegisteredHandler> list = handlers.get(event.getClass());
        if (list == null || list.isEmpty()) return;

        boolean isCancelled = (event instanceof Cancellable) && ((Cancellable) event).isCancelled();

        for (RegisteredHandler handler : list) {
            if (isCancelled && handler.ignoreCancelled) continue;
            try {
                handler.method.invoke(handler.listener, event);
                if (event instanceof Cancellable) {
                    isCancelled = ((Cancellable) event).isCancelled();
                }
            } catch (Throwable t) {
                System.err.println("[Nome-PluginManager] Error dispatching event " + event.getEventName() + " to " + handler.plugin.getName());
                t.printStackTrace();
            }
        }
    }

    public List<Plugin> loadPlugins() {
        File[] jarFiles = pluginFolder.listFiles((dir, name) -> name.endsWith(".jar"));
        List<Plugin> loaded = new ArrayList<>();
        if (jarFiles == null) return loaded;

        for (File jar : jarFiles) {
            Plugin p = loadPlugin(jar);
            if (p != null) {
                loaded.add(p);
            }
        }
        return loaded;
    }

    @Override
    public Plugin loadPlugin(File file) {
        try (JarFile jar = new JarFile(file)) {
            JarEntry entry = jar.getJarEntry("plugin.yml");
            if (entry == null) entry = jar.getJarEntry("paper-plugin.yml");
            if (entry == null) {
                System.err.println("[Nome-PluginManager] Missing plugin.yml in " + file.getName());
                return null;
            }

            Properties props = new Properties();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(jar.getInputStream(entry)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int colon = line.indexOf(':');
                    if (colon > 0) {
                        props.setProperty(line.substring(0, colon).trim(), line.substring(colon + 1).trim());
                    }
                }
            }

            String name = props.getProperty("name", file.getName().replace(".jar", ""));
            String version = props.getProperty("version", "1.0.0");
            String mainClass = props.getProperty("main");
            String author = props.getProperty("author", "Unknown");
            String desc = props.getProperty("description", "");

            if (mainClass == null) return null;

            URLClassLoader cl = new URLClassLoader(new URL[]{file.toURI().toURL()}, getClass().getClassLoader());
            Class<?> clazz = Class.forName(mainClass, true, cl);
            if (!Plugin.class.isAssignableFrom(clazz)) return null;

            Plugin plugin = (Plugin) clazz.getDeclaredConstructor().newInstance();
            PluginDescriptionFile pdf = new PluginDescriptionFile(name, version, mainClass, desc, author);
            File dataFolder = new File(pluginFolder, name);

            if (plugin instanceof JavaPlugin) {
                ((JavaPlugin) plugin).init(pdf, dataFolder, server);
            }
            plugins.put(name, plugin);
            plugin.onLoad();
            return plugin;
        } catch (Throwable t) {
            System.err.println("[Nome-PluginManager] Failed to load " + file.getName() + ": " + t.getMessage());
            return null;
        }
    }

    @Override public Plugin getPlugin(String name) { return plugins.get(name); }
    @Override public Plugin[] getPlugins() { return plugins.values().toArray(new Plugin[0]); }
    @Override public boolean isPluginEnabled(String name) { Plugin p = plugins.get(name); return p != null && p.isEnabled(); }
    @Override public boolean isPluginEnabled(Plugin plugin) { return plugin != null && plugin.isEnabled(); }

    @Override
    public void enablePlugin(Plugin plugin) {
        if (!plugin.isEnabled()) {
            if (plugin instanceof JavaPlugin) {
                ((JavaPlugin) plugin).setEnabled(true);
            } else {
                plugin.onEnable();
            }
        }
    }

    @Override
    public void disablePlugin(Plugin plugin) {
        if (plugin.isEnabled()) {
            if (plugin instanceof JavaPlugin) {
                ((JavaPlugin) plugin).setEnabled(false);
            } else {
                plugin.onDisable();
            }
        }
    }

    @Override
    public void clearPlugins() {
        for (Plugin p : plugins.values()) disablePlugin(p);
        plugins.clear();
        handlers.clear();
    }
}
