package net.kyori.adventure.text;

import net.kyori.adventure.text.format.TextColor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface TextComponent extends Component {
    String content();
    TextColor color();
}

class TextComponentImpl implements TextComponent {
    private final String content;
    private TextColor color;
    private final List<Component> children = new ArrayList<>();

    TextComponentImpl(String content) {
        this.content = content != null ? content : "";
    }

    void setColor(TextColor color) {
        this.color = color;
    }

    @Override
    public String content() {
        return content;
    }

    @Override
    public TextColor color() {
        return color;
    }

    @Override
    public List<Component> children() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public Component append(Component component) {
        if (component != null) {
            children.add(component);
        }
        return this;
    }

    @Override
    public String toString() {
        return content;
    }
}
