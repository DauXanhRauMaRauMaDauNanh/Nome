package net.kyori.adventure.key;

public interface Key extends Comparable<Key> {
    String MINECRAFT_NAMESPACE = "minecraft";

    String namespace();
    String value();
    String asString();

    static Key key(String string) {
        int index = string.indexOf(':');
        if (index >= 0) {
            return key(string.substring(0, index), string.substring(index + 1));
        }
        return key(MINECRAFT_NAMESPACE, string);
    }

    static Key key(String namespace, String value) {
        return new KeyImpl(namespace, value);
    }

    @Override
    default int compareTo(Key other) {
        int res = namespace().compareTo(other.namespace());
        return res != 0 ? res : value().compareTo(other.value());
    }
}
