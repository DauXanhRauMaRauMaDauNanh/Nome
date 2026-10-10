package org.bukkit.entity;

public enum EntityType {
    PLAYER(1),
    ZOMBIE(2),
    SKELETON(3),
    CREEPER(4),
    SPIDER(5),
    COW(6),
    SHEEP(7),
    PIG(8),
    CHICKEN(9),
    VILLAGER(10),
    IRON_GOLEM(11),
    ENDERMAN(12),
    ENDER_DRAGON(13),
    WITHER(14),
    ITEM(15);

    private final int id;

    EntityType(int id) {
        this.id = id;
    }

    public int getTypeId() {
        return id;
    }
}
