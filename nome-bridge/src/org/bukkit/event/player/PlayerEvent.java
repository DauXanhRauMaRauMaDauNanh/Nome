package org.bukkit.event.player;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public abstract class PlayerEvent extends Event {
    protected final Player player;

    public PlayerEvent(Player player) {
        this.player = player;
    }

    public PlayerEvent(Player player, boolean async) {
        super(async);
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}
