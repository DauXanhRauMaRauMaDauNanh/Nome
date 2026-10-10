package org.bukkit.event.entity;

import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class EntityDamageEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    protected final Entity entity;
    private double damage;
    private final DamageCause cause;

    public enum DamageCause {
        CONTACT, ENTITY_ATTACK, PROJECTILE, SUFFOCATION, FALL, FIRE, FIRE_TICK,
        MELTING, LAVA, DROWNING, BLOCK_EXPLOSION, ENTITY_EXPLOSION, VOID,
        LIGHTNING, SUICIDE, STARVATION, POISON, MAGIC, WITHER, FALLING_BLOCK,
        THORNS, DRAGON_BREATH, CUSTOM, FLY_INTO_WALL, HOT_FLOOR, CRAMMING,
        DRYOUT, FREEZE, SONIC_BOOM
    }

    public EntityDamageEvent(Entity damagee, DamageCause cause, double damage) {
        this.entity = damagee;
        this.cause = cause;
        this.damage = damage;
    }

    public Entity getEntity() {
        return entity;
    }

    public DamageCause getCause() {
        return cause;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
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
