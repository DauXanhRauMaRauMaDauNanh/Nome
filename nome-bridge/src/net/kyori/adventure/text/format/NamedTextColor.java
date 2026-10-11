package net.kyori.adventure.text.format;

public final class NamedTextColor implements TextColor {
    public static final NamedTextColor BLACK = new NamedTextColor(0x000000, "black");
    public static final NamedTextColor DARK_BLUE = new NamedTextColor(0x0000AA, "dark_blue");
    public static final NamedTextColor DARK_GREEN = new NamedTextColor(0x00AA00, "dark_green");
    public static final NamedTextColor DARK_AQUA = new NamedTextColor(0x00AAAA, "dark_aqua");
    public static final NamedTextColor DARK_RED = new NamedTextColor(0xAA0000, "dark_red");
    public static final NamedTextColor DARK_PURPLE = new NamedTextColor(0xAA00AA, "dark_purple");
    public static final NamedTextColor GOLD = new NamedTextColor(0xFFAA00, "gold");
    public static final NamedTextColor GRAY = new NamedTextColor(0xAAAAAA, "gray");
    public static final NamedTextColor DARK_GRAY = new NamedTextColor(0x555555, "dark_gray");
    public static final NamedTextColor BLUE = new NamedTextColor(0x5555FF, "blue");
    public static final NamedTextColor GREEN = new NamedTextColor(0x55FF55, "green");
    public static final NamedTextColor AQUA = new NamedTextColor(0x55FFFF, "aqua");
    public static final NamedTextColor RED = new NamedTextColor(0xFF5555, "red");
    public static final NamedTextColor LIGHT_PURPLE = new NamedTextColor(0xFF55FF, "light_purple");
    public static final NamedTextColor YELLOW = new NamedTextColor(0xFFFF55, "yellow");
    public static final NamedTextColor WHITE = new NamedTextColor(0xFFFFFF, "white");

    private final int value;
    private final String name;

    private NamedTextColor(int value, String name) {
        this.value = value;
        this.name = name;
    }

    @Override
    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return name;
    }
}
