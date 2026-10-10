package org.bukkit.command;

import java.util.List;

public interface CommandMap {
    void registerAll(String fallbackPrefix, List<Command> commands);
    boolean register(String label, String fallbackPrefix, Command command);
    boolean register(String fallbackPrefix, Command command);
    boolean dispatch(CommandSender sender, String cmdLine);
    void clearCommands();
    Command getCommand(String name);
    List<String> tabComplete(CommandSender sender, String cmdLine);
}
