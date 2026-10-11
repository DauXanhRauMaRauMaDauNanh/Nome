package net.kyori.adventure.text.serializer.plain;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

public class PlainTextComponentSerializer {
    private static final PlainTextComponentSerializer INSTANCE = new PlainTextComponentSerializer();

    public static PlainTextComponentSerializer plainText() {
        return INSTANCE;
    }

    public String serialize(Component component) {
        if (component == null) return "";
        if (component instanceof TextComponent tc) {
            StringBuilder sb = new StringBuilder(tc.content());
            for (Component child : tc.children()) {
                sb.append(serialize(child));
            }
            return sb.toString();
        }
        return component.toString();
    }
}
