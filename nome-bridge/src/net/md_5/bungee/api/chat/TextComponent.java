package net.md_5.bungee.api.chat;

import net.md_5.bungee.api.ChatColor;
import java.util.ArrayList;
import java.util.List;

public class TextComponent extends BaseComponent {
    private String text;

    public TextComponent() {
        this("");
    }

    public TextComponent(String text) {
        this.text = text != null ? text : "";
    }

    public TextComponent(TextComponent textComponent) {
        super(textComponent);
        this.text = textComponent.text;
    }

    public TextComponent(BaseComponent... extras) {
        this("");
        if (extras != null) {
            for (BaseComponent c : extras) {
                addExtra(c);
            }
        }
    }

    public static BaseComponent[] fromLegacyText(String message) {
        if (message == null || message.isEmpty()) {
            return new BaseComponent[]{new TextComponent("")};
        }
        List<BaseComponent> components = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        ChatColor currentColor = null;

        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == ChatColor.COLOR_CHAR && i + 1 < message.length()) {
                char code = message.charAt(++i);
                ChatColor color = ChatColor.getByChar(code);
                if (color != null) {
                    if (current.length() > 0) {
                        TextComponent part = new TextComponent(current.toString());
                        part.setColor(currentColor);
                        components.add(part);
                        current.setLength(0);
                    }
                    currentColor = color;
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            TextComponent part = new TextComponent(current.toString());
            part.setColor(currentColor);
            components.add(part);
        }
        return components.toArray(new BaseComponent[0]);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text != null ? text : "";
    }

    @Override
    public BaseComponent duplicate() {
        return new TextComponent(this);
    }

    @Override
    public String toPlainText() {
        StringBuilder sb = new StringBuilder(text);
        if (getExtra() != null) {
            for (BaseComponent c : getExtra()) {
                sb.append(c.toPlainText());
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toPlainText();
    }
}
