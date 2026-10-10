package org.bukkit.plugin.messaging;

import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public interface Messenger {
    int MAX_MESSAGE_SIZE = 32766;
    int MAX_CHANNEL_SIZE = 64;

    boolean isReservedChannel(String channel);
    void registerOutgoingPluginChannel(Plugin plugin, String channel);
    void unregisterOutgoingPluginChannel(Plugin plugin, String channel);
    void unregisterOutgoingPluginChannel(Plugin plugin);
    PluginMessageListenerRegistration registerIncomingPluginChannel(Plugin plugin, String channel, PluginMessageListener listener);
    void unregisterIncomingPluginChannel(Plugin plugin, String channel, PluginMessageListener listener);
    void unregisterIncomingPluginChannel(Plugin plugin, String channel);
    void unregisterIncomingPluginChannel(Plugin plugin);
    Set<String> getOutgoingChannels();
    Set<String> getOutgoingChannels(Plugin plugin);
    Set<String> getIncomingChannels();
    Set<String> getIncomingChannels(Plugin plugin);
    Set<PluginMessageListenerRegistration> getIncomingChannelRegistrations(Plugin plugin);
    Set<PluginMessageListenerRegistration> getIncomingChannelRegistrations(String channel);
    Set<PluginMessageListenerRegistration> getIncomingChannelRegistrations(Plugin plugin, String channel);
    boolean isRegistrationValid(PluginMessageListenerRegistration registration);
    boolean isIncomingChannelRegistered(Plugin plugin, String channel);
    boolean isOutgoingChannelRegistered(Plugin plugin, String channel);
    void dispatchIncomingMessage(Player player, String channel, byte[] message);
}
