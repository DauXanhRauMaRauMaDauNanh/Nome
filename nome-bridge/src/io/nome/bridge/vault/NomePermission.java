package io.nome.bridge.vault;

import net.milkbowl.vault.permission.Permission;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Built-in ultra-fast Permission provider for Nome.
 */
public class NomePermission extends Permission {

    private final Map<String, Set<String>> playerPermissions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> playerGroups = new ConcurrentHashMap<>();

    @Override
    public String getName() {
        return "Nome-Native-Permissions";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean hasSuperPerms() {
        return true;
    }

    private String getKey(String player) {
        return player == null ? "" : player.toLowerCase();
    }

    @Override
    public boolean playerHas(String world, String player, String permission) {
        if (permission == null) return false;
        Set<String> perms = playerPermissions.get(getKey(player));
        if (perms != null) {
            if (perms.contains("*") || perms.contains(permission.toLowerCase())) {
                return true;
            }
            // Check wildcards like "minecraft.*"
            String[] parts = permission.toLowerCase().split("\\.");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length - 1; i++) {
                sb.append(parts[i]).append(".");
                if (perms.contains(sb.toString() + "*")) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean playerAdd(String world, String player, String permission) {
        if (permission == null) return false;
        playerPermissions.computeIfAbsent(getKey(player), k -> ConcurrentHashMap.newKeySet()).add(permission.toLowerCase());
        return true;
    }

    @Override
    public boolean playerRemove(String world, String player, String permission) {
        if (permission == null) return false;
        Set<String> perms = playerPermissions.get(getKey(player));
        if (perms != null) {
            return perms.remove(permission.toLowerCase());
        }
        return false;
    }

    @Override
    public boolean playerInGroup(String world, String player, String group) {
        if (group == null) return false;
        Set<String> groups = playerGroups.get(getKey(player));
        return groups != null && groups.contains(group.toLowerCase());
    }

    @Override
    public boolean playerAddGroup(String world, String player, String group) {
        if (group == null) return false;
        playerGroups.computeIfAbsent(getKey(player), k -> ConcurrentHashMap.newKeySet()).add(group.toLowerCase());
        return true;
    }

    @Override
    public boolean playerRemoveGroup(String world, String player, String group) {
        if (group == null) return false;
        Set<String> groups = playerGroups.get(getKey(player));
        return groups != null && groups.remove(group.toLowerCase());
    }

    @Override
    public String[] getPlayerGroups(String world, String player) {
        Set<String> groups = playerGroups.get(getKey(player));
        if (groups == null || groups.isEmpty()) {
            return new String[]{"default"};
        }
        return groups.toArray(new String[0]);
    }

    @Override
    public String getPrimaryGroup(String world, String player) {
        String[] groups = getPlayerGroups(world, player);
        return groups.length > 0 ? groups[0] : "default";
    }

    @Override
    public String[] getGroups() {
        Set<String> allGroups = new HashSet<>();
        allGroups.add("default");
        for (Set<String> grps : playerGroups.values()) {
            allGroups.addAll(grps);
        }
        return allGroups.toArray(new String[0]);
    }

    @Override
    public boolean hasGroupSupport() {
        return true;
    }
}
