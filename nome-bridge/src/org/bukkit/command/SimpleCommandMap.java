package org.bukkit.command;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Server;

public class SimpleCommandMap implements CommandMap {
    protected final Map<String, Command> knownCommands = new ConcurrentHashMap<>();
    private final Server server;

    public SimpleCommandMap(Server server) {
        this.server = server;
    }

    @Override
    public void registerAll(String fallbackPrefix, List<Command> commands) {
        if (commands != null) {
            for (Command c : commands) register(fallbackPrefix, c);
        }
    }

    @Override
    public boolean register(String label, String fallbackPrefix, Command command) {
        if (label == null || command == null) return false;
        label = label.toLowerCase().trim();
        fallbackPrefix = fallbackPrefix != null ? fallbackPrefix.toLowerCase().trim() : "nome";
        knownCommands.put(fallbackPrefix + ":" + label, command);
        knownCommands.putIfAbsent(label, command);
        return true;
    }

    @Override
    public boolean register(String fallbackPrefix, Command command) {
        if (command == null) return false;
        return register(command.getName(), fallbackPrefix, command);
    }

    @Override
    public boolean dispatch(CommandSender sender, String cmdLine) {
        if (cmdLine == null || sender == null) return false;
        if (cmdLine.startsWith("/")) cmdLine = cmdLine.substring(1);
        String[] parts = cmdLine.split(" ");
        if (parts.length == 0 || parts[0].isEmpty()) return false;
        String sentCommandLabel = parts[0].toLowerCase();
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);
        Command target = getCommand(sentCommandLabel);
        if (target == null) return false;
        try {
            return target.execute(sender, sentCommandLabel, args);
        } catch (Throwable t) {
            sender.sendMessage("§cAn internal error occurred while attempting to perform this command");
            t.printStackTrace();
            return false;
        }
    }

    @Override
    public void clearCommands() {
        knownCommands.clear();
    }

    @Override
    public Command getCommand(String name) {
        if (name == null) return null;
        return knownCommands.get(name.toLowerCase());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String cmdLine) {
        return Collections.emptyList();
    }

    public Map<String, Command> getKnownCommands() {
        return knownCommands;
    }
}
