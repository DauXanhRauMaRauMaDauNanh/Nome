package org.bukkit.plugin.messaging;

import java.util.Set;
import org.bukkit.plugin.Plugin;

public interface PluginMessageRecipient {
    void sendPluginMessage(Plugin source, String channel, byte[] message);
    Set<String> getListeningPluginChannels();
}
