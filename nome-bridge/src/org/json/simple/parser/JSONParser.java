package org.json.simple.parser;

import org.json.simple.JSONValue;
import java.io.Reader;
import java.io.StringWriter;

public class JSONParser {
    public Object parse(String s) throws ParseException {
        return JSONValue.parseWithException(s);
    }

    public Object parse(Reader in) throws ParseException {
        try {
            char[] arr = new char[8192];
            StringBuilder buf = new StringBuilder();
            int numChars;
            while ((numChars = in.read(arr, 0, arr.length)) > 0) {
                buf.append(arr, 0, numChars);
            }
            return parse(buf.toString());
        } catch (Exception e) {
            throw new ParseException(0, e);
        }
    }
}
