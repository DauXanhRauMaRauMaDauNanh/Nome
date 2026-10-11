package net.kyori.adventure.text.serializer.legacy;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.ChatColor;

public class LegacyComponentSerializer {
    private static final LegacyComponentSerializer SECTION = new LegacyComponentSerializer('\u00A7');
    private static final LegacyComponentSerializer AMPERSAND = new LegacyComponentSerializer('&');

    private final char character;

    private LegacyComponentSerializer(char character) {
        this.character = character;
    }

    public static LegacyComponentSerializer legacySection() {
        return SECTION;
    }

    public static LegacyComponentSerializer legacyAmpersand() {
        return AMPERSAND;
    }

    public Component deserialize(String input) {
        if (input == null) return Component.empty();
        String translated = ChatColor.translateAlternateColorCodes(character, input);
        return Component.text(translated);
    }

    public String serialize(Component component) {
        if (component == null) return "";
        return component.toString();
    }
}
