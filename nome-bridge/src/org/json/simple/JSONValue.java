package org.json.simple;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.json.simple.parser.ParseException;

public class JSONValue {
    public static Object parse(String s) {
        if (s == null) return null;
        try {
            JsonElement elem = JsonParser.parseString(s);
            return convertElement(elem);
        } catch (Exception e) {
            return null;
        }
    }

    public static Object parseWithException(String s) throws ParseException {
        if (s == null) return null;
        try {
            JsonElement elem = JsonParser.parseString(s);
            return convertElement(elem);
        } catch (Exception e) {
            throw new ParseException(0, e);
        }
    }

    static Object convertElement(JsonElement elem) {
        if (elem == null || elem.isJsonNull()) return null;
        if (elem.isJsonObject()) {
            JSONObject obj = new JSONObject();
            for (var entry : elem.getAsJsonObject().entrySet()) {
                obj.put(entry.getKey(), convertElement(entry.getValue()));
            }
            return obj;
        }
        if (elem.isJsonArray()) {
            JSONArray arr = new JSONArray();
            for (JsonElement item : elem.getAsJsonArray()) {
                arr.add(convertElement(item));
            }
            return arr;
        }
        if (elem.isJsonPrimitive()) {
            var prim = elem.getAsJsonPrimitive();
            if (prim.isBoolean()) return prim.getAsBoolean();
            if (prim.isNumber()) return prim.getAsNumber();
            return prim.getAsString();
        }
        return elem.toString();
    }
}
