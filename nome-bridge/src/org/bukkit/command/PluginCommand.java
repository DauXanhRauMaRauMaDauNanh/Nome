package org.bukkit.command;

import java.util.Collections;
import java.util.List;
import org.bukkit.plugin.Plugin;

public final class PluginCommand extends Command implements PluginIdentifiableCommand {
    private final Plugin owningPlugin;
    private CommandExecutor executor;
    private TabCompleter completer;

    public PluginCommand(String name, Plugin owner) {
        super(name);
        this.owningPlugin = owner;
        this.executor = owner instanceof CommandExecutor ? (CommandExecutor) owner : null;
        this.completer = owner instanceof TabCompleter ? (TabCompleter) owner : null;
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        boolean success = false;
        if (!owningPlugin.isEnabled()) return false;
        if (executor != null) {
            success = executor.onCommand(sender, this, commandLabel, args);
        }
        return success;
    }

    public void setExecutor(CommandExecutor executor) {
        this.executor = executor != null ? executor : (owningPlugin instanceof CommandExecutor ? (CommandExecutor) owningPlugin : null);
    }

    public CommandExecutor getExecutor() {
        return executor;
    }

    public void setTabCompleter(TabCompleter completer) {
        this.completer = completer;
    }

    public TabCompleter getTabCompleter() {
        return completer;
    }

    @Override
    public Plugin getPlugin() {
        return owningPlugin;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (completer != null) {
            return completer.onTabComplete(sender, this, alias, args);
        }
        return Collections.emptyList();
    }
}
