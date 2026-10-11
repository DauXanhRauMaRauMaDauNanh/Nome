package com.mojang.brigadier.arguments;

public class BoolArgumentType implements ArgumentType<Boolean> {
    public static BoolArgumentType bool() {
        return new BoolArgumentType();
    }

    public static boolean getBool(com.mojang.brigadier.context.CommandContext<?> context, String name) {
        return context.getArgument(name, Boolean.class);
    }
}
