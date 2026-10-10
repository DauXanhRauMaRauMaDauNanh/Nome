package org.bukkit.plugin.messaging;

public class ChannelNameTooLongException extends RuntimeException {
    public ChannelNameTooLongException() { super(); }
    public ChannelNameTooLongException(String channel) { super("Channel name too long: " + channel); }
}
