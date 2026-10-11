package net.kyori.adventure.text.format;

public interface TextColor {
    int value();

    static TextColor color(int rgb) {
        return new TextColorImpl(rgb);
    }
}

class TextColorImpl implements TextColor {
    private final int value;

    TextColorImpl(int value) {
        this.value = value;
    }

    @Override
    public int value() {
        return value;
    }
}
