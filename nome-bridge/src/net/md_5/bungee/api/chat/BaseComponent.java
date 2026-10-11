package net.md_5.bungee.api.chat;

import net.md_5.bungee.api.ChatColor;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseComponent {
    private ChatColor color;
    private Boolean bold;
    private Boolean italic;
    private Boolean underlined;
    private Boolean strikethrough;
    private Boolean obfuscated;
    private ClickEvent clickEvent;
    private HoverEvent hoverEvent;
    private List<BaseComponent> extra;

    public BaseComponent() {}

    public BaseComponent(BaseComponent old) {
        this.color = old.color;
        this.bold = old.bold;
        this.italic = old.italic;
        this.underlined = old.underlined;
        this.strikethrough = old.strikethrough;
        this.obfuscated = old.obfuscated;
        this.clickEvent = old.clickEvent;
        this.hoverEvent = old.hoverEvent;
        if (old.extra != null) {
            this.extra = new ArrayList<>(old.extra);
        }
    }

    public abstract BaseComponent duplicate();

    public abstract String toPlainText();

    public String toLegacyText() {
        StringBuilder b = new StringBuilder();
        toLegacyText(b);
        return b.toString();
    }

    protected void toLegacyText(StringBuilder builder) {
        if (color != null) builder.append(color.toString());
        if (Boolean.TRUE.equals(bold)) builder.append(ChatColor.BOLD);
        if (Boolean.TRUE.equals(italic)) builder.append(ChatColor.ITALIC);
        if (Boolean.TRUE.equals(underlined)) builder.append(ChatColor.UNDERLINE);
        if (Boolean.TRUE.equals(strikethrough)) builder.append(ChatColor.STRIKETHROUGH);
        if (Boolean.TRUE.equals(obfuscated)) builder.append(ChatColor.MAGIC);
    }

    public ChatColor getColor() { return color; }
    public void setColor(ChatColor color) { this.color = color; }
    public Boolean isBold() { return bold; }
    public void setBold(Boolean bold) { this.bold = bold; }
    public Boolean isItalic() { return italic; }
    public void setItalic(Boolean italic) { this.italic = italic; }
    public Boolean isUnderlined() { return underlined; }
    public void setUnderlined(Boolean underlined) { this.underlined = underlined; }
    public Boolean isStrikethrough() { return strikethrough; }
    public void setStrikethrough(Boolean strikethrough) { this.strikethrough = strikethrough; }
    public Boolean isObfuscated() { return obfuscated; }
    public void setObfuscated(Boolean obfuscated) { this.obfuscated = obfuscated; }

    public ClickEvent getClickEvent() { return clickEvent; }
    public void setClickEvent(ClickEvent clickEvent) { this.clickEvent = clickEvent; }
    public HoverEvent getHoverEvent() { return hoverEvent; }
    public void setHoverEvent(HoverEvent hoverEvent) { this.hoverEvent = hoverEvent; }

    public List<BaseComponent> getExtra() { return extra; }
    public void setExtra(List<BaseComponent> extra) { this.extra = extra; }
    public void addExtra(BaseComponent component) {
        if (extra == null) extra = new ArrayList<>();
        extra.add(component);
    }
    public void addExtra(String text) {
        addExtra(new TextComponent(text));
    }
}
