package org.bukkit;

public enum Material {
    AIR(0),
    STONE(1),
    GRASS_BLOCK(2),
    DIRT(3),
    COBBLESTONE(4),
    OAK_PLANKS(5),
    OAK_SAPLING(6),
    BEDROCK(7),
    WATER(8),
    LAVA(10),
    SAND(12),
    GRAVEL(13),
    GOLD_ORE(14),
    IRON_ORE(15),
    COAL_ORE(16),
    OAK_LOG(17),
    OAK_LEAVES(18),
    GLASS(20),
    LAPIS_ORE(21),
    LAPIS_BLOCK(22),
    DISPENSER(23),
    SANDSTONE(24),
    NOTE_BLOCK(25),
    CHEST(54),
    DIAMOND_ORE(56),
    DIAMOND_BLOCK(57),
    CRAFTING_TABLE(58),
    FARMLAND(60),
    FURNACE(61),
    LADDER(65),
    REDSTONE_ORE(73),
    SNOW(78),
    ICE(79),
    SNOW_BLOCK(80),
    CLAY(82),
    NETHERRACK(87),
    SOUL_SAND(88),
    GLOWSTONE(89),
    STONE_BRICKS(98),
    OBSIDIAN(49),
    EMERALD_BLOCK(133),
    BEACON(138),
    ANVIL(145),
    HOPPER(154),
    BARRIER(166),
    SHULKER_BOX(219),
    NETHERITE_BLOCK(1000),
    ANCIENT_DEBRIS(1001),
    COPPER_BLOCK(1002);

    private final int id;

    Material(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public boolean isBlock() {
        return true;
    }

    public static Material getMaterial(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static Material getMaterial(int id) {
        for (Material m : values()) {
            if (m.id == id) return m;
        }
        return AIR;
    }
}
