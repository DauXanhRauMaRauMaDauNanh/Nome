package net.milkbowl.vault.permission;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public abstract class Permission {

    public abstract String getName();

    public abstract boolean isEnabled();

    public abstract boolean hasSuperPerms();

    public abstract boolean playerHas(String world, String player, String permission);

    public boolean playerHas(String world, OfflinePlayer player, String permission) {
        return playerHas(world, player.getName(), permission);
    }

    public boolean playerHas(Player player, String permission) {
        return playerHas(player.getWorld().getName(), player.getName(), permission);
    }

    public abstract boolean playerAdd(String world, String player, String permission);

    public boolean playerAdd(String world, OfflinePlayer player, String permission) {
        return playerAdd(world, player.getName(), permission);
    }

    public boolean playerAdd(Player player, String permission) {
        return playerAdd(player.getWorld().getName(), player.getName(), permission);
    }

    public abstract boolean playerRemove(String world, String player, String permission);

    public boolean playerRemove(String world, OfflinePlayer player, String permission) {
        return playerRemove(world, player.getName(), permission);
    }

    public boolean playerRemove(Player player, String permission) {
        return playerRemove(player.getWorld().getName(), player.getName(), permission);
    }

    public abstract boolean playerInGroup(String world, String player, String group);

    public abstract boolean playerAddGroup(String world, String player, String group);

    public abstract boolean playerRemoveGroup(String world, String player, String group);

    public abstract String[] getPlayerGroups(String world, String player);

    public abstract String getPrimaryGroup(String world, String player);

    public abstract String[] getGroups();

    public abstract boolean hasGroupSupport();
}
