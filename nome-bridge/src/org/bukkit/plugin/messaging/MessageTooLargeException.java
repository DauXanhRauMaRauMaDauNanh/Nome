package org.bukkit.plugin.messaging;

public class MessageTooLargeException extends RuntimeException {
    public MessageTooLargeException() { super(); }
    public MessageTooLargeException(byte[] message) { super("Message too large: " + (message != null ? message.length : 0)); }
    public MessageTooLargeException(int length) { super("Message too large: " + length); }
    public MessageTooLargeException(String message) { super(message); }
}
