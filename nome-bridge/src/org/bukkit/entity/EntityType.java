package org.bukkit.entity;

public enum EntityType {
    PLAYER(0),
    ITEM(1),
    XP_ORB(2),
    ARROW(3),
    TNT(4),
    FALLING_BLOCK(5),

    // Hostile Mobs
    ZOMBIE(10),
    SKELETON(11),
    CREEPER(12),
    SPIDER(13),
    ENDERMAN(14),

    // Passive & Neutral Mobs
    COW(20),
    PIG(21),
    SHEEP(22),
    CHICKEN(23),
    VILLAGER(24),
    IRON_GOLEM(25);

    private final int id;

    EntityType(int id) {
        this.id = id;
    }

    public int getTypeId() {
        return id;
    }
}
