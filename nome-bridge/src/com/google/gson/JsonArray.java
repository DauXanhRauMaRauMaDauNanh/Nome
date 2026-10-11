package com.google.gson;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class JsonArray extends JsonElement implements Iterable<JsonElement> {
    private final List<JsonElement> elements = new ArrayList<>();

    public void add(JsonElement element) {
        elements.add(element == null ? JsonNull.INSTANCE : element);
    }

    public void add(String string) {
        add(string == null ? JsonNull.INSTANCE : new JsonPrimitive(string));
    }

    public void add(Boolean bool) {
        add(bool == null ? JsonNull.INSTANCE : new JsonPrimitive(bool));
    }

    public void add(Character character) {
        add(character == null ? JsonNull.INSTANCE : new JsonPrimitive(character));
    }

    public void add(Number number) {
        add(number == null ? JsonNull.INSTANCE : new JsonPrimitive(number));
    }

    public int size() {
        return elements.size();
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public JsonElement get(int i) {
        return elements.get(i);
    }

    @Override
    public Iterator<JsonElement> iterator() {
        return elements.iterator();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (JsonElement elem : elements) {
            if (!first) sb.append(',');
            first = false;
            sb.append(elem.toString());
        }
        sb.append(']');
        return sb.toString();
    }
}
