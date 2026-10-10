package net.kyori.adventure.key;

import java.util.Objects;

final class KeyImpl implements Key {
    private final String namespace;
    private final String value;

    KeyImpl(String namespace, String value) {
        this.namespace = Objects.requireNonNull(namespace, "namespace");
        this.value = Objects.requireNonNull(value, "value");
    }

    @Override
    public String namespace() {
        return namespace;
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public String asString() {
        return namespace + ":" + value;
    }

    @Override
    public String toString() {
        return asString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Key other)) return false;
        return namespace.equals(other.namespace()) && value.equals(other.value());
    }

    @Override
    public int hashCode() {
        return 31 * namespace.hashCode() + value.hashCode();
    }

    @Override
    public int compareTo(Key o) {
        return asString().compareTo(o.asString());
    }
}
