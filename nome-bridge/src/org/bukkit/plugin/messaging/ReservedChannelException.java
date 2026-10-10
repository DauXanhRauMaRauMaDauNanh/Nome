package org.bukkit.plugin.messaging;

public class ReservedChannelException extends RuntimeException {
    public ReservedChannelException() { super(); }
    public ReservedChannelException(String channel) {
        super("Attempted to register reserved channel name: " + channel);
    }
}
