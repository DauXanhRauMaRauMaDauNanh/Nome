package org.bukkit.event.block;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class BlockPlaceEvent extends BlockEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private final Block placedAgainst;
    private final ItemStack itemInHand;
    private final Player player;
    private boolean canBuild = true;

    public BlockPlaceEvent(Block placedBlock, Block placedAgainst, ItemStack itemInHand, Player player, boolean canBuild) {
        super(placedBlock);
        this.placedAgainst = placedAgainst;
        this.itemInHand = itemInHand;
        this.player = player;
        this.canBuild = canBuild;
    }

    public Player getPlayer() {
        return player;
    }

    public Block getBlockPlaced() {
        return getBlock();
    }

    public Block getBlockAgainst() {
        return placedAgainst;
    }

    public ItemStack getItemInHand() {
        return itemInHand;
    }

    public boolean canBuild() {
        return canBuild;
    }

    public void setBuild(boolean canBuild) {
        this.canBuild = canBuild;
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
