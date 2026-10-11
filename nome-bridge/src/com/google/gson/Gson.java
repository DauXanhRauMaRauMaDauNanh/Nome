package com.google.gson;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class Gson {
    public String toJson(Object src) {
        if (src == null) return "null";
        if (src instanceof JsonElement elem) return elem.toString();
        if (src instanceof String s) return "\"" + s.replace("\"", "\\\"") + "\"";
        if (src instanceof Number || src instanceof Boolean) return src.toString();
        if (src instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append('"').append(entry.getKey()).append("\":").append(toJson(entry.getValue()));
            }
            sb.append('}');
            return sb.toString();
        }
        if (src instanceof Iterable<?> iter) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object o : iter) {
                if (!first) sb.append(',');
                first = false;
                sb.append(toJson(o));
            }
            sb.append(']');
            return sb.toString();
        }

        // POJO reflection serialization
        try {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Field f : src.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) continue;
                f.setAccessible(true);
                Object val = f.get(src);
                if (val != null) {
                    if (!first) sb.append(',');
                    first = false;
                    sb.append('"').append(f.getName()).append("\":").append(toJson(val));
                }
            }
            sb.append('}');
            return sb.toString();
        } catch (Exception e) {
            return "\"" + src.toString() + "\"";
        }
    }

    public <T> T fromJson(String json, Class<T> classOfT) {
        if (json == null || json.trim().isEmpty() || "null".equals(json.trim())) return null;
        JsonElement elem = JsonParser.parseString(json);
        return fromJson(elem, classOfT);
    }

    @SuppressWarnings("unchecked")
    public <T> T fromJson(JsonElement elem, Class<T> classOfT) {
        if (elem == null || elem.isJsonNull()) return null;
        if (classOfT.isInstance(elem)) return (T) elem;
        if (classOfT == String.class) return (T) (elem.isJsonPrimitive() ? elem.getAsString() : elem.toString());
        if (classOfT == Integer.class || classOfT == int.class) return (T) Integer.valueOf(elem.getAsInt());
        if (classOfT == Long.class || classOfT == long.class) return (T) Long.valueOf(elem.getAsLong());
        if (classOfT == Double.class || classOfT == double.class) return (T) Double.valueOf(elem.getAsDouble());
        if (classOfT == Boolean.class || classOfT == boolean.class) return (T) Boolean.valueOf(elem.getAsBoolean());

        if (elem.isJsonObject()) {
            try {
                T instance = classOfT.getDeclaredConstructor().newInstance();
                JsonObject obj = elem.getAsJsonObject();
                for (Field f : classOfT.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) continue;
                    f.setAccessible(true);
                    if (obj.has(f.getName())) {
                        Object val = fromJson(obj.get(f.getName()), f.getType());
                        f.set(instance, val);
                    }
                }
                return instance;
            } catch (Exception ignored) {}
        }
        return null;
    }
}
