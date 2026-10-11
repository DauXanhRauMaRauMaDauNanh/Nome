package org.json.simple;

import java.util.ArrayList;
import java.util.List;

public class JSONArray extends ArrayList<Object> implements List<Object> {
    public JSONArray() {
        super();
    }

    public JSONArray(List<?> list) {
        super(list);
    }

    public String toJSONString() {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Object val : this) {
            if (!first) sb.append(',');
            first = false;
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
        sb.append(']');
        return sb.toString();
    }

    @Override
    public String toString() {
        return toJSONString();
    }
}
