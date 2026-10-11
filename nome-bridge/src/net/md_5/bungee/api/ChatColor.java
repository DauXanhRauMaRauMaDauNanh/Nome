package net.md_5.bungee.api;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class ChatColor {
    public static final char COLOR_CHAR = '\u00A7';
    public static final String ALL_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRr";
    public static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)" + COLOR_CHAR + "[0-9A-FK-OR]");

    public static final ChatColor BLACK = new ChatColor('0', "black");
    public static final ChatColor DARK_BLUE = new ChatColor('1', "dark_blue");
    public static final ChatColor DARK_GREEN = new ChatColor('2', "dark_green");
    public static final ChatColor DARK_AQUA = new ChatColor('3', "dark_aqua");
    public static final ChatColor DARK_RED = new ChatColor('4', "dark_red");
    public static final ChatColor DARK_PURPLE = new ChatColor('5', "dark_purple");
    public static final ChatColor GOLD = new ChatColor('6', "gold");
    public static final ChatColor GRAY = new ChatColor('7', "gray");
    public static final ChatColor DARK_GRAY = new ChatColor('8', "dark_gray");
    public static final ChatColor BLUE = new ChatColor('9', "blue");
    public static final ChatColor GREEN = new ChatColor('a', "green");
    public static final ChatColor AQUA = new ChatColor('b', "aqua");
    public static final ChatColor RED = new ChatColor('c', "red");
    public static final ChatColor LIGHT_PURPLE = new ChatColor('d', "light_purple");
    public static final ChatColor YELLOW = new ChatColor('e', "yellow");
    public static final ChatColor WHITE = new ChatColor('f', "white");
    public static final ChatColor MAGIC = new ChatColor('k', "obfuscated");
    public static final ChatColor BOLD = new ChatColor('l', "bold");
    public static final ChatColor STRIKETHROUGH = new ChatColor('m', "strikethrough");
    public static final ChatColor UNDERLINE = new ChatColor('n', "underline");
    public static final ChatColor ITALIC = new ChatColor('o', "italic");
    public static final ChatColor RESET = new ChatColor('r', "reset");

    private static final Map<Character, ChatColor> BY_CHAR = new HashMap<>();

    static {
        for (ChatColor color : new ChatColor[]{BLACK, DARK_BLUE, DARK_GREEN, DARK_AQUA, DARK_RED, DARK_PURPLE,
                GOLD, GRAY, DARK_GRAY, BLUE, GREEN, AQUA, RED, LIGHT_PURPLE, YELLOW, WHITE, MAGIC, BOLD,
                STRIKETHROUGH, UNDERLINE, ITALIC, RESET}) {
            BY_CHAR.put(color.code, color);
        }
    }

    private final char code;
    private final String name;
    private final String toString;

    private ChatColor(char code, String name) {
        this.code = code;
        this.name = name;
        this.toString = new String(new char[]{COLOR_CHAR, code});
    }

    public static ChatColor getByChar(char code) {
        return BY_CHAR.get(code);
    }

    public static String stripColor(String input) {
        if (input == null) return null;
        return STRIP_COLOR_PATTERN.matcher(input).replaceAll("");
    }

    public static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        if (textToTranslate == null) return null;
        char[] b = textToTranslate.toCharArray();
        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == altColorChar && ALL_CODES.indexOf(b[i + 1]) > -1) {
                b[i] = COLOR_CHAR;
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }
        return new String(b);
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return toString;
    }
}
