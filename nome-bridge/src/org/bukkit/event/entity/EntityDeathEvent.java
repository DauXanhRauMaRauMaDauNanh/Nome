package org.bukkit.event.entity;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class EntityDeathEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Entity entity;
    private final List<ItemStack> drops = new ArrayList<>();
    private int droppedExp = 0;

    public EntityDeathEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    public List<ItemStack> getDrops() {
        return drops;
    }

    public int getDroppedExp() {
        return droppedExp;
    }

    public void setDroppedExp(int exp) {
        this.droppedExp = exp;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
