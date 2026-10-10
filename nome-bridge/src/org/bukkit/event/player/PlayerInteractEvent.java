package org.bukkit.event.player;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

public class PlayerInteractEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private final ItemStack item;
    private final Action action;
    private final Block clickedBlock;

    public PlayerInteractEvent(Player who, Action action, ItemStack item, Block clickedBlock) {
        super(who);
        this.action = action;
        this.item = item;
        this.clickedBlock = clickedBlock;
    }

    public Action getAction() {
        return action;
    }

    public ItemStack getItem() {
        return item;
    }

    public Block getClickedBlock() {
        return clickedBlock;
    }

    public boolean hasItem() {
        return item != null;
    }

    public boolean hasBlock() {
        return clickedBlock != null;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
