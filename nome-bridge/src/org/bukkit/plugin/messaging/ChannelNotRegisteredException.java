package org.bukkit.plugin.messaging;

public class ChannelNotRegisteredException extends RuntimeException {
    public ChannelNotRegisteredException() { super(); }
    public ChannelNotRegisteredException(String message) { super(message); }
    public ChannelNotRegisteredException(String channel, boolean incoming) {
        super("Attempted to send a plugin message through an unregistered channel: " + channel);
    }
}
