package org.bukkit.plugin;

import java.io.File;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;

public interface PluginManager {
    void registerEvents(Listener listener, Plugin plugin);
    void callEvent(Event event);
    Plugin getPlugin(String name);
    Plugin[] getPlugins();
    boolean isPluginEnabled(String name);
    boolean isPluginEnabled(Plugin plugin);
    void enablePlugin(Plugin plugin);
    void disablePlugin(Plugin plugin);
    void clearPlugins();
    Plugin loadPlugin(File file);
}
