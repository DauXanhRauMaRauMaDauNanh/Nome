package net.kyori.adventure.text;

import net.kyori.adventure.text.format.TextColor;
import java.util.Collections;
import java.util.List;

public interface Component {
    static Component empty() {
        return text("");
    }

    static TextComponent text(String content) {
        return new TextComponentImpl(content);
    }

    static TextComponent text(String content, TextColor color) {
        TextComponentImpl c = new TextComponentImpl(content);
        c.setColor(color);
        return c;
    }

    default List<Component> children() {
        return Collections.emptyList();
    }

    default Component append(Component component) {
        return this;
    }
}
