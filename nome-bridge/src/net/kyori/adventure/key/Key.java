package net.kyori.adventure.key;

public interface Key extends Comparable<Key> {
    String namespace();
    String value();
    String asString();

    static Key key(String namespace, String value) {
        return new KeyImpl(namespace, value);
    }

    static Key key(String string) {
        String[] parts = string.split(":", 2);
        return parts.length == 2 ? key(parts[0], parts[1]) : key("minecraft", parts[0]);
    }
}
