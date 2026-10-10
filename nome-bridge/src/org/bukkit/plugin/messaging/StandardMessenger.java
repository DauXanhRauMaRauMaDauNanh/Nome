package org.bukkit.plugin.messaging;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class StandardMessenger implements Messenger {
    private final Map<String, Set<PluginMessageListenerRegistration>> incomingByChannel = new ConcurrentHashMap<>();
    private final Map<Plugin, Set<PluginMessageListenerRegistration>> incomingByPlugin = new ConcurrentHashMap<>();
    private final Map<String, Set<Plugin>> outgoingByChannel = new ConcurrentHashMap<>();
    private final Map<Plugin, Set<String>> outgoingByPlugin = new ConcurrentHashMap<>();

    private void validatePlugin(Plugin plugin) {
        if (plugin == null) throw new IllegalArgumentException("Plugin cannot be null");
    }

    private void validateChannel(String channel) {
        if (channel == null) throw new IllegalArgumentException("Channel cannot be null");
        if (channel.length() > MAX_CHANNEL_SIZE) {
            throw new ChannelNameTooLongException(channel);
        }
    }

    @Override
    public boolean isReservedChannel(String channel) {
        validateChannel(channel);
        return channel.equalsIgnoreCase("REGISTER") || channel.equalsIgnoreCase("UNREGISTER");
    }

    @Override
    public void registerOutgoingPluginChannel(Plugin plugin, String channel) {
        validatePlugin(plugin);
        validateChannel(channel);
        if (isReservedChannel(channel)) {
            throw new ReservedChannelException(channel);
        }
        outgoingByChannel.computeIfAbsent(channel, k -> new CopyOnWriteArraySet<>()).add(plugin);
        outgoingByPlugin.computeIfAbsent(plugin, k -> new CopyOnWriteArraySet<>()).add(channel);
    }

    @Override
    public void unregisterOutgoingPluginChannel(Plugin plugin, String channel) {
        validatePlugin(plugin);
        validateChannel(channel);
        Set<Plugin> plugins = outgoingByChannel.get(channel);
        if (plugins != null) plugins.remove(plugin);
        Set<String> channels = outgoingByPlugin.get(plugin);
        if (channels != null) channels.remove(channel);
    }

    @Override
    public void unregisterOutgoingPluginChannel(Plugin plugin) {
        validatePlugin(plugin);
        Set<String> channels = outgoingByPlugin.remove(plugin);
        if (channels != null) {
            for (String ch : channels) {
                Set<Plugin> plugins = outgoingByChannel.get(ch);
                if (plugins != null) plugins.remove(plugin);
            }
        }
    }

    @Override
    public PluginMessageListenerRegistration registerIncomingPluginChannel(Plugin plugin, String channel, PluginMessageListener listener) {
        validatePlugin(plugin);
        validateChannel(channel);
        if (listener == null) throw new IllegalArgumentException("Listener cannot be null");
        if (isReservedChannel(channel)) {
            throw new ReservedChannelException(channel);
        }

        PluginMessageListenerRegistration reg = new PluginMessageListenerRegistration(this, plugin, channel, listener);
        incomingByChannel.computeIfAbsent(channel, k -> new CopyOnWriteArraySet<>()).add(reg);
        incomingByPlugin.computeIfAbsent(plugin, k -> new CopyOnWriteArraySet<>()).add(reg);
        return reg;
    }

    @Override
    public void unregisterIncomingPluginChannel(Plugin plugin, String channel, PluginMessageListener listener) {
        validatePlugin(plugin);
        validateChannel(channel);
        if (listener == null) throw new IllegalArgumentException("Listener cannot be null");

        Set<PluginMessageListenerRegistration> regs = incomingByChannel.get(channel);
        if (regs != null) {
            regs.removeIf(r -> r.getPlugin().equals(plugin) && r.getListener().equals(listener));
        }
        Set<PluginMessageListenerRegistration> pRegs = incomingByPlugin.get(plugin);
        if (pRegs != null) {
            pRegs.removeIf(r -> r.getChannel().equalsIgnoreCase(channel) && r.getListener().equals(listener));
        }
    }

    @Override
    public void unregisterIncomingPluginChannel(Plugin plugin, String channel) {
        validatePlugin(plugin);
        validateChannel(channel);
        Set<PluginMessageListenerRegistration> regs = incomingByChannel.get(channel);
        if (regs != null) {
            regs.removeIf(r -> r.getPlugin().equals(plugin));
        }
        Set<PluginMessageListenerRegistration> pRegs = incomingByPlugin.get(plugin);
        if (pRegs != null) {
            pRegs.removeIf(r -> r.getChannel().equalsIgnoreCase(channel));
        }
    }

    @Override
    public void unregisterIncomingPluginChannel(Plugin plugin) {
        validatePlugin(plugin);
        Set<PluginMessageListenerRegistration> regs = incomingByPlugin.remove(plugin);
        if (regs != null) {
            for (PluginMessageListenerRegistration r : regs) {
                Set<PluginMessageListenerRegistration> cRegs = incomingByChannel.get(r.getChannel());
                if (cRegs != null) cRegs.remove(r);
            }
        }
    }

    @Override
    public Set<String> getOutgoingChannels() {
        return Collections.unmodifiableSet(outgoingByChannel.keySet());
    }

    @Override
    public Set<String> getOutgoingChannels(Plugin plugin) {
        validatePlugin(plugin);
        Set<String> channels = outgoingByPlugin.get(plugin);
        return channels != null ? Collections.unmodifiableSet(channels) : Collections.emptySet();
    }

    @Override
    public Set<String> getIncomingChannels() {
        return Collections.unmodifiableSet(incomingByChannel.keySet());
    }

    @Override
    public Set<String> getIncomingChannels(Plugin plugin) {
        validatePlugin(plugin);
        Set<PluginMessageListenerRegistration> regs = incomingByPlugin.get(plugin);
        if (regs == null) return Collections.emptySet();
        Set<String> set = new HashSet<>();
        for (PluginMessageListenerRegistration r : regs) set.add(r.getChannel());
        return Collections.unmodifiableSet(set);
    }

    @Override
    public Set<PluginMessageListenerRegistration> getIncomingChannelRegistrations(Plugin plugin) {
        validatePlugin(plugin);
        Set<PluginMessageListenerRegistration> regs = incomingByPlugin.get(plugin);
        return regs != null ? Collections.unmodifiableSet(regs) : Collections.emptySet();
    }

    @Override
    public Set<PluginMessageListenerRegistration> getIncomingChannelRegistrations(String channel) {
        validateChannel(channel);
        Set<PluginMessageListenerRegistration> regs = incomingByChannel.get(channel);
        return regs != null ? Collections.unmodifiableSet(regs) : Collections.emptySet();
    }

    @Override
    public Set<PluginMessageListenerRegistration> getIncomingChannelRegistrations(Plugin plugin, String channel) {
        validatePlugin(plugin);
        validateChannel(channel);
        Set<PluginMessageListenerRegistration> regs = incomingByPlugin.get(plugin);
        if (regs == null) return Collections.emptySet();
        Set<PluginMessageListenerRegistration> result = new HashSet<>();
        for (PluginMessageListenerRegistration r : regs) {
            if (r.getChannel().equalsIgnoreCase(channel)) result.add(r);
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public boolean isRegistrationValid(PluginMessageListenerRegistration registration) {
        if (registration == null) return false;
        Set<PluginMessageListenerRegistration> regs = incomingByChannel.get(registration.getChannel());
        return regs != null && regs.contains(registration);
    }

    @Override
    public boolean isIncomingChannelRegistered(Plugin plugin, String channel) {
        validatePlugin(plugin);
        validateChannel(channel);
        Set<PluginMessageListenerRegistration> regs = incomingByPlugin.get(plugin);
        if (regs == null) return false;
        for (PluginMessageListenerRegistration r : regs) {
            if (r.getChannel().equalsIgnoreCase(channel)) return true;
        }
        return false;
    }

    @Override
    public boolean isOutgoingChannelRegistered(Plugin plugin, String channel) {
        validatePlugin(plugin);
        validateChannel(channel);
        Set<String> chs = outgoingByPlugin.get(plugin);
        return chs != null && chs.contains(channel);
    }

    @Override
    public void dispatchIncomingMessage(Player player, String channel, byte[] message) {
        validateChannel(channel);
        if (message.length > MAX_MESSAGE_SIZE) {
            throw new MessageTooLargeException(message);
        }
        Set<PluginMessageListenerRegistration> regs = incomingByChannel.get(channel);
        if (regs != null) {
            for (PluginMessageListenerRegistration reg : regs) {
                try {
                    reg.getListener().onPluginMessageReceived(channel, player, message);
                } catch (Throwable t) {
                    player.getServer().getLogger().severe("Error dispatching plugin message on " + channel + " to " + reg.getPlugin().getName());
                }
            }
        }
    }
}
