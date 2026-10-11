package com.google.gson;

public final class JsonPrimitive extends JsonElement {
    private final Object value;

    public JsonPrimitive(Boolean bool) {
        this.value = bool;
    }

    public JsonPrimitive(Number number) {
        this.value = number;
    }

    public JsonPrimitive(String string) {
        this.value = string;
    }

    public JsonPrimitive(Character c) {
        this.value = String.valueOf(c);
    }

    public boolean isBoolean() {
        return value instanceof Boolean;
    }

    public boolean isNumber() {
        return value instanceof Number;
    }

    public boolean isString() {
        return value instanceof String;
    }

    @Override
    public boolean getAsBoolean() {
        return isBoolean() ? (Boolean) value : Boolean.parseBoolean(getAsString());
    }

    @Override
    public Number getAsNumber() {
        return isNumber() ? (Number) value : Double.parseDouble(getAsString());
    }

    @Override
    public String getAsString() {
        return String.valueOf(value);
    }

    @Override
    public double getAsDouble() {
        return isNumber() ? getAsNumber().doubleValue() : Double.parseDouble(getAsString());
    }

    @Override
    public float getAsFloat() {
        return isNumber() ? getAsNumber().floatValue() : Float.parseFloat(getAsString());
    }

    @Override
    public long getAsLong() {
        return isNumber() ? getAsNumber().longValue() : Long.parseLong(getAsString());
    }

    @Override
    public int getAsInt() {
        return isNumber() ? getAsNumber().intValue() : Integer.parseInt(getAsString());
    }

    @Override
    public byte getAsByte() {
        return isNumber() ? getAsNumber().byteValue() : Byte.parseByte(getAsString());
    }

    @Override
    public String toString() {
        if (isString()) {
            return "\"" + value.toString().replace("\"", "\\\"") + "\"";
        }
        return String.valueOf(value);
    }
}
