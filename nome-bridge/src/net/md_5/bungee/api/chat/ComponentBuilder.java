package net.md_5.bungee.api.chat;

import net.md_5.bungee.api.ChatColor;
import java.util.ArrayList;
import java.util.List;

public class ComponentBuilder {
    private final List<BaseComponent> parts = new ArrayList<>();
    private BaseComponent current;

    public ComponentBuilder() {
        this("");
    }

    public ComponentBuilder(String text) {
        current = new TextComponent(text);
    }

    public ComponentBuilder(BaseComponent component) {
        current = component.duplicate();
    }

    public ComponentBuilder append(String text) {
        parts.add(current);
        current = new TextComponent(text);
        return this;
    }

    public ComponentBuilder append(BaseComponent component) {
        parts.add(current);
        current = component.duplicate();
        return this;
    }

    public ComponentBuilder color(ChatColor color) {
        current.setColor(color);
        return this;
    }

    public ComponentBuilder bold(boolean bold) {
        current.setBold(bold);
        return this;
    }

    public ComponentBuilder italic(boolean italic) {
        current.setItalic(italic);
        return this;
    }

    public ComponentBuilder event(ClickEvent clickEvent) {
        current.setClickEvent(clickEvent);
        return this;
    }

    public ComponentBuilder event(HoverEvent hoverEvent) {
        current.setHoverEvent(hoverEvent);
        return this;
    }

    public BaseComponent[] create() {
        BaseComponent[] result = new BaseComponent[parts.size() + 1];
        for (int i = 0; i < parts.size(); i++) {
            result[i] = parts.get(i);
        }
        result[parts.size()] = current;
        return result;
    }
}
