package org.bukkit.command;

import org.bukkit.Server;

public interface CommandSender {
    void sendMessage(String message);
    void sendMessage(String[] messages);
    String getName();
    Server getServer();
    boolean isOp();
    void setOp(boolean value);
    boolean hasPermission(String name);
}
