package org.json.simple;

import java.util.HashMap;
import java.util.Map;

public class JSONObject extends HashMap<Object, Object> implements Map<Object, Object> {
    public JSONObject() {
        super();
    }

    public JSONObject(Map<?, ?> map) {
        super(map);
    }

    public String toJSONString() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<Object, Object> entry : entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(entry.getKey()).append("\":");
            Object val = entry.getValue();
            if (val instanceof String s) {
                sb.append('"').append(s.replace("\"", "\\\"")).append('"');
            } else if (val instanceof JSONObject jo) {
                sb.append(jo.toJSONString());
            } else if (val instanceof JSONArray ja) {
                sb.append(ja.toJSONString());
            } else {
                sb.append(val);
            }
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public String toString() {
        return toJSONString();
    }
}
