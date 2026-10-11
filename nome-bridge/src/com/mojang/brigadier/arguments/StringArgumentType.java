package com.mojang.brigadier.arguments;

public class StringArgumentType implements ArgumentType<String> {
    public static StringArgumentType word() {
        return new StringArgumentType();
    }

    public static StringArgumentType string() {
        return new StringArgumentType();
    }

    public static StringArgumentType greedyString() {
        return new StringArgumentType();
    }

    public static String getString(com.mojang.brigadier.context.CommandContext<?> context, String name) {
        return context.getArgument(name, String.class);
    }
}
