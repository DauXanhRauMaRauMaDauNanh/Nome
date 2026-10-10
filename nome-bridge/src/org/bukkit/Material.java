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
    DEEPSLATE(100),
    COPPER_ORE(110),
    EMERALD_ORE(129),
    EMERALD_BLOCK(133),
    BEACON(138),
    ANVIL(145),
    HOPPER(154),
    BARRIER(166),
    HAY_BLOCK(170),
    CAMPFIRE(210),
    STRAW_BED(211),
    CUSHION(212),
    RED_CUSHION(213),
    WOOL_SLAB(214),
    RED_WOOL_SLAB(215),
    WOOL_STAIRS(216),
    RED_WOOL_STAIRS(217),
    CONCRETE_SLAB(218),
    CONCRETE_STAIRS(219),
    POPLAR_LOG(220),
    POPLAR_LEAVES(221),
    RED_POPLAR_LEAVES(222),
    ORANGE_POPLAR_LEAVES(223),
    YELLOW_POPLAR_LEAVES(224),
    SHELF_MUSHROOM(225),
    RED_SHRUB(226),
    WHITE_WOOL(35),
    RED_WOOL(36),
    BROWN_MUSHROOM(39),
    RED_MUSHROOM(40),
    COMPASS(345),
    MAP(395),
    SHULKER_BOX(219),
    NETHERITE_BLOCK(1000),
    ANCIENT_DEBRIS(1001),
    COPPER_BLOCK(1002),
    DEEPSLATE_DIAMOND_ORE(200),
    DEEPSLATE_GOLD_ORE(201),
    DEEPSLATE_IRON_ORE(202),
    DEEPSLATE_COPPER_ORE(203),
    DEEPSLATE_COAL_ORE(204),
    DEEPSLATE_LAPIS_ORE(205),
    DEEPSLATE_REDSTONE_ORE(206),
    DEEPSLATE_EMERALD_ORE(208),

    // Items, Weapons, Tools
    APPLE(260),
    BOW(261),
    ARROW(262),
    DIAMOND(264),
    IRON_INGOT(265),
    GOLD_INGOT(266),
    IRON_SWORD(267),
    WOODEN_SWORD(268),
    WOODEN_PICKAXE(270),
    STONE_SWORD(272),
    DIAMOND_SWORD(276),
    DIAMOND_PICKAXE(278),
    STICK(280),
    GOLDEN_SWORD(283),
    BREAD(297),
    COOKED_BEEF(364),
    EMERALD(388),
    NETHERITE_SWORD(1003);

    private final int id;

    Material(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public boolean isBlock() {
        return id < 256;
    }

    public boolean isItem() {
        return id >= 256;
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
