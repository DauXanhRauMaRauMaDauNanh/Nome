package org.json.simple.parser;

public class ParseException extends Exception {
    private final int position;

    public ParseException(int position, Throwable cause) {
        super(cause);
        this.position = position;
    }

    public ParseException(int position, String message) {
        super(message);
        this.position = position;
    }

    public int getPosition() {
        return position;
    }
}
