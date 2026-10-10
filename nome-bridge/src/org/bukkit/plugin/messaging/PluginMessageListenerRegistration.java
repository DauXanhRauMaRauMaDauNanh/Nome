package org.bukkit.plugin.messaging;

import org.bukkit.plugin.Plugin;

public final class PluginMessageListenerRegistration {
    private final Messenger messenger;
    private final Plugin plugin;
    private final String channel;
    private final PluginMessageListener listener;

    public PluginMessageListenerRegistration(Messenger messenger, Plugin plugin, String channel, PluginMessageListener listener) {
        if (messenger == null || plugin == null || channel == null || listener == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }
        this.messenger = messenger;
        this.plugin = plugin;
        this.channel = channel;
        this.listener = listener;
    }

    public PluginMessageListener getListener() {
        return listener;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public String getChannel() {
        return channel;
    }

    public boolean isValid() {
        return messenger.isRegistrationValid(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PluginMessageListenerRegistration other)) return false;
        return this.plugin.equals(other.plugin) && this.channel.equalsIgnoreCase(other.channel) && this.listener.equals(other.listener);
    }

    @Override
    public int hashCode() {
        return plugin.hashCode() ^ channel.toLowerCase().hashCode() ^ listener.hashCode();
    }
}
