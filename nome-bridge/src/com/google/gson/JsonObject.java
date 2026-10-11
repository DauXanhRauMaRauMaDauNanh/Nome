package com.google.gson;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class JsonObject extends JsonElement {
    private final Map<String, JsonElement> members = new LinkedHashMap<>();

    public void add(String property, JsonElement value) {
        members.put(property, value == null ? JsonNull.INSTANCE : value);
    }

    public JsonElement remove(String property) {
        return members.remove(property);
    }

    public void addProperty(String property, String value) {
        add(property, value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    public void addProperty(String property, Number value) {
        add(property, value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    public void addProperty(String property, Boolean value) {
        add(property, value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    public void addProperty(String property, Character value) {
        add(property, value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    public Set<Map.Entry<String, JsonElement>> entrySet() {
        return members.entrySet();
    }

    public boolean has(String memberName) {
        return members.containsKey(memberName);
    }

    public JsonElement get(String memberName) {
        return members.get(memberName);
    }

    public JsonPrimitive getAsJsonPrimitive(String memberName) {
        JsonElement elem = members.get(memberName);
        return elem != null && elem.isJsonPrimitive() ? (JsonPrimitive) elem : null;
    }

    public JsonArray getAsJsonArray(String memberName) {
        JsonElement elem = members.get(memberName);
        return elem != null && elem.isJsonArray() ? (JsonArray) elem : null;
    }

    public JsonObject getAsJsonObject(String memberName) {
        JsonElement elem = members.get(memberName);
        return elem != null && elem.isJsonObject() ? (JsonObject) elem : null;
    }

    public int size() {
        return members.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, JsonElement> entry : members.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(entry.getKey()).append("\":").append(entry.getValue());
        }
        sb.append('}');
        return sb.toString();
    }
}
