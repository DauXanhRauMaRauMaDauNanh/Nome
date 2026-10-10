package org.bukkit.command;

import java.util.ArrayList;
import java.util.List;

public abstract class Command {
    private final String name;
    private String description = "";
    private String usageMessage = "";
    private String permission = "";
    private List<String> aliases = new ArrayList<>();

    protected Command(String name) {
        this.name = name;
    }

    public abstract boolean execute(CommandSender sender, String commandLabel, String[] args);

    public String getName() { return name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUsage() { return usageMessage; }
    public void setUsage(String usage) { this.usageMessage = usage; }
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
    public List<String> getAliases() { return aliases; }
    public void setAliases(List<String> aliases) { this.aliases = aliases; }
}
