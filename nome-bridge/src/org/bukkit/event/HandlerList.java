package org.bukkit.event;

public class HandlerList {
    private static final HandlerList DEFAULT = new HandlerList();

    public static HandlerList getHandlerList() {
        return DEFAULT;
    }
}
