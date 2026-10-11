package net.kyori.adventure.text.minimessage;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;

public class MiniMessage {
    private static final MiniMessage INSTANCE = new MiniMessage();

    public static MiniMessage miniMessage() {
        return INSTANCE;
    }

    public Component deserialize(String input) {
        if (input == null) return Component.empty();
        // Fast tag strip/replace: <red>, <green>, <bold>, etc.
        String parsed = input
            .replace("<black>", "\u00A70")
            .replace("<dark_blue>", "\u00A71")
            .replace("<dark_green>", "\u00A72")
            .replace("<dark_aqua>", "\u00A73")
            .replace("<dark_red>", "\u00A74")
            .replace("<dark_purple>", "\u00A75")
            .replace("<gold>", "\u00A76")
            .replace("<gray>", "\u00A77")
            .replace("<dark_gray>", "\u00A78")
            .replace("<blue>", "\u00A79")
            .replace("<green>", "\u00A7a")
            .replace("<aqua>", "\u00A7b")
            .replace("<red>", "\u00A7c")
            .replace("<light_purple>", "\u00A7d")
            .replace("<yellow>", "\u00A7e")
            .replace("<white>", "\u00A7f")
            .replace("<bold>", "\u00A7l")
            .replace("<italic>", "\u00A7o")
            .replace("<underlined>", "\u00A7n")
            .replace("<strikethrough>", "\u00A7m")
            .replace("<reset>", "\u00A7r");
        return Component.text(parsed);
    }

    public String serialize(Component component) {
        if (component == null) return "";
        return component.toString();
    }
}
