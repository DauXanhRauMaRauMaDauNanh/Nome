package com.google.gson;

public class JsonParser {
    public static JsonElement parseString(String json) {
        if (json == null) return JsonNull.INSTANCE;
        String trimmed = json.trim();
        if (trimmed.isEmpty() || "null".equals(trimmed)) return JsonNull.INSTANCE;

        SimpleJsonReader reader = new SimpleJsonReader(trimmed);
        return reader.parse();
    }

    private static class SimpleJsonReader {
        private final String src;
        private int pos = 0;

        SimpleJsonReader(String src) {
            this.src = src;
        }

        JsonElement parse() {
            skipWhitespace();
            if (pos >= src.length()) return JsonNull.INSTANCE;
            char c = src.charAt(pos);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseStringVal();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') return parseNull();
            return parseNumber();
        }

        private void skipWhitespace() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }

        private JsonObject parseObject() {
            JsonObject obj = new JsonObject();
            pos++; // skip '{'
            skipWhitespace();
            if (pos < src.length() && src.charAt(pos) == '}') {
                pos++;
                return obj;
            }

            while (pos < src.length()) {
                skipWhitespace();
                String key = parseRawString();
                skipWhitespace();
                if (pos < src.length() && src.charAt(pos) == ':') pos++;
                skipWhitespace();
                JsonElement val = parse();
                obj.add(key, val);

                skipWhitespace();
                if (pos < src.length() && src.charAt(pos) == ',') {
                    pos++;
                } else if (pos < src.length() && src.charAt(pos) == '}') {
                    pos++;
                    break;
                }
            }
            return obj;
        }

        private JsonArray parseArray() {
            JsonArray arr = new JsonArray();
            pos++; // skip '['
            skipWhitespace();
            if (pos < src.length() && src.charAt(pos) == ']') {
                pos++;
                return arr;
            }

            while (pos < src.length()) {
                skipWhitespace();
                JsonElement val = parse();
                arr.add(val);

                skipWhitespace();
                if (pos < src.length() && src.charAt(pos) == ',') {
                    pos++;
                } else if (pos < src.length() && src.charAt(pos) == ']') {
                    pos++;
                    break;
                }
            }
            return arr;
        }

        private String parseRawString() {
            skipWhitespace();
            if (pos < src.length() && src.charAt(pos) == '"') {
                pos++;
                StringBuilder sb = new StringBuilder();
                while (pos < src.length()) {
                    char c = src.charAt(pos++);
                    if (c == '\\') {
                        if (pos < src.length()) {
                            char esc = src.charAt(pos++);
                            if (esc == '"' || esc == '\\' || esc == '/') sb.append(esc);
                            else if (esc == 'b') sb.append('\b');
                            else if (esc == 'f') sb.append('\f');
                            else if (esc == 'n') sb.append('\n');
                            else if (esc == 'r') sb.append('\r');
                            else if (esc == 't') sb.append('\t');
                            else sb.append(esc);
                        }
                    } else if (c == '"') {
                        break;
                    } else {
                        sb.append(c);
                    }
                }
                return sb.toString();
            }
            return "";
        }

        private JsonPrimitive parseStringVal() {
            return new JsonPrimitive(parseRawString());
        }

        private JsonPrimitive parseBoolean() {
            if (src.startsWith("true", pos)) {
                pos += 4;
                return new JsonPrimitive(true);
            }
            if (src.startsWith("false", pos)) {
                pos += 5;
                return new JsonPrimitive(false);
            }
            return new JsonPrimitive(false);
        }

        private JsonNull parseNull() {
            if (src.startsWith("null", pos)) {
                pos += 4;
            }
            return JsonNull.INSTANCE;
        }

        private JsonPrimitive parseNumber() {
            int start = pos;
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                    pos++;
                } else {
                    break;
                }
            }
            String numStr = src.substring(start, pos);
            try {
                if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E")) {
                    return new JsonPrimitive(Double.parseDouble(numStr));
                } else {
                    return new JsonPrimitive(Long.parseLong(numStr));
                }
            } catch (Exception e) {
                return new JsonPrimitive(numStr);
            }
        }
    }
}
