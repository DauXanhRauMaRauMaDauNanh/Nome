package com.mojang.brigadier.arguments;

public class IntegerArgumentType implements ArgumentType<Integer> {
    public static IntegerArgumentType integer() {
        return new IntegerArgumentType();
    }

    public static IntegerArgumentType integer(int min) {
        return new IntegerArgumentType();
    }

    public static IntegerArgumentType integer(int min, int max) {
        return new IntegerArgumentType();
    }

    public static int getInteger(com.mojang.brigadier.context.CommandContext<?> context, String name) {
        return context.getArgument(name, Integer.class);
    }
}
